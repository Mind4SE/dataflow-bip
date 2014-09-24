/**
 * Copyright (C) 2010 STMicroelectronics
 * Copyright (C) 2010 VERIMAG
 * Copyright (C) 2014 Schneider-Electric
 *
 * This file is part of "Mind Compiler"
 * The "Mind Compiler" is free software: you can redistribute 
 * it and/or modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact: mind@ow2.org
 *
 * Authors: Matthieu Leclercq, Marc Poulhiès
 * Contributors:
  * - Stephane Seyvoz - mind-compiler 2.1-SNAPSHOT support
 */

package org.ow2.mind.beam.adl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.fractal.adl.ADLException;
import org.objectweb.fractal.adl.Definition;
import org.objectweb.fractal.adl.interfaces.Interface;
import org.objectweb.fractal.adl.util.FractalADLLogManager;
import org.ow2.mind.InputResourceLocator;
import org.ow2.mind.adl.InstanceSourceGenerator;
import org.ow2.mind.adl.InstancesDescriptor;
import org.ow2.mind.adl.ast.ASTHelper;
import org.ow2.mind.adl.ast.Binding;
import org.ow2.mind.adl.ast.BindingContainer;
import org.ow2.mind.adl.ast.MindInterface;
import org.ow2.mind.adl.graph.ComponentGraph;
import org.ow2.mind.adl.idl.InterfaceDefinitionDecorationHelper;
import org.ow2.mind.beam.BackendCommandLineHandler;
import org.ow2.mind.beam.annotation.BeamFilterAnnotationProcessor;
import org.ow2.mind.idl.IDLLoader;
import org.ow2.mind.idl.ast.InterfaceDefinition;
import org.ow2.mind.idl.ast.Method;
import org.ow2.mind.io.IOErrors;
import org.ow2.mind.io.OutputFileLocator;

import ujf.verimag.bip.Core.Behaviors.AtomType;
import ujf.verimag.bip.Core.Behaviors.Port;
import ujf.verimag.bip.Core.Behaviors.PortDefinition;
import ujf.verimag.bip.Core.Behaviors.PortType;
import ujf.verimag.bip.Core.Interactions.Component;
import ujf.verimag.bip.Core.Interactions.CompoundType;
import ujf.verimag.bip.Core.Interactions.ConnectorType;
import ujf.verimag.bip.Core.Interactions.InnerPortReference;
import ujf.verimag.bip.Core.Modules.Module;
import ujf.verimag.bip.backend.bip2src.Reverse;
import ujf.verimag.bip.compiler.backend.BackendException;
import ujf.verimag.bip.metamodelAPI.BipCreator;
import ujf.verimag.bip.metamodelAPI.BipUtil;

import com.google.inject.Inject;

public class BipInstanceSourceGenerator implements InstanceSourceGenerator {
  
  protected static Logger logger = FractalADLLogManager
  .getLogger("beam-bip-visitor::instance");
  
  
  protected InstancesDescriptor root_input = null;
  protected CompoundType root_ct = null;
  protected int subcomponent_count = -1;
  protected int subcomponent_done = 0;
  
  // ---------------------------------------------------------------------------
  // Client Interfaces
  // ---------------------------------------------------------------------------

  /** Client interface used to locate output files. */
  @Inject
  public OutputFileLocator    outputFileLocatorItf;

  /** client interface used to checks timestamps of input resources. */
  @Inject
  public InputResourceLocator inputResourceLocatorItf;

  @Inject
  public IDLLoader idlLoaderItf;

