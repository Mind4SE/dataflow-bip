#ifndef BEAM_PARSE_SHIELD
#define START_ACT_LOOP {int act_idx; for (act_idx=0;act_idx<c;act_idx++){
#define END_ACT_LOOP }}
#endif

void METH(filterctrl, act) (int c){
  int i;
  int z;
START_ACT_LOOP
    z = CALL(in,get)();
    printf("%d\n", z);
END_ACT_LOOP
}



int METH(filterctrl, activated)(){
  return 1;
}
