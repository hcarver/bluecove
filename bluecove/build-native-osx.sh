#!/bin/sh

# $Id$

BUILD_ROOT=`pwd`

cd src/main/c/intelbth

xcodebuild
BUILD_ERROR_CODE=$?

cd ${BUILD_ROOT}

cp src/main/resources/libbluecove.jnilib target/classes/

if [[ ${BUILD_ERROR_CODE} != 0  ]] ; then
  exit ${BUILD_ERROR_CODE}
fi
