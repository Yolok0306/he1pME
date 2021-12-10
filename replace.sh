#!/usr/bin/env bash

rm -f /home/ubuntu/he1pME/he1pME-1.1.jar || true

cp /var/lib/jenkins/workspace/he1pME/target/he1pME-1.1.jar /home/ubuntu/he1pME/

rm -r /home/ubuntu/he1pME/lib || true

cp -r /var/lib/jenkins/workspace/he1pME/target/lib /home/ubuntu/he1pME/