#ifndef BEAM_PARSE_SHIELD
#include <stdlib.h>
#define START_ACT_LOOP {int act_idx; for (act_idx=0;act_idx<c;act_idx++){
#define END_ACT_LOOP }}
#endif


void METH(filterctrl, act) (int c){
  int z;
  int m;
  int w;
  int j;

  m = 0;
  w = 1;

  START_ACT_LOOP
    for (j=0; j<ATTR(width); j++){
      int tmp;
      tmp = CALL(in, peek)(j);
      m = (tmp + m*j)/(j+1);
    }
    CALL(in, get)();
    
    CALL(out, put)(m);
  END_ACT_LOOP
}



int METH(filterctrl, activated)(){
  return 1;
}
