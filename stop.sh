#!/usr/bin/env bash
if [ -f './pid' ]; then
    PID=$(cat ./pid) && kill -15 $PID
fi