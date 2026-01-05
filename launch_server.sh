#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mvn verify -f $SCRIPT_DIR/pom.xml
rm -rf $SCRIPT_DIR/envs/server/plugins
mkdir $SCRIPT_DIR/envs/server/plugins
cp $SCRIPT_DIR/target/Arkama*.jar $SCRIPT_DIR/envs/server/plugins/
docker compose -f $SCRIPT_DIR/envs/docker-compose.yml up --build