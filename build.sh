#!/usr/bin/env bash

docker-compose build
docker-compose run gradle test
docker-compose run gradle :projectBlueWater:assembleRelease
