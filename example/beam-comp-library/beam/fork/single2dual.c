void METH(filterctrl, act) (int c){
  int i;
  dualint_t z;

  for(i=0; i<c; i++){
    z.x = CALL(in1,get)();
    z.y = CALL(in2,get)();
#ifdef BEAM_DEBUG
    printf("joiner: input1 = %d\n", z.x);
    printf("joiner: input2 = %d\n", z.y);
    printf("joiner: output = {x:%d; y:%d}\n", z.x, z.y);
#endif
    CALL(out, put)(z);
  }
}

/** SSZ - Added - Was missing */
int METH(filterctrl, activated)(void) {
	printf("Activated - SSZ patch - do nothing - FIXME");
}
