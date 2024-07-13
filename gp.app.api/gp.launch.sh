#!/bin/sh
echo ++ try to launch war ++
GP_HOME=`pwd`
CP=$GP_HOME/target/gp.web-0.1.war 
LP=$GP_HOME/target/lib
echo $CP
echo $LP
java -cp $CP -Dloader.path=$CP!BOOT-INF/classes/WEB-INF/classes,$CP!BOOT-INF/classes/WEB-INF,$LP org.springframework.boot.loader.PropertiesLauncher
