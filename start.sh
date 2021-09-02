#!/usr/bin/env bash
PID=0
if [ -f './pid' ]; then
    PID=$(cat ./pid)
fi

if ps -p $PID > /dev/null
then
   echo "he1pME-1.1.jar with $PID is already running"
else
    nohup java -jar target/he1pME-1.1.jar > he1pME.log 2> error.log < /dev/null & PID=$!; echo $PID > ./pid
fi