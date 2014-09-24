#ifndef BEAM_PARSE_SHIELD
#include <stdlib.h>
#define START_ACT_LOOP {int act_idx; for (act_idx=0;act_idx<c;act_idx++){
#define END_ACT_LOOP }}
#endif

void METH(filterctrl, act) (int c){
  int z1,z2,z3;

  START_ACT_LOOP
    z1 = CALL(in1,get)();
    z2 = CALL(in2,get)();
    z3 = CALL(in3,get)();

    printf("%d %d %d\n", z1, z2, z3);
  END_ACT_LOOP
}


int METH(filterctrl, activated)(){
  return 1;
}
