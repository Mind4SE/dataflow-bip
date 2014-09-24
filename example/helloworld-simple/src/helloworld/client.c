#include <stdio.h>

void METH(filterctrl, act) (int c){
  int x = 19;
  printf ("Client");
  CALL(s,put)(x);
  printf("[%d]\n", x);
}

/** SSZ - Added - Was missing */
int METH(filterctrl, activated)(void) {
	printf("Activated - SSZ patch - do nothing - FIXME");
}
