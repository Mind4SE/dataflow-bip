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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.fractal.adl.ADLException;
import org.objectweb.fractal.adl.Definition;
import org.objectweb.fractal.adl.Node;
import org.objectweb.fractal.adl.interfaces.InterfaceContainer;
import org.objectweb.fractal.adl.types.TypeInterface;
import org.objectweb.fractal.adl.util.FractalADLLogManager;
import org.ow2.mind.InputResourcesHelper;
import org.ow2.mind.adl.annotation.ADLLoaderPhase;
import org.ow2.mind.adl.annotation.AbstractADLLoaderAnnotationProcessor;
import org.ow2.mind.adl.ast.ASTHelper;
import org.ow2.mind.adl.ast.Binding;
import org.ow2.mind.adl.ast.BindingContainer;
import org.ow2.mind.adl.ast.Component;
import org.ow2.mind.adl.ast.MindInterface;
import org.ow2.mind.adl.idl.InterfaceDefinitionDecorationHelper;
import org.ow2.mind.annotation.Annotation;
import org.ow2.mind.beam.CommandLineHandler;
import org.ow2.mind.beam.Constants;
import org.ow2.mind.idl.ast.InterfaceDefinition;

public class BeamFilterAnnotationProcessor
    extends
      AbstractADLLoaderAnnotationProcessor 
    implements
      Constants {

    public static String DEFINITION_HAS_INSTANCE_FILTER = "definition-has-instance-filter";
    
  protected static Logger logger = FractalADLLogManager
  .getLogger("beam-filter-annot");

  /**
   * Used to gather all filters encountered during parsing
   */
  static List<Component> filters = new ArrayList<Component>();
  
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
  

  @SuppressWarnings("unchecked")
  protected void addFilterToContext(Component filter, Map<Object,Object> context){
    List<Component> ctx_filters;
    
    if (context.containsKey(BEAM_CONTEXT_FILTERS_COMP)){
      ctx_filters = (List<Component>) context.get(BEAM_CONTEXT_FILTERS_COMP);
    } else {
      ctx_filters = new ArrayList<Component>();
      context.put(BEAM_CONTEXT_FILTERS_COMP, ctx_filters);
    }
    ctx_filters.add(filter);
  }
  // ---------------------------------------------------------------------------
  // Implementation of the ADLLoaderAnnotationProcessor interface
  // ---------------------------------------------------------------------------

  public Definition processAnnotation(final Annotation annotation,
      final Node node, final Definition definition, final ADLLoaderPhase phase,
      final Map<Object, Object> context) throws ADLException {
    assert annotation instanceof BeamFilter;
    
    assert node instanceof Component;

    boolean apply_ast_transformations = !context.containsKey(CommandLineHandler.BEAM_CLI_GEN_BIP);

    Component c = (Component) node;
    if (phase == ADLLoaderPhase.AFTER_PARSING){
        if (!apply_ast_transformations){
            logger.log(Level.INFO, "not doing anything as --beam-bip used");
            return null;
        }
        
      filters.add(c);
      addFilterToContext(c, context);
      
      logger.log(Level.INFO, "Filter : " + c.getName() + "(#" + filters.size() + ")");

    } else if (phase == ADLLoaderPhase.AFTER_EXTENDS){
      logger.log(Level.INFO, "Handling filter : " + c.getName());

      Definition def = ASTHelper.getResolvedComponentDefinition(c, loaderItf, context);
      assert(def != null);
      assert def instanceof InterfaceContainer;
      def.astSetDecoration(DEFINITION_HAS_INSTANCE_FILTER, Boolean.TRUE);

      if (!apply_ast_transformations){
          logger.log(Level.INFO, "not doing anything as --beam-bip used");
          return null;
      }
      
      /*
       * Create Scheduler -> filter binding
       *  - add new client interface to scheduler
       *  - bind it to the filter
       */
      Component sched_comp = ASTHelper.getComponent(definition, BEAM_SCHEDULER_COMP_NAME);
      
      assert(sched_comp != null);
      Definition sched_def = ASTHelper.getResolvedComponentDefinition(sched_comp, loaderItf, context);
      assert(sched_def != null);
      assert sched_def instanceof InterfaceContainer;
      
      MindInterface sched_client = ASTHelper.newClientInterfaceNode(nodeFactoryItf, 
          c.getName(),
          BEAM_FILTER_CTRL_IFACE_TYPE);
      processItf(definition, sched_client, definition, context);

      ((InterfaceContainer)sched_def).addInterface(sched_client);
      logger.log(Level.INFO, " - added client iface '" + sched_client.getName() 
          +"' on '" + sched_comp.getName() + "'");

      Binding sched_filter_b = ASTHelper.newBinding(nodeFactoryItf);
      sched_filter_b.setFromComponent(BEAM_SCHEDULER_COMP_NAME);
      sched_filter_b.setFromInterface(c.getName());
      sched_filter_b.setToComponent(c.getName());
      sched_filter_b.setToInterface(BEAM_FILTER_CTRL_IFACE_NAME);

      assert(definition instanceof BindingContainer);
      ((BindingContainer) definition).addBinding(sched_filter_b);
      
      logger.log(Level.INFO, " - added binding '" + 
          sched_filter_b.getFromComponent() + "." + sched_filter_b.getFromInterface() + " to " +
          sched_filter_b.getToComponent() + "." + sched_filter_b.getToInterface() + "'");
    }
    return null;
  }
}
