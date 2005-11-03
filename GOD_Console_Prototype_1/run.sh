#!/bin/bash

export C_CLASSPATH=$CLASSPATH
for i in `ls lib`; do C_CLASSPATH=$C_CLASSPATH:"lib/$i"; done;

java -cp $C_CLASSPATH ddproto1.Main --fakeconsole=false --debug=false $@