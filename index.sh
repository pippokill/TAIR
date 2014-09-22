#!/bin/sh
#---------------------------------#
# dynamically build the classpath #
#---------------------------------#
THE_CLASSPATH=./target/TEE2-0.1-SNAPSHOT.jar
for i in `ls ./target/lib/*.jar`
do
  THE_CLASSPATH=${THE_CLASSPATH}:${i}
done

echo Indexing...

java -Xmx4g -cp ".:${THE_CLASSPATH}" di.uniba.it.tee2.wiki.Wikidump2IndexMT $1 $2 $3 $4 $5