  /**
   * Creates the connector types & instance between the filters and the buffers
   * @param bip_module the root bip module
   * @param ct the root compound type hosting all instances
   * @param input the input instance
   * @param context the compiler context
   * @throws ADLException
   */
  protected void createConnectors(ujf.verimag.bip.Core.Modules.Module bip_module, CompoundType ct, 
          InstancesDescriptor input, Map<Object, Object> context) throws ADLException{
      for (Binding binding : ((BindingContainer) input.instanceDefinition).getBindings()){
          String from_comp_name = binding.getFromComponent();
          String from_iface_name = binding.getFromInterface();
          String to_comp_name = binding.getToComponent();
          String to_iface_name = binding.getToInterface();

         
          CompoundType root_ct = (CompoundType)context.get(BipDefinitionSourceGenerator.BEAM_BIP_ROOT_COMP_DEF);
          assert(root_ct != null);
          
          Component to_comp_instance = BipUtil.getComponentInstance(root_ct, to_comp_name);
          Component from_comp_instance = BipUtil.getComponentInstance(root_ct, from_comp_name);
          assert(to_comp_instance != null);
          assert(from_comp_instance != null);
          
          org.ow2.mind.adl.ast.Component from_comp = ASTHelper.getComponent(input.instanceDefinition, from_comp_name);
          org.ow2.mind.adl.ast.Component to_comp = ASTHelper.getComponent(input.instanceDefinition, to_comp_name);
          
          Definition from_comp_def = ASTHelper.getResolvedDefinition(from_comp.getDefinitionReference(), null, context);
          Definition to_comp_def = ASTHelper.getResolvedDefinition(to_comp.getDefinitionReference(), null, context);
          
          AtomType from_atom_type = BipUtil.getAtomTypeDefinition(
                  BipDefinitionSourceGenerator.mindToBipMangleName(from_comp_def.getName()), bip_module);
          AtomType to_atom_type = BipUtil.getAtomTypeDefinition(
                  BipDefinitionSourceGenerator.mindToBipMangleName(to_comp_def.getName()), bip_module);
          assert(from_atom_type != null);
          assert(to_atom_type != null);
          
          Interface from_iface_ = ASTHelper.getInterface(from_comp_def, from_iface_name);
          Interface to_iface_ = ASTHelper.getInterface(to_comp_def, to_iface_name);
          
          assert(from_iface_ instanceof MindInterface);
          assert(to_iface_ instanceof MindInterface);
          
          MindInterface from_iface = (MindInterface) from_iface_;
          MindInterface to_iface = (MindInterface) to_iface_;
          
          InterfaceDefinition from_idef = 
              InterfaceDefinitionDecorationHelper.getResolvedInterfaceDefinition(from_iface, 
                      idlLoaderItf, context);
          
          InterfaceDefinition to_idef = 
              InterfaceDefinitionDecorationHelper.getResolvedInterfaceDefinition(to_iface, 
                      idlLoaderItf, context);
          assert(from_idef == to_idef);
          
          String bip_atom_buffer_type_name = from_idef.getName().replace('.', '_');
          AtomType buffer_type = BipUtil.getAtomTypeDefinition(bip_atom_buffer_type_name, bip_module);
          String buffer_instance_name = "buffer__" + from_comp_name + "_" + from_iface_name +"__" + to_comp_name + "_" + to_iface_name;
          
          Component buffer_instance = BipCreator.createComponentInstance(buffer_instance_name, ct, buffer_type);
          
          for (Method m: from_idef.getMethods()){
              String mname = m.getName();
              ConnectorType connector_t;
              
              /*
               * Client to buffer side
               */
              String client_callp_name = from_iface_name + "__" + mname + "_CALLp";
              
              PortDefinition client_callp_def = BipUtil.getPortDefinition(from_atom_type, client_callp_name);
              // if null, Atom does not have the port, no need to create the connector.
              if (client_callp_def != null) {
                  PortType client_callp_type = client_callp_def.getType();
                  PortDefinition in_s_def = BipUtil.getPortDefinition(buffer_type, "in_S");
                  assert(in_s_def != null);

                  PortType in_s_type = in_s_def.getType();

                  List<PortType> synchrons = new ArrayList<PortType>();
                  synchrons.add(in_s_type);
                  synchrons.add(client_callp_type);

                  connector_t = BipCreator.createRDVConnectorType(bip_module, buffer_instance_name + "__" + m.getName() + "CALL_t", synchrons);
                  Port buffer_port = BipUtil.getPort(buffer_type, in_s_def.getName());
                  Port filter_port = BipUtil.getPort(from_atom_type, client_callp_def.getName());
                  assert(buffer_port != null && filter_port != null);
                  
                  InnerPortReference in_s_ipr = BipCreator.createInnerPortReferenceUnbounded(buffer_port, buffer_instance);
                  InnerPortReference call_ipr = BipCreator.createInnerPortReferenceUnbounded(filter_port, from_comp_instance);
                  
                  BipCreator.createConnector(connector_t, ct, buffer_instance_name + "__" + m.getName() + "CALL",  new InnerPortReference[]{in_s_ipr, call_ipr});

                  String client_retp_name = from_iface_name + "__" + mname + "_RETp";

                  PortDefinition client_retp_def = BipUtil.getPortDefinition(from_atom_type, client_retp_name);
                  assert (client_retp_def != null);

                  PortType client_retp_type = client_retp_def.getType();
                  PortDefinition in_e_def = BipUtil.getPortDefinition(buffer_type, "in_E");
                  assert(in_e_def != null);

                  PortType in_e_type = in_e_def.getType();

                  synchrons = new ArrayList<PortType>();
                  synchrons.add(in_e_type);
                  synchrons.add(client_retp_type);
                  
                  connector_t = BipCreator.createRDVConnectorType(bip_module, buffer_instance_name + "__" + m.getName() + "RET_t", synchrons);
                  buffer_port = BipUtil.getPort(buffer_type, in_e_def.getName());
                  filter_port = BipUtil.getPort(from_atom_type, client_retp_def.getName());
                  assert(buffer_port != null && filter_port != null);

                  InnerPortReference in_e_ipr = BipCreator.createInnerPortReferenceUnbounded(buffer_port, buffer_instance);
                  InnerPortReference ret_ipr = BipCreator.createInnerPortReferenceUnbounded(filter_port, from_comp_instance);
                  BipCreator.createConnector(connector_t, ct, buffer_instance_name + "__" + m.getName() + "RET",  new InnerPortReference[]{in_e_ipr, ret_ipr});

              }
              
              
              /*
               * buffer to server side
               */
              String server_callp_name = to_iface_name + "__" + mname + "_CALLp";
              
              PortDefinition server_callp_def = BipUtil.getPortDefinition(to_atom_type, server_callp_name);
              // if null, Atom does not have the port, no need to create the connector.
              if (server_callp_def != null) {
                  PortType server_callp_type = server_callp_def.getType();
                  PortDefinition out_s_def = BipUtil.getPortDefinition(buffer_type, "out_S");
                  assert(out_s_def != null);

                  PortType out_s_type = out_s_def.getType();

                  List<PortType> synchrons = new ArrayList<PortType>();
                  synchrons.add(out_s_type);
                  synchrons.add(server_callp_type);

                  connector_t = BipCreator.createRDVConnectorType(bip_module, buffer_instance_name + "__" + m.getName() + "CALL_t", synchrons);
                  Port buffer_port = BipUtil.getPort(buffer_type, out_s_def.getName());
                  Port filter_port = BipUtil.getPort(to_atom_type, server_callp_def.getName());
                  assert(buffer_port != null && filter_port != null);
                  
                  InnerPortReference out_s_ipr = BipCreator.createInnerPortReferenceUnbounded(buffer_port, buffer_instance);
                  InnerPortReference call_ipr = BipCreator.createInnerPortReferenceUnbounded(filter_port, to_comp_instance);
                  
                  BipCreator.createConnector(connector_t, ct, buffer_instance_name + "__" + m.getName() + "CALL", new InnerPortReference[]{out_s_ipr, call_ipr});

                  String server_retp_name = to_iface_name + "__" + mname + "_RETp";

                  PortDefinition server_retp_def = BipUtil.getPortDefinition(to_atom_type, server_retp_name);
                  assert (server_retp_def != null);

                  PortType server_retp_type = server_retp_def.getType();
                  PortDefinition out_e_def = BipUtil.getPortDefinition(buffer_type, "out_E");
                  assert(out_e_def != null);

                  PortType out_e_type = out_e_def.getType();

                  synchrons = new ArrayList<PortType>();
                  synchrons.add(out_e_type);
                  synchrons.add(server_retp_type);
                  
                  buffer_port = BipUtil.getPort(buffer_type, out_e_def.getName());
                  filter_port = BipUtil.getPort(to_atom_type, server_retp_def.getName());
                  assert(buffer_port != null && filter_port != null);
                  
                  connector_t = BipCreator.createRDVConnectorType(bip_module, buffer_instance_name + "__" + m.getName() + "RET_t", synchrons);
                  InnerPortReference out_e_ipr = BipCreator.createInnerPortReferenceUnbounded(buffer_port, buffer_instance);
                  InnerPortReference ret_ipr = BipCreator.createInnerPortReferenceUnbounded(filter_port, to_comp_instance);
                  
                  BipCreator.createConnector(connector_t, ct, buffer_instance_name + "__" + m.getName() + "RET", new InnerPortReference[]{out_e_ipr, ret_ipr});

              }
          }
          
      }
  }
  
