#ifndef _BUFFER_MAIN_H /* should be useless, but never hurts */
#define _BUFFER_MAIN_H

#ifdef FIFO_DETECT_BAD_USE
#include "buffer-protect.h"
#else
#include "buffer-unsafe.h"
#endif

#endif
