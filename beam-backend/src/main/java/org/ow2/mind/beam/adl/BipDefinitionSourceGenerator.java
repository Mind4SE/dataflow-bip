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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplateGroupLoader;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.objectweb.fractal.adl.ADLException;
import org.objectweb.fractal.adl.Definition;
import org.objectweb.fractal.adl.interfaces.Interface;
import org.objectweb.fractal.adl.interfaces.InterfaceContainer;
import org.objectweb.fractal.adl.types.TypeInterface;
import org.objectweb.fractal.adl.util.FractalADLLogManager;
import org.ow2.mind.InputResourceLocator;
import org.ow2.mind.adl.DefinitionSourceGenerator;
import org.ow2.mind.adl.ast.ASTHelper;
import org.ow2.mind.adl.ast.Attribute;
import org.ow2.mind.adl.ast.AttributeContainer;
import org.ow2.mind.adl.ast.Data;
import org.ow2.mind.adl.ast.ImplementationContainer;
import org.ow2.mind.adl.ast.MindInterface;
import org.ow2.mind.adl.ast.Source;
import org.ow2.mind.adl.idl.InterfaceDefinitionDecorationHelper;
import org.ow2.mind.adl.implementation.ImplementationLocator;
import org.ow2.mind.beam.BackendCommandLineHandler;
import org.ow2.mind.beam.annotation.BeamFilterAnnotationProcessor;
import org.ow2.mind.idl.IDLLoader;
import org.ow2.mind.idl.ast.InterfaceDefinition;
import org.ow2.mind.idl.ast.Method;
import org.ow2.mind.idl.ast.Parameter;
import org.ow2.mind.idl.ast.PointerOf;
import org.ow2.mind.idl.ast.PrimitiveType;
import org.ow2.mind.idl.ast.Type;
import org.ow2.mind.idl.ast.TypeDefReference;
import org.ow2.mind.io.OutputFileLocator;

import ujf.verimag.bip.Core.ActionLanguage.Actions.CompositeAction;
import ujf.verimag.bip.Core.ActionLanguage.Expressions.FunctionCallExpression;
import ujf.verimag.bip.Core.ActionLanguage.Expressions.VariableReference;
import ujf.verimag.bip.Core.Behaviors.AtomType;
import ujf.verimag.bip.Core.Behaviors.DataParameter;
import ujf.verimag.bip.Core.Behaviors.DataType;
import ujf.verimag.bip.Core.Behaviors.Expression;
import ujf.verimag.bip.Core.Behaviors.PetriNet;
import ujf.verimag.bip.Core.Behaviors.PortDefinition;
import ujf.verimag.bip.Core.Behaviors.PortType;
import ujf.verimag.bip.Core.Behaviors.State;
import ujf.verimag.bip.Core.Behaviors.Transition;
import ujf.verimag.bip.Core.Behaviors.Variable;
import ujf.verimag.bip.Core.Interactions.CompoundType;
import ujf.verimag.bip.Core.Modules.Module;
import ujf.verimag.bip.Core.Modules.OpaqueElement;
import ujf.verimag.bip.codegen.C2BIPUtil;
import ujf.verimag.bip.codegen.C2BIPVisitor;
import ujf.verimag.bip.codegen.InteractionPoint;
import ujf.verimag.bip.metamodelAPI.BipCreator;
import ujf.verimag.bip.metamodelAPI.BipUtil;

import com.google.inject.Inject;

/*
 * If you wonder why all the backend is not part of a single class...
 * The single would have to implement DefinitionSG *and* InstanceSG,
 * which in turn makes the class inherits from two conflicting interface:
 * each one implement VoidVisitor with different generic types, which
 * is not supported by Java.
 */

public class BipDefinitionSourceGenerator implements DefinitionSourceGenerator {
  
  final public static String BEAM_BIP_MODEL = "beam-bip-model";
  final public static String BEAM_BIP_ROOT_COMP_DEF = "beam-bip-root-comp-def";
  
  final public static String ENTRY_METHOD_IN_FILTERS = "filterctrl__act";
  
  final public static String PORTTYPE_BUFFER_PREFIX = "ptbuf_";
  
  protected static Logger logger = FractalADLLogManager
  .getLogger("beam-bip-visitor::definition");
  
  protected static int buffer_uniq_id = 0;
  
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
  public ImplementationLocator implementationLocatorItf;

  @Inject
  public IDLLoader idlLoaderItf;

