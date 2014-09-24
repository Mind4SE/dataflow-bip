#ifndef BEAM_PARSE_SHIELD
#include <stdlib.h>
#define START_ACT_LOOP {int act_idx; for (act_idx=0;act_idx<c;act_idx++){
#define END_ACT_LOOP }}
#endif

void METH(filterctrl, act) (int c){
  START_ACT_LOOP
    int z;
    z = CALL(in, get)();
    
    CALL(out1, put)(z);
    CALL(out2, put)(z);
    CALL(out3, put)(z);
  END_ACT_LOOP
}


int METH(filterctrl, activated)(){
  return 1;
}
