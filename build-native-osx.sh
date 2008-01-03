#!/bin/sh

# $Id$

BUILD_ROOT=`pwd`

cd src/main/c/intelbth

xcodebuild

cd ${BUILD_ROOT}

cp src/main/resources/libbluecove.jnilib target/classes/
