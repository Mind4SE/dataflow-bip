#include <stdio.h>

void METH(filterctrl, act) (int c){
  int x=-1;
  printf("Server");
  x = CALL(s, get)();
  printf("[%d]\n", x);
}

/** SSZ - Added - Was missing */
int METH(filterctrl, activated)(void) {
	printf("Activated - SSZ patch - do nothing - FIXME");
}
