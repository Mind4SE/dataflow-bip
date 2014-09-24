int METH(main, main) (int argc, char *argv[]){

  for(;;){
    CALL(generator, act)(1);

    CALL(maxfilter, act)(1);
    CALL(dumper, act)(1);
  }

  return 0;
}
