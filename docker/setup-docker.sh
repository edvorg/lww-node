#! /bin/bash

echo "creating lww-set network"
docker network create lww-set || echo "lww-set network is already created"
