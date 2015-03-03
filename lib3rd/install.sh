#!/bin/sh
mvn install:install-file -Dfile=de.unihd.dbs.heideltime.standalone.jar -DgroupId=heideltime -DartifactId=heideltime -Dversion=1.8 -Dpackaging=jar