  /** The {@link StringTemplateGroupLoader} client interface. */
  @Inject
  public StringTemplateGroupLoader   templateLoaderItf;

  protected ujf.verimag.bip.Core.Modules.System model;
  
  protected Set<String> header_types_for_buffers = new HashSet<String>();
  
  public BipDefinitionSourceGenerator(){
    this.model = BipCreator.createSystem("mysystem");
  }
  
  /**
   * Returns the StringTemplate template with the given
   * <code>templateName</code> name and found in the
   * <code>templateGroupName</code> group.
   * 
   * @param templateGroupName the groupName from which the template must be
   *          loaded.
   * @param templateName the name of the template.
   * @return a StringTemplate template
   * @see StringTemplateGroupLoader
   */
  protected StringTemplate getTemplate(final String templateGroupName,
      final String templateName) {
    final StringTemplateGroup templateGroup = templateLoaderItf.loadGroup(
        templateGroupName, AngleBracketTemplateLexer.class, null);
    return templateGroup.getInstanceOf(templateName);
  }
  /**
   * Mangles a mind component definition name into its corresponding
   * BIP component definition name
   * @param mindName the name of the mind component definition
   * @return the corresponding BIP component definition name
   */
  public static String mindToBipMangleName(String mindName){
      return mindName.replace(".","__");
  }
  
  /**
   * Converts a mind data type into a string
   * @param t the mind data type
   * @return a String representing the mind type
   * @throws ADLException in case the type cannot be stringified
   */
  private static String typeToString(Type t) throws ADLException{
      String pointer_suffix = "";
      String the_type = null;
      while (t instanceof PointerOf){
          pointer_suffix +="*";
          t = ((PointerOf)t).getType();
      }
      
      if (t instanceof PrimitiveType){
          PrimitiveType pt = (PrimitiveType)t;
          the_type = pt.getName() + pointer_suffix;
      } else if (t instanceof TypeDefReference){
          TypeDefReference pt = (TypeDefReference)t;
          the_type = pt.getName() + pointer_suffix;
      } else {
          throw new ADLException(BeamErrors.BEAM_ERROR);
      }
      
      assert(the_type != null);
      return the_type;
  }

