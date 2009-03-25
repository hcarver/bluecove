#!/bin/sh
# @version $Revision$ ($Author$) $Date$
#
GENERATED_VERSION="${SCRIPTS_DIR}/generated-version.sh"
if [ ! -f "${GENERATED_VERSION}" ]; then
    echo "${GENERATED_VERSION} Not Found, run maven first"
    exit 1;
fi
if [ ! -x "${GENERATED_VERSION}" ]; then
    chmod +x "${GENERATED_VERSION}"
fi
. "${GENERATED_VERSION}"
# echo BLUECOVE_VERSION=${BLUECOVE_VERSION}
