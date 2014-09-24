#include <beam.h> 

int METH(main, main) (int argc, char *argv[]){
  CALL(buffer__generator_output__maxfilter_input, init)();
  CALL(buffer__maxfilter_output__dumper_input, init)();

  for(;;){
    CALL(generator, act)(1);
    CALL(maxfilter, act)(1);
    CALL(dumper, act)(1);
  }
  return 0;
}
