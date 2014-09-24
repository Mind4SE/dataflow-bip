int METH(main, main) (int argc, char *argv[]){

  for(;;){
    CALL(client, act)(1);

    CALL(server, act)(1);
  }

  return 0;
}
