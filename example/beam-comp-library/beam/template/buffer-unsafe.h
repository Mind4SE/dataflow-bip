#ifndef __BEAM_BUFFER_TEMPLATE__H
#define __BEAM_BUFFER_TEMPLATE__H

//typedef int size_tt;

#define FIFO_INIT_N(ptype,id) _fifo_init_##ptype##id
#define FIFO_INIT(ptype,id)				\
  void FIFO_INIT_N(ptype,id) (TYPE_NAME(ptype,id) *f) {	\
  f->e = -1;\
  f->s = 0;\
}

#define FIFO_SIZE_N(ptype,id) _fifo_size_##ptype##id
#define FIFO_SIZE(ptype,size,id)				\
  int FIFO_SIZE_N(ptype,id) (TYPE_NAME(ptype,id) *f) {		\
  if (f->e == -1) return 0;\
  if (f->e == f->s) return size;\
  return (f->e-f->s>0?size-f->e+f->s:f->s-f->e);\
}


#define FIFO_PUSH_N(ptype,id) _push_##ptype##id
#define FIFO_PUSH(ptype,size,id)				\
  void FIFO_PUSH_N(ptype,id) (ptype x, TYPE_NAME(ptype,id) *f) {	\
  if (f->e == -1) f->e = f->s;\
  f->data[f->s] = x; f->s = (f->s+1)%size;\
}



#define FIFO_POP_N(ptype,id) _pop_##ptype##id
#define FIFO_POP(ptype,size,id)			\
  ptype FIFO_POP_N(ptype,id) (TYPE_NAME(ptype,id) *f) {	\
  ptype t = f->data[f->e];\
  f->e = (f->e+1)%size;\
  if (f->e == f->s) f->e = -1;\
  return t;\
}


#define FIFO_PEEK_N(ptype,id) _peek_##ptype##id
#define FIFO_PEEK(ptype,size,id)				\
  ptype FIFO_PEEK_N(ptype,id) (int i, TYPE_NAME(ptype,id) *f) {	\
    ptype t = f->data[(f->e+i)%size];				\
  return t; \
}



#define FIFO_TOP_N(ptype,id) _top_##ptype##id
#define FIFO_TOP(ptype,id)			\
  ptype FIFO_TOP_N(ptype,id) (TYPE_NAME(ptype,id) *f) {	\
  ptype t = f->data[f->e];\
  return t; \
}


#define FIFO_REQUEUE_N(ptype,id) _requeue_##ptype##id
#define FIFO_REQUEUE(ptype,id)						\
  void FIFO_REQUEUE_N(ptype,id) (ptype in, ptype out, TYPE_NAME(ptype,id) *f){ \
    FIFO_PUSH_N(ptype,id)(out, f);					\
    FIFO_POP_N(ptype,id)(f);						\
}


#endif /*  __BEAM_BUFFER_TEMPLATE__H */
