#!/bin/bash

# prepare env path variables
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.2.jdk/Contents/Home
MVN_HOME=/gdev/dev-tools/apache-maven-3.6.3

# reset path variable
PATH=$PATH:$MVN_HOME/bin:$JAVA_HOME/bin

mvn exec:java