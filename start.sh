#!/usr/bin/env bash
PID=0
if [ -f './pid' ]; then
    PID=$(cat ./pid)
fi

if ps -p $PID > /dev/null
then
   echo "he1pME with $PID is already running"
else
    nohup java -jar ./he1pME-1.1.jar > nohups.out 2> errors.log < /dev/null & PID=$!; echo $PID > ./pid
fi