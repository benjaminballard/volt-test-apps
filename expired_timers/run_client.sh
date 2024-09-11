#!/usr/bin/env bash

# set CLASSPATH
VOLT_DIR="$(dirname $(dirname "$(which voltdb)"))"
echo "VOLT_DIR = $VOLT_DIR"
if [ -d $VOLT_DIR ]; then
    CP="$(ls -1 $VOLT_DIR/voltdb/voltdbclient-*.jar)"
    CP="$CP:$(ls -1 $VOLT_DIR/lib/slf4j-api-*.jar)"
    CP="$CP:$(ls -1 $VOLT_DIR/lib/slf4j-reload4j-*.jar)"
    CP="$CP:$(ls -1 $VOLT_DIR/lib/reload4j-*.jar)"
    echo "CP = $CP"
else
    echo "VoltDB client library not found.  If you installed with the tar.gz file, you need to add the bin directory to your PATH"
    exit
fi

SRC=`find client/src -name "*.java"`

if [ ! -z "$SRC" ]; then
    mkdir -p client/obj
    javac -classpath $CP -d client/obj $SRC
    # stop if compilation fails
    if [ $? != 0 ]; then exit; fi

    jar cf client/client.jar -C client/obj .
    rm -rf client/obj

    java -classpath "client/client.jar:$CP" org.voltdb.example.Benchmark $*

fi
