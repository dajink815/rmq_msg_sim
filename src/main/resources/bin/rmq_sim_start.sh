#!/bin/sh
HOME=/home/ca2ssim
CONFIG_FILE=$HOME/ca2ssim/config/rmq_sim.config

PATH_TO_JAR=$HOME/ca2ssim/lib/rmqmsgsim-jar-with-dependencies.jar
JAVA_OPT="-Dlogback.configurationFile=$HOME/ca2ssim/config/logback_rmqsim.xml"


#java -jar $PATH_TO_JAR $CONFIG_FILE
source /etc/profile
java $JAVA_OPT $DEBUG -classpath $PATH_TO_JAR media.platform.rmqmsgsim.RmqMsgSimMain $CONFIG_FILE
exit