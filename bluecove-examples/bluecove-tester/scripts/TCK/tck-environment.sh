#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
SCRIPTS_DIR=`dirname ${0}`/..
. ${SCRIPTS_DIR}/environment.sh

BLUECOVE_TCK_HOST=tckhost
BLUECOVE_TCK_PORT=8080

if [[ ! -d ${BLUECOVE_3RDPARTY_HOME} ]] ; then
  echo "Invalid 3-rd party directory ${BLUECOVE_3RDPARTY_HOME}"
  exit 1
fi

MICROEMULATOR_HOME=${BLUECOVE_3RDPARTY_HOME}/microemulator


