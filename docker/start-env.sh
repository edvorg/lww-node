#! /bin/bash

set -e

ARGS=$@

if [ -z "${BASH_SOURCE[0]}" ] ; then
    LOCAL_DIR=`dirname $0`
else
    LOCAL_DIR=`dirname ${BASH_SOURCE[0]}`
fi

LOCAL_DIR=`realpath ${LOCAL_DIR}`

pushd ${LOCAL_DIR}

if [ -f "./parse-args.sh" ] ; then
    source "./parse-args.sh"
fi

if [ -f "./setup.sh" ] ; then
    source "./setup.sh"
fi

./build ${ARGS}
./run ${ARGS}

popd
