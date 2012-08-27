#!/bin/bash

# Function to create lib paths
make_paths() {

    local RESULT=
    for lib in `ls $1`
    do
        if [ -f $1/$lib ]; then
            RESULT=$RESULT:$1/$lib
        fi
    done

    echo $RESULT
}

# Get the path to this script
BASEDIR=`dirname $0`

# Set the Eoulsan libraries path
LIBDIR=$BASEDIR/lib

# Set the memory in MiB needed by Aozan (only Java part, not external tools)
# By Default 2048
MEMORY=2048

# Additional JVM options
JVM_OPTS="-server"

# Add here your plugins and dependencies
PLUGINS=

# Set the path to java
if [ -z "$JAVA_HOME" ] ; then
    JAVA_CMD="java"
else
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

COMMON_LIBS=$(make_paths $LIBDIR)
PLUGINS_LIBS=$(make_paths $AOZAN_PLUGINS)

# Launch Aozan
$JAVA_CMD \
        $JVM_OPTS \
        -Xmx${MEMORY}m \
        -cp $COMMON_LIBS:$PLUGINS:$PLUGINS_LIBS \
        org.python.util.jython \
        -jar $LIBDIR/jythonlib.jar "$@"

