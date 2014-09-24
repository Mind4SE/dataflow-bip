#include <stdlib.h>

void METH(filterctrl, act) (int c){
  int i;
  dualint_t r;

  for (i=0;i<c;i++){
    r.x = rand() % 256;
    r.y = rand() % 256;
#ifdef BEAM_DEBUG
    printf("DR: output = { x:%d; y:%d}\n", r.x, r.y);
#endif
    CALL(out, put)(r);
  }
}


int METH(filterctrl, activated)(){
  return 1;
}
