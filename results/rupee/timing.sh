#! /bin/bash

# this file needs to be edited for each type of benchmark

bm=$1
st=$2
id=$3

java -jar ../../rupee-mgr/target/rupee-mgr-0.0.1-SNAPSHOT-jar-with-dependencies.jar -s DB_ID,SCOP,${id},100,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,TOP_ALIGNED,TM_SCORE >> ./${bm}_timing_${st}.txt
