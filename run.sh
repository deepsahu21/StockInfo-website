#!/bin/bash -ex

mvn -q clean
mvn -q compile
mvn -q exec:exec -Dexec.mainClass=stockinfo.app/cs1302.api.${1:-ApiDriver}
