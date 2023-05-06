# Java debuggee


## launch target JVM
```shell
java -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=y -cp debugger/target/classes ga.rugal.DebuggeeMain
```
