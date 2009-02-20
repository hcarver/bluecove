#!/bin/sh

# $Id$

BUILD_ROOT=`pwd`

cd ${BUILD_ROOT}/src/main/c/intelbth

xcodebuild
BUILD_ERROR_CODE=$?

cd ${BUILD_ROOT}

if [[ ${BUILD_ERROR_CODE} != 0  ]] ; then
  exit ${BUILD_ERROR_CODE}
fi

echo Copy Library to ${BUILD_ROOT}/target/classes/
cp ${BUILD_ROOT}/src/main/resources/libbluecove.jnilib ${BUILD_ROOT}/target/classes/