  protected void createBufferAtom(String typedefs, String buffername, String typename){
      PetriNet behav = BipCreator.createPetriNet();
 
      AtomType buffer = BipCreator.createAtomType(behav, buffername, this.model);
      
      State idle = BipCreator.createState(behav, "IDLE", true);
      State in_state = BipCreator.createState(behav, "IN", true);
      State out_state = BipCreator.createState(behav, "OUT", true);
      State peek_state = BipCreator.createState(behav, "PEEK", true);
      
      DataType dt = BipCreator.createDataType(typename, this.model);
      DataType dsizet = BipCreator.createDataType("int", this.model);

      DataType fifo_type = BipCreator.createDataType("fifo_" + typename + buffer_uniq_id +"_t", this.model);

      DataParameter dpi = BipCreator.createDataParameter("ind", dt);
      DataParameter dpo = BipCreator.createDataParameter("outd", dt);

      PortType pt_buf = BipCreator.createPortType(PORTTYPE_BUFFER_PREFIX + buffername, this.model, new DataParameter[]{dpi});
      
      Variable inv = BipCreator.createVariable(dt, "inv", buffer, false, false);
      Variable outv = BipCreator.createVariable(dt, "outv", buffer, false, false);
      Variable sizev = BipCreator.createVariable(dsizet, "sizev", buffer, false, false);
      Variable peekv = BipCreator.createVariable(dsizet, "peekv", buffer, false, false);

      Variable innerfifo = BipCreator.createVariable(fifo_type, "innerfifo", buffer, false, false);
      
      PortDefinition pin_s = BipCreator.createPortDefinitionAndExport(pt_buf, "in_S", new Variable[]{inv}, buffer);
      PortDefinition pin_e = BipCreator.createPortDefinitionAndExport(pt_buf, "in_E", new Variable[]{inv}, buffer);
      PortDefinition pout_s = BipCreator.createPortDefinitionAndExport(pt_buf, "out_S", new Variable[]{outv}, buffer);
      PortDefinition pout_e = BipCreator.createPortDefinitionAndExport(pt_buf, "out_E", new Variable[]{outv}, buffer);
      PortDefinition ppeek_s = BipCreator.createPortDefinitionAndExport(pt_buf, "peek_S", new Variable[]{peekv}, buffer);
      PortDefinition ppeek_e = BipCreator.createPortDefinitionAndExport(pt_buf, "peek_E", new Variable[]{outv}, buffer);
      PortDefinition psize = BipCreator.createPortDefinitionAndExport(pt_buf, "size_S", new Variable[]{sizev}, buffer);

      // push transition
      Transition push_trans = BipCreator.createTransition(pin_s, null, idle, in_state, buffer);
      CompositeAction ca = BipCreator.createCompositeAction();
      push_trans.setAction(ca);
      
      FunctionCallExpression fce = BipCreator.createFunctionCallExpression("_push_" + typename + buffer_uniq_id, new Expression[]{
              BipCreator.createVariableReference(inv),
              BipCreator.createFunctionCallExpression("R", new Expression[]{BipCreator.createVariableReference(innerfifo)})
      });
      ca.getContent().add(fce);
      
      BipCreator.createTransition(pin_e, null, in_state, idle, buffer);
      
      // Peek transition
      Transition peek_trans = BipCreator.createTransition(ppeek_s, null, idle, peek_state, buffer);
      BipCreator.createTransition(ppeek_e, null, peek_state, idle, buffer);
      ca = BipCreator.createCompositeAction();
      peek_trans.setAction(ca);
      VariableReference out_vr = BipCreator.createVariableReference(outv);
      fce = BipCreator.createFunctionCallExpression("_peek_" + typename + buffer_uniq_id, new Expression[]{
              BipCreator.createFunctionCallExpression("R", new Expression[]{BipCreator.createVariableReference(innerfifo)})
      });
      
      ca.getContent().add(BipCreator.createAssignmentAction(out_vr, fce));
      
      // pop transition
      Transition pop_trans = BipCreator.createTransition(pout_s, null, idle, out_state, buffer);
      BipCreator.createTransition(pout_e, null, out_state, idle, buffer);
      ca = BipCreator.createCompositeAction();
      pop_trans.setAction(ca);
      out_vr = BipCreator.createVariableReference(outv);
      fce = BipCreator.createFunctionCallExpression("_pop_" + typename + buffer_uniq_id, new Expression[]{
              BipCreator.createFunctionCallExpression("R", new Expression[]{BipCreator.createVariableReference(innerfifo)})
      });
      
      ca.getContent().add(BipCreator.createAssignmentAction(out_vr, fce));

      
      
      StringTemplate buffer_st =  getTemplate("st.beam.bip.Buffer", "SimpleFifo");
      assert(buffer_st != null);
      
      buffer_st.setAttribute("type", typename);
      buffer_st.setAttribute("size", 256);
      buffer_st.setAttribute("id", buffer_uniq_id);
      
      OpaqueElement header_oe = BipCreator.createOpaqueElementFromCCode(typedefs + buffer_st.toString(),true);
      BipUtil.addDeclarationToModule(this.model, header_oe);
      buffer_uniq_id++;
  }
  // ---------------------------------------------------------------------------
  // Implementation of the Visitor interface
  // ---------------------------------------------------------------------------

