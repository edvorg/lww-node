#! /bin/bash

DATA=`realpath ./data`
mkdir -p "${DATA}"
docker run --name lww-node-redis -v "${DATA}:/data" -p 6379:6379 -d redis:3.2 redis-server --appendonly yes
