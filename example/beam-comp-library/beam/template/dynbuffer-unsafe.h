#ifndef __BEAM_DYNBUFFER_TEMPLATE__H
#define __BEAM_DYNBUFFER_TEMPLATE__H

#include <stdlib.h>

#define FIFO_INIT_N(ptype,id) _dynfifo_init_##ptype##id
#define FIFO_INIT(ptype,id)				\
  void FIFO_INIT_N(ptype,id) (TYPE_NAME(ptype,id) *f) {	\
     f->size = 0; \
     f->first = NULL; \
     f->last = NULL; \
  }


#define FIFO_SIZE_N(ptype,id) _dynfifo_size_##ptype##id
#define FIFO_SIZE(ptype,usize,id)				\
  int FIFO_SIZE_N(ptype,id) (TYPE_NAME(ptype,id) *f) {		\
     return f->size;  \
  }


#define FIFO_PUSH_N(ptype,id) _dynpush_##ptype##id
#define FIFO_PUSH(ptype,usize,id)				\
  void FIFO_PUSH_N(ptype,id) (ptype x, TYPE_NAME(ptype,id) *f) {	\
      ELT_TYPE_NAME(ptype,id) *new = malloc(sizeof(ELT_TYPE_NAME(ptype,id)));		\
      new->data = x; \
      new->next = f->first; \
      new->prev = NULL; \
      if (f->size == 0){ \
         f->last = new; \
      } else { \
         f->first->prev = new; \
      } \
      f->first = new; \
      f->size++; \
  }

#define FIFO_POP_N(ptype,id) _dynpop_##ptype##id
#define FIFO_POP(ptype,usize,id)			\
  ptype FIFO_POP_N(ptype,id) (TYPE_NAME(ptype,id) *f) {	\
     ELT_TYPE_NAME(ptype,id) *elt = f->last; \
     ptype res = f->last->data; \
     if (f->last->prev != NULL) \
        f->last->prev->next = NULL; \
     f->last = f->last->prev; \
     f->size--; \
     if (f->size == 0){ \
       f->first = NULL; \
     } \
     free(elt); \
     return res; \
  }

#define FIFO_PEEK_N(ptype,id) _dynpeek_##ptype##id
#define FIFO_PEEK(ptype,usize,id)				\
  ptype FIFO_PEEK_N(ptype,id) (int i, TYPE_NAME(ptype,id) *f) {	\
     int _i=i; \
     ELT_TYPE_NAME(ptype,id) *elt = f->last; \
     while (_i){ \
        if (elt->prev != NULL){			\
           elt = elt->prev; \
           _i--; \
        } else		    \
           for(;;); \
     } \
     return elt->data; \
  }

#define FIFO_TOP_N(ptype,id) _dyntop_##ptype##id
#define FIFO_TOP(ptype,id)			\
  ptype FIFO_TOP_N(ptype,id) (TYPE_NAME(ptype,id) *f) {	\
     return f->last->data; \
  }

#endif /*  __BEAM_DYNBUFFER_TEMPLATE__H */
