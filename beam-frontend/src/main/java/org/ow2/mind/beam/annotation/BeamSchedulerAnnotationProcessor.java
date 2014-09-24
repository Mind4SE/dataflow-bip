/**
 * Copyright (C) 2010 STMicroelectronics
 * Copyright (C) 2010 VERIMAG

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
 */

package org.ow2.mind.beam.annotation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.stringtemplate.StringTemplate;
import org.objectweb.fractal.adl.ADLException;
import org.objectweb.fractal.adl.Definition;
import org.objectweb.fractal.adl.Node;
import org.objectweb.fractal.adl.interfaces.Interface;
import org.objectweb.fractal.adl.interfaces.InterfaceContainer;
import org.objectweb.fractal.adl.types.TypeInterface;
import org.objectweb.fractal.adl.util.ClassLoaderHelper;
import org.objectweb.fractal.adl.util.FractalADLLogManager;
import org.ow2.mind.CommonASTHelper;
import org.ow2.mind.InputResourcesHelper;
import org.ow2.mind.adl.annotation.ADLLoaderPhase;
import org.ow2.mind.adl.annotation.AbstractADLLoaderAnnotationProcessor;
import org.ow2.mind.adl.ast.ASTHelper;
import org.ow2.mind.adl.ast.Binding;
import org.ow2.mind.adl.ast.BindingContainer;
import org.ow2.mind.adl.ast.Component;
import org.ow2.mind.adl.ast.ComponentContainer;
import org.ow2.mind.adl.ast.DefinitionReference;
import org.ow2.mind.adl.ast.ImplementationContainer;
import org.ow2.mind.adl.ast.MindInterface;
import org.ow2.mind.adl.ast.Source;
import org.ow2.mind.adl.idl.InterfaceDefinitionDecorationHelper;
import org.ow2.mind.annotation.Annotation;
import org.ow2.mind.annotation.AnnotationHelper;
import org.ow2.mind.beam.CommandLineHandler;
import org.ow2.mind.beam.Constants;
import org.ow2.mind.idl.ast.InterfaceDefinition;


