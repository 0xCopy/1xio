#! /bin/sh

# $Id: build.sh,v 1.2 2003/04/15 06:01:10 sfcat Exp $

if [ -z "$JAVA_HOME" ] ; then
  JAVA=`which java`
  if [ -z "$JAVA" ] ; then
    echo "Cannot find JAVA. Please set your PATH."
    exit 1
  fi
  JAVA_BIN=`dirname $JAVA`
  JAVA_HOME=$JAVA_BIN/..
fi

JAVA=$JAVA_HOME/bin/java

$JAVA -classpath $CLASSPATH:$JAVA_HOME/lib/tools.jar -Dant.home=tools org.apache.tools.ant.Main $1 $2 $3 $4 $5 $6


