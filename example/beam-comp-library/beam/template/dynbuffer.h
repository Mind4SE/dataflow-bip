#ifndef __BEAM_DYNBUFFER_MAIN__H /* should be useless, but never hurts */
#define __BEAM_DYNBUFFER_MAIN__H

#ifdef FIFO_DETECT_BAD_USE
#include "dynbuffer-protect.h"
#else
#include "dynbuffer-unsafe.h"
#endif

#endif /*  __BEAM_DYNBUFFER_MAIN__H */