  public void visit(InstancesDescriptor input, Map<Object, Object> context)
      throws ADLException {

    if (!context.containsKey(BackendCommandLineHandler.BEAM_ENABLE_BIP_BACKEND)){
        return;
    }
    
    assert(input.instances.size() == 1);

    Module bip_module = (Module)context.get(BipDefinitionSourceGenerator.BEAM_BIP_MODEL);
    
    ComponentGraph comp_instance = null;
    
    for (ComponentGraph cg : input.instances){
      comp_instance = cg;
    }
    
    String n;
    
    // we don't support shared components...
    assert(comp_instance.getParents().length <= 1);
    
    if (comp_instance.getParents().length == 0){
      n = "TOPLEVEL"; // it has no name.
    } else {
      n = comp_instance.getNameInParent(comp_instance.getParents()[0]);
    }
    
    logger.log(Level.INFO, "Visiting instance..." + n);
    
    if (comp_instance.getSubComponents().length == 0){
        Definition instance_def = comp_instance.getDefinition();
        
        // if no decoration found, means that it is not a filter => do not translate it.
        Object o = instance_def.astGetDecoration(BeamFilterAnnotationProcessor.DEFINITION_HAS_INSTANCE_FILTER);
        if (o == null)
            return;
        
        AtomType at = BipUtil.getAtomTypeDefinition(
                BipDefinitionSourceGenerator.mindToBipMangleName(instance_def.getName()), 
                bip_module);

        assert(at != null);
        logger.log(Level.INFO, "  - found BIP type: " + at.getName());

        String parent_bip_def_name = BipDefinitionSourceGenerator.mindToBipMangleName(comp_instance.getParents()[0].getDefinition().getName());
        logger.log(Level.INFO, "  - parent type: " + parent_bip_def_name);
        
        CompoundType ct = BipUtil.getCompoundTypeDefinition(parent_bip_def_name, bip_module);
        assert (ct != null);
        
        Component c = BipCreator.createComponentInstance(n, ct, at);
        logger.log(Level.INFO, "    - added instance '" + n + "'");
        subcomponent_done++;
    } else {
        Definition instance_def = comp_instance.getDefinition();
        CompoundType ct = BipUtil.getCompoundTypeDefinition(
                BipDefinitionSourceGenerator.mindToBipMangleName(instance_def.getName()), 
                bip_module);
        assert(ct != null);
        logger.log(Level.INFO, "  - found BIP composite type: " + ct.getName());
        assert(bip_module instanceof ujf.verimag.bip.Core.Modules.System);
        
        logger.log(Level.INFO, "    - added as Root in system");
        BipCreator.createRoot(ct, n, (ujf.verimag.bip.Core.Modules.System)bip_module);
        
        root_input = input;
        root_ct = ct;
        subcomponent_count = comp_instance.getSubComponents().length - 1;
    }
    
    if (subcomponent_done == subcomponent_count && (root_input != null && root_ct != null)){
        createConnectors(bip_module, root_ct, root_input, context);
    }
  }

  public void endVisit(final ComponentGraph graph, Map<Object, Object> context) throws ADLException {
      
      if (!context.containsKey(BackendCommandLineHandler.BEAM_ENABLE_BIP_BACKEND)){
          return;
      }
      
      String output_path = "/" + BeamHelper.getBipFileName(context);

      File outputfile = outputFileLocatorItf.getCSourceOutputFile(output_path, context);

      try {
          PrintStream ps = new PrintStream(new FileOutputStream(outputfile));
          Reverse r = new Reverse(ps);
          Module bip_module = (Module)context.get(
                  BipDefinitionSourceGenerator.BEAM_BIP_MODEL);
          assert(bip_module != null);
          r.decompile(bip_module);
          ps.close();
      } catch (FileNotFoundException e) {
          throw new ADLException(IOErrors.WRITE_ERROR, e);
      } catch (BackendException e){
          throw new ADLException(IOErrors.WRITE_ERROR, e);
      }
  }

  public void startVisit(final ComponentGraph graph, Map<Object, Object> context) {
  }

}
