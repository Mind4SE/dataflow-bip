package org.ow2.mind.beam;

public interface Constants {
  final String BEAM_FILTER_CTRL_IFACE_NAME = "filterctrl";
  final String BEAM_SCHEDULER_COMP_NAME = "sched";
  final String BEAM_SCHEDULER_COMP_DEF_NAME = "Scheduler";

  final String BEAM_BUFFER_COMP_PREFIX_NAME = "buffer__";
  
  final String BEAM_FILTER_CTRL_IFACE_TYPE = "beam.BeamFilter";
  
  final String BEAM_FIFO_CTRL_IFACE_NAME = "control";
  final String BEAM_FIFO_CTRL_IFACE_TYPE = "beam.BeamBufferControl";
  /*
   * Beam specific key for the compiler 'context'
   */
  final String BEAM_CONTEXT_FILTERS_COMP = "beam-filters-comp";
  final String BEAM_CONTEXT_FIFO_COMP = "beam-fifo-comp";
}
