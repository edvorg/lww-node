#! /bin/bash

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

echo stopping lww-set environment

DIRS=`ls | sort -r`

for entry in ${DIRS} ; do
    if [ -d ${entry} ] ; then
        echo "------------------------------------------------------------------------------------------------"
        echo entering ${entry}
        pushd ${entry}
        if [ ! -f ./stop ] ; then
            echo nothing to stop
        else
            ./stop ${ARGS}
        fi
        popd
    fi
done

popd
