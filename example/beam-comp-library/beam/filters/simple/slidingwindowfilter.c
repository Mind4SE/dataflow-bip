#ifndef BEAM_PARSE_SHIELD
#define START_ACT_LOOP {int act_idx; for (act_idx=0;act_idx<c;act_idx++){
#define END_ACT_LOOP }}
#endif
void METH(filterctrl, act) (int c){
  int z;
  int m;
  int w;
  int i,j;

  m=0;
  w=1;

  if (!PRIVATE.init_hack){
    int i;
    PRIVATE.init_hack = 1;
    for (i=0; i<256; i++)
      PRIVATE.history[i]=0;
  }

  START_ACT_LOOP
    z = CALL(in, get)();
#ifdef BEAM_DEBUG
    printf("SW: input = %d\n", z);
#endif
    PRIVATE.history[PRIVATE.ptr] = z;

    for (j=0; j<256; j++){
      m = (PRIVATE.history[j] + m*j)/(j+1);
    }
#ifdef BEAM_DEBUG
    printf("SW: output = %d\n", m);
#endif
    CALL(out, put)(m);
    PRIVATE.ptr = (PRIVATE.ptr+1)%256;
  END_ACT_LOOP
}



int METH(filterctrl, activated)(){
  return 1;
}
