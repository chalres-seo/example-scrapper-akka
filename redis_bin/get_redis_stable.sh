#!/bin/bash

if [ -x "./redis-server" ]
then
    echo "redis-server bin exist."
    exit 0
fi

echo "get and extract redis stable tar ball."
wget http://download.redis.io/redis-stable.tar.gz
tar xvf redis-stable.tar.gz

echo "build and get redis-server (only redis-server)"
cd redis-stable
make
cd ..

cp redis-stable/src/redis-server redis-server

chmod 700 redis-server

echo "clean up."
rm redis-stable.tar.gz
rm -rf redis-stable
