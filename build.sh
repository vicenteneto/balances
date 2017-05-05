#!/bin/bash

CURRENT_DIR=`pwd`

wget https://my.datomic.com/downloads/free/0.9.5561 -O datomic-free-0.9.5561.zip
unzip datomic-free-0.9.5561.zip

echo "{:user {:datomic {:install-location \"${CURRENT_DIR}/datomic-free-0.9.5561\"}}}" > ~/.lein/profiles.clj

lein datomic start & sleep 5
lein datomic initialize
lein midje
