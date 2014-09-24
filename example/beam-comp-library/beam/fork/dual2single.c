void METH(filterctrl, act) (int c){
  int i;
  dualint_t z;

  for(i=0; i<c; i++){
    z = CALL(in,get)();
#ifdef BEAM_DEBUG
    printf("forker: input = {x:%d; y:%d}\n", z.x, z.y);
    printf("forker: output1 = %d\n", z.x);
    printf("forker: output2 = %d\n", z.y);
#endif
    CALL(out1, put)(z.x);
    CALL(out2, put)(z.y);
  }
}

/** SSZ - Added - Was missing */
int METH(filterctrl, activated)(void) {
	printf("Activated - SSZ patch - do nothing - FIXME");
}