  public void visit(Definition input, Map<Object, Object> context)
      throws ADLException {
    logger.log(Level.INFO, "Visiting definition " + input.getName());
    
    if (!context.containsKey(BackendCommandLineHandler.BEAM_ENABLE_BIP_BACKEND)){
        return;
    }
        
    if (!context.containsKey(BEAM_BIP_MODEL)){
        context.put(BEAM_BIP_MODEL, this.model);
    }

    if (ASTHelper.isComposite(input)){
        CompoundType ct = BipCreator.createCompoundType(mindToBipMangleName(input.getName()), this.model);
        context.put(BEAM_BIP_ROOT_COMP_DEF, ct);
    } else {
        // if no decoration found, means that it is not a filter => do not translate it.
        Object o = input.astGetDecoration(BeamFilterAnnotationProcessor.DEFINITION_HAS_INSTANCE_FILTER);
        if (o == null)
            return;
        
        assert(input instanceof ImplementationContainer);
        ImplementationContainer ic = (ImplementationContainer) input;
        
        Source[] srcs = ic.getSources();
        // Currently, we do not support components with multiple source files.
        // All files could be concatenated into a single file and then parsed.
        assert(srcs.length == 1);
        
        List<InteractionPoint> ips = new ArrayList<InteractionPoint>();
        
        assert (input instanceof InterfaceContainer);
        for(final Interface iface: ((InterfaceContainer)input).getInterfaces()){
            assert(iface instanceof MindInterface);
            MindInterface miface = (MindInterface)iface;
            InterfaceDefinition idef = 
                InterfaceDefinitionDecorationHelper.getResolvedInterfaceDefinition(miface, 
                        idlLoaderItf, context);
            
            /*
             * Following SB are used to create a union type, wrapped in a struct. One
             * for each interface. This is used to create a fifo component that will hold
             * the data to be queued.
             * This is overkill for the current task, as our FIFO should hold only elements of the same type.
             */
            StringBuffer iface_struct = new StringBuffer();
            iface_struct.append("\ntypedef struct {\n");
            iface_struct.append("   int tid;\n");
            iface_struct.append("   union {\n");
            
                for (final Method m: idef.getMethods()){
                    String fname = miface.getName() + "__" + m.getName();
                    String mname = m.getName();
                    StringBuffer struct_declaration = new StringBuffer();

                    struct_declaration.append("struct {\n");
                    
                    Type t = m.getType();
                    String return_type = typeToString(t);
                    
                    Parameter[] params = m.getParameters();
                    List<String> param_types = new ArrayList<String>();
                    for (final Parameter param: params){
                        Type param_t = param.getType();
                        String param_t_name = typeToString(param_t); 
                        param_types.add(param_t_name);
                        
                        struct_declaration.append("       " + param_t_name + " " + param.getName() + ";\n");
                    }
                    String[] params_string = param_types.toArray(new String[0]);
                    InteractionPoint ip = new InteractionPoint(return_type, fname, params_string);
                    ips.add(ip);
                    
//                    struct_declaration.append("} " + mname + "_arg_t;\n");
                    struct_declaration.append("    } ");
                    iface_struct.append("      " + struct_declaration.toString() + mname + ";\n");
                }
                String iface_union_wrapped_tname = idef.getName().replace('.', '_') + "_arg_t";
                iface_struct.append("   } data;\n} " + iface_union_wrapped_tname + ";\n");
                
                if (miface.getRole().equals(TypeInterface.CLIENT_ROLE) && !header_types_for_buffers.contains(iface_union_wrapped_tname)){
                    createBufferAtom(iface_struct.toString(), idef.getName().replace('.', '_'), iface_union_wrapped_tname);
                    header_types_for_buffers.add(iface_union_wrapped_tname);
                }
        }

        Map<String,String> attributes = new HashMap<String,String>();
        
        assert (input instanceof AttributeContainer);
        for(final Attribute attr: ((AttributeContainer)input).getAttributes()){
            attributes.put(attr.getType(), "attr_" + attr.getName());
        }
        
        InteractionPoint[] ips_array = ips.toArray(new InteractionPoint[0]);
        
        // we loop over all srcs. First version of the code forces
        // the list to contain only one element (looping only once...)
        for (final Source src : srcs){
            URL f = implementationLocatorItf.findResource(src.getPath(), context);
            String local_c_context = 
                "#define BEAM_PARSE_SHIELD\n" +
                "#define START_ACT_LOOP \n" +
                "#define END_ACT_LOOP \n" +
                "\n/* for interface methods */ \n" +
                "#define METH(x,y) x##__##y\n" +
                "#define CALL(x,y) x##__##y\n" +
                "\n/* for private method */\n"+
                "#define PMETH(x) __priv_##x\n"+
                "#define PCALL(x) __priv_##x\n" +
                
                "#define ATTR(x) attr_##x\n";
            
//            local_c_context += "typedef struct _frame_t {  int len;  unsigned char *buffer;} frame_t;\n";
            
            Data data = ic.getData();
            if (data != null && data.getCCode() != null){
                local_c_context += data.getCCode() + "\n";
            }
            
            try {
                C2BIPVisitor c2bipVisitor= new C2BIPVisitor(ENTRY_METHOD_IN_FILTERS,
                        mindToBipMangleName(input.getName()), attributes,
                        this.model, true);
                System.out.println("File: " + f.toString());
                
                // SSZ - Inspired from OW2 Mind SVN r1202/r1203 comparison (2 arguments were missing) 
                Module res = C2BIPUtil.c2bipAsModel(f.getFile(),
                		ENTRY_METHOD_IN_FILTERS,
                        mindToBipMangleName(input.getName()), 
                		true, local_c_context, 
                        ips_array, this.model, c2bipVisitor);
            } catch (Exception e) {
                throw new ADLException(BeamErrors.BEAM_ERROR, e);
            }
        }
    }
    
  }

}
