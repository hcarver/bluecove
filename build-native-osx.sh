#!/bin/sh

# $Id$

BUILD_ROOT=`pwd`

cd src/main/c/intelbth

xcodebuild

cd ${BUILD_ROOT}