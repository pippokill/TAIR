#!/bin/sh
#---------------------------------#
# dynamically build the classpath #
#---------------------------------#
THE_CLASSPATH=./target/TEE2-0.1-SNAPSHOT.jar
for i in `ls ./target/lib/*.jar`
do
  THE_CLASSPATH=${THE_CLASSPATH}:${i}
done

echo Run GUI...

java -Xmx2G -cp ".:${THE_CLASSPATH}" di.uniba.it.tee2.gui.TEEgui $1 $2 $3 $4 $5 $6