public class BeamSchedulerAnnotationProcessor
    extends
      AbstractADLLoaderAnnotationProcessor 
    implements
      Constants {

  public final String SCHEDULER_DEBUG_EXEC_TRACE = "scheduler-trace";
  public final String SCHEDULER_DEBUG_FIFO_SIZE = "fifo-size-trace";

  public URL findResource(final String name, final Map<Object, Object> context) {
    return ClassLoaderHelper.getClassLoader(this, context).getResource(name);
  }
  
  protected static Logger logger = FractalADLLogManager
  .getLogger("beam-scheduler-annot");

  static Definition scheduler_definition = null;
  /*
   * Stolen from "InterfaceSignatureLoader"
   */
  protected void processItf(final Definition def, final TypeInterface itf,
      final Definition container, final Map<Object, Object> context)
      throws ADLException {
    InterfaceDefinition itfDef;
    try {
      itfDef = itfSignatureResolverItf.resolve(itf, container, context);
    } catch (final ADLException e) {
      throw e;
    }

    InterfaceDefinitionDecorationHelper.setResolvedInterfaceDefinition(itf, itfDef);
    InputResourcesHelper.addInputResources(def, InputResourcesHelper
        .getInputResources(itfDef));
  }

  /**
   * Creates implementation from an existing one.
   * @param filters The list of all filters in the system
   * @param kindarg The arguments for the generation
   * @return
   */
  protected Source createFromExistingImplementation(List<Component> filters, 
      List<Component> fifos, String[] kindarg, Map<Object, Object> context){
    logger.log(Level.INFO, "  - Loading scheduler template for " + kindarg[0]);
    Source src =null;
    URL existing_implem = findResource(kindarg[0], context);

    BufferedReader br;
    try {
      br = new BufferedReader(new FileReader(existing_implem.getPath()));

      src = CommonASTHelper.newNode(nodeFactoryItf, "source", Source.class);
      String line = br.readLine();
      StringBuffer sb = new StringBuffer();
      while(line != null){
        sb.append(line + "\n");
        line = br.readLine();
      }
      src.setCCode(sb.toString());
      br.close();
      // FIXME use a real error handling mechanism
    } catch (FileNotFoundException e) {
      assert(false);
    } catch (IOException e) {
      assert(false);
    }
    return src;
  }
  
  /**
   * Creates implementation from a template.
   * @param filters The list of all filters in the system
   * @param kindarg The arguments for the generation
   * @return
   * @throws ADLException 
   */
  protected Source createFromTemplateImplementation(List<Component> filters, 
      List<Component> fifos, String[] kindarg, Map<Object, Object> context) throws ADLException{
    logger.log(Level.INFO, "  - Loading scheduler template for " + kindarg[0]);

    StringTemplate sched_st =  getTemplate("st.beam.scheduler.Templates", kindarg[0]);
    
 

    Map<String, Map<String, BeamInterface>> filters_server_ifaces = new HashMap<String, Map<String, BeamInterface>>();
    Map<String, Map<String, Component>>     fifos_for_comps       = new HashMap<String, Map<String, Component>>();
    Set<Component> no_input_filters = new HashSet<Component>();
    
    for (Component c: filters){
      c.getDefinitionReference();
      Definition def = ASTHelper.getResolvedComponentDefinition(c, loaderItf, context);

      Set<MindInterface> s = (Set<MindInterface>)def.astGetDecoration(BeamFifoAnnotationProcessor.WAS_SERVER_INTERFACE);
      if (s == null || s.isEmpty()){
        no_input_filters.add(c);
        continue;
      }
      
      Map<String, Component> fifo_for_comp = new HashMap<String, Component>();
      fifos_for_comps.put(c.getName(), fifo_for_comp);

      for (Interface iface : s){
        for (Component fifo: fifos){
          if (fifo.getName().endsWith(c.getName() + "_" + iface.getName())){
            fifo_for_comp.put(iface.getName(), fifo);
          }
        }
      }

      
      Map<String, BeamInterface> server_iface_map = new HashMap<String, BeamInterface>();
      filters_server_ifaces.put(c.getName(), server_iface_map);

      for (MindInterface mif : s){
        logger.log(Level.INFO, "iface: " + mif.getName());
        BeamInterface a = AnnotationHelper.getAnnotation(mif, BeamInterface.class);
        if (a == null)
          assert(false);
        
        server_iface_map.put(mif.getName(), a);
      }
    }
    
    sched_st.setAttribute("fifos_for_comps", fifos_for_comps);
    sched_st.setAttribute("no_input_filters", no_input_filters);
    sched_st.setAttribute("filters", filters);
    sched_st.setAttribute("fifos", fifos);
    sched_st.setAttribute("server_ifaces", filters_server_ifaces);
    
    Set<String> debug_set = (Set<String>) context.get(CommandLineHandler.BEAM_EXEC_DEBUG_MAP);
    if (debug_set == null || !debug_set.contains(SCHEDULER_DEBUG_EXEC_TRACE)){
      sched_st.setAttribute("debug", false);
    } else {
      sched_st.setAttribute("debug", true);
    }
    
    if (debug_set == null || !debug_set.contains(SCHEDULER_DEBUG_FIFO_SIZE)){
    	sched_st.setAttribute("debugfifosize", false);
    } else {
    	sched_st.setAttribute("debugfifosize", true);
    }

    final Source src = CommonASTHelper.newNode(nodeFactoryItf, "source", Source.class);
    src.setCCode(sched_st.toString());
    return src;
  }
  
  /**
   * Creates implementation for an automatic scheduler
   * @param filters The list of all filters in the system
   * @param kindarg The arguments for the automatic generation
   * @return
   */
  protected Source createAutomaticSchedulerImplementation(List<Component> filters, 
      List<Component> fifos, String[] kindarg, Map<Object, Object> context){
    
    String kindarg0 = kindarg[0];

    if (kindarg0.equals("RoundRobin")){
      StringBuffer inline_code = new StringBuffer();
      
      inline_code.append("int METH(main, main) (int argc, char *argv[]){\n");
      
      for (Component fifo: fifos){
        inline_code.append("CALL("  + fifo.getName() + ", init)();\n");
      }
      
      inline_code.append("for(;;){\n");

      for (Component filter : filters){
        inline_code.append("CALL(" + filter.getName()+ ", act)(1);\n");
      }

      inline_code.append("}\n");
      inline_code.append("return 0;\n}\n");

      final Source src = CommonASTHelper.newNode(nodeFactoryItf, "source", Source.class);
      src.setCCode(inline_code.toString());
      return src;
    } else {
      assert(false) : "unable to create automatic schedule for " + kindarg0;
      return null;
    }
  }
  
  /**
   * Creates the scheduler component. The ADL definition is generated. The implementation
   * can be entirely synthesized or only partly synthesized
   * @param container_def the definition in which to add the scheduler component
   * @param context the compiler context
   * @return the scheduler component
   * @throws ADLException
   */
  protected Component loadSchedulerComponent(Definition container_def, 
      final Map<Object,Object> context)
      throws ADLException{
    
    assert(scheduler_definition == null);
    logger.log(Level.INFO, "Creating scheduler component");
    
    String def_name = "beam.generated." + BEAM_SCHEDULER_COMP_DEF_NAME;
    Definition new_sched_def = ASTHelper.newPrimitiveDefinitionNode(nodeFactoryItf, def_name, 
        (DefinitionReference[]) null);
    MindInterface iface = ASTHelper.newServerInterfaceNode(nodeFactoryItf,
        "main", "boot.Main");

    processItf(container_def, iface, container_def, context);

    assert(new_sched_def instanceof InterfaceContainer);
    assert(new_sched_def instanceof ImplementationContainer);
    
    ((InterfaceContainer)new_sched_def).addInterface(iface);

    DefinitionReference dr = ASTHelper.newDefinitionReference(nodeFactoryItf, 
        def_name);
    
    ASTHelper.setResolvedDefinition(dr, new_sched_def);

    Component sched_comp = ASTHelper.newComponent(
        nodeFactoryItf, 
        BEAM_SCHEDULER_COMP_NAME, 
        dr);
    
    ASTHelper.setResolvedComponentDefinition(sched_comp, new_sched_def);

    logger.log(Level.INFO, "  - Adding scheduler in container component definition '" +
        container_def.getName() + "'");
    
    assert(container_def instanceof ComponentContainer);
    ((ComponentContainer) container_def).addComponent(sched_comp);
    
    
    /*
     * bind it to the this.main interface
     */
    Binding sched_main_b = ASTHelper.newBinding(nodeFactoryItf);
    sched_main_b.setFromComponent("bootstrap");
    sched_main_b.setFromInterface("entryPoint");
    sched_main_b.setToComponent(BEAM_SCHEDULER_COMP_NAME);
    sched_main_b.setToInterface("main");

    assert(container_def instanceof BindingContainer);
    ((BindingContainer) container_def).addBinding(sched_main_b);
    
    scheduler_definition = new_sched_def;
    return sched_comp;
  }
  // ---------------------------------------------------------------------------
  // Implementation of the ADLLoaderAnnotationProcessor interface
  // ---------------------------------------------------------------------------

  public Definition processAnnotation(final Annotation annotation,
      final Node node, final Definition definition, final ADLLoaderPhase phase,
      final Map<Object, Object> context) throws ADLException {
    assert annotation instanceof BeamScheduler;
    
    if (context.containsKey(CommandLineHandler.BEAM_CLI_GEN_BIP)){
      logger.log(Level.INFO, "not doing anything as --beam-bip used");
      return null;
    }
    
    BeamScheduler beam_sched_annot = (BeamScheduler) annotation;
    
    assert node instanceof Definition;

    Definition def = (Definition) node;
    
    if (phase == ADLLoaderPhase.AFTER_EXTENDS){
      logger.log(Level.INFO, "Handling (adding scheduler comp) definition : " + def.getName());
      
      /*
       * Create Scheduler -> filter binding
       *  - add new client interface to scheduler
       *  - bind it to the filter
       */
      Component sched_comp;
      
      sched_comp = loadSchedulerComponent(def, context);
      
      assert(sched_comp != null);
      
    } else if (phase == ADLLoaderPhase.AFTER_CHECKING){
      assert(context.containsKey(BEAM_CONTEXT_FILTERS_COMP));
      
      List<Component> filters = (List<Component>) context.get(BEAM_CONTEXT_FILTERS_COMP);
      List<Component> fifos = (List<Component>) context.get(BEAM_CONTEXT_FIFO_COMP);

      String kind = beam_sched_annot.kind;
      String args = "";
      for (String i: beam_sched_annot.arg){
        args += i + ",";
      }
      if (kind.equals("automatic")){
        logger.log(Level.INFO, "  - using automatic implementation, with arg: " + args);
        Source implem = createAutomaticSchedulerImplementation(filters,fifos, beam_sched_annot.arg, context);
        logger.log(Level.INFO, "  - Adding implementation code in scheduler");
        ((ImplementationContainer)scheduler_definition).addSource(implem);
      } else if (kind.equals("template")){
        logger.log(Level.INFO, "  - creating implementation from template, with arg: " + args);
        Source implem = createFromTemplateImplementation(filters, fifos, beam_sched_annot.arg, context);
        logger.log(Level.INFO, "  - Adding implementation code in scheduler");
        ((ImplementationContainer)scheduler_definition).addSource(implem);
      } else if (kind.equals("existing")) {
        logger.log(Level.INFO, "  - creating implementation from existing, with arg: " + args);
        Source implem = createFromExistingImplementation(filters, fifos, beam_sched_annot.arg, context);
        logger.log(Level.INFO, "  - Adding implementation code in scheduler");
        ((ImplementationContainer)scheduler_definition).addSource(implem);
      } else {
        // FIXME: use a real error handling mechanism
        assert(false): "unknown kind";
      }

    }
    return null;
  }
}
