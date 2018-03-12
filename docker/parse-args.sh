#! /bin/bash

if [ -z "${BASH_SOURCE[0]}" ] ; then
    LOCAL_DIR=`dirname ${0}`
else
    LOCAL_DIR=`dirname ${BASH_SOURCE[0]}`
fi

LOCAL_DIR=`realpath ${LOCAL_DIR}`

if [ ! -f "${LOCAL_DIR}/local" ] ; then
    cp "${LOCAL_DIR}/local.example" "${LOCAL_DIR}/local"
fi

if [ -f "${LOCAL_DIR}/local" ] ; then
    source "${LOCAL_DIR}/local"
fi

# parse arguments

while [[ $# -ge 1 ]]
do
    key="$1"

    case $key in
        -n|--no-cache)
            NOCACHE="--no-cache"
            shift # past argument
            ;;

        -v|--verbose)
            VERBOSE="yes"
            shift # past argument
            ;;

        -ub|--use-builder)
            USE_BUILDER=${2}
            shift # past argument
            ;;

        -s|--start)
            START_NODES="yes"
            START_NORMAL_MONKEY="yes"
            START_OFFLINE_ONLINE_MONKEY="yes"
            START_CLIENT_VIEWER_MONKEY="yes"
            shift # past argument
            ;;

        -sn|--start-nodes)
            START_NODES=${2}
            shift # past argument
            ;;

        -snm|--start-normal-monkey)
            START_NORMAL_MONKEY=${2}
            shift # past argument
            ;;

        -soom|--start-offline-online-monkey)
            START_OFFLINE_ONLINE_MONKEY=${2}
            shift # past argument
            ;;

        -scvm|--start-client-viewer-monkey)
            START_CLIENT_VIEWER_MONKEY=${2}
            shift # past argument
            ;;

        -nc|--nodes-count)
            NODES_COUNT=${2}
            shift # past argument
            ;;

        -en|--external-nodes)
            EXTERNAL_NODES=${2}
            shift # past argument
            ;;

        -nmc|--normal-monkey-count)
            NORMAL_MONKEY_COUNT=${2}
            shift # past argument
            ;;

        -nmipc|--normal-monkey-in-process-count)
            NORMAL_MONKEY_IN_PROCESS_COUNT=${2}
            shift # past argument
            ;;

        -oomc|--offline-online-monkey-count)
            OFFLINE_ONLINE_MONKEY_COUNT=${2}
            shift # past argument
            ;;

        -oomipc|--offline-online-monkey-in-process-count)
            OFFLINE_ONLINE_MONKEY_IN_PROCESS_COUNT=${2}
            shift # past argument
            ;;

        -cvmc|--client-viewer-monkey-count)
            CLIENT_VIEWER_MONKEY_COUNT=${2}
            shift # past argument
            ;;

        -cvmipc|--client-viewer-monkey-in-process-count)
            CLIENT_VIEWER_MONKEY_IN_PROCESS_COUNT=${2}
            shift # past argument
            ;;

        -p|--prod)
            START_NODES="yes"
            NODES_COUNT=1
            START_NORMAL_MONKEY="no"
            START_OFFLINE_ONLINE_MONKEY="no"
            START_CLIENT_VIEWER_MONKEY="no"
            shift # past argument
            ;;

        -i|--integration)
            DROP_DATA="yes"
            DATA_DIR="data-integration"
            START_NODES="yes"
            NODES_COUNT=3
            START_NORMAL_MONKEY="no"
            START_OFFLINE_ONLINE_MONKEY="no"
            START_CLIENT_VIEWER_MONKEY="yes"
            CLIENT_VIEWER_MONKEY_COUNT=1
            CLIENT_VIEWER_MONKEY_IN_PROCESS_COUNT=1
            shift # past argument
            ;;

        *)
            # unknown option
            shift # past argument or value
            ;;
    esac
done

if [ -z "${DATA_DIR}" ] ; then
    DATA_DIR="data"
fi

if ! [[ "${NODES_COUNT}" =~ ^[0-9]+$ ]] ; then
    echo NODES_COUNT must be an integer but has value: "'${NODES_COUNT}'" setting to 1
    NODES_COUNT=1
fi

if ! [[ "${NORMAL_MONKEY_COUNT}" =~ ^[0-9]+$ ]] ; then
    echo NORMAL_MONKEY_COUNT must be an integer but has value: "'${NORMAL_MONKEY_COUNT}'" setting to 1
    NORMAL_MONKEY_COUNT=1
fi

if ! [[ "${NORMAL_MONKEY_IN_PROCESS_COUNT}" =~ ^[0-9]+$ ]] ; then
    echo NORMAL_MONKEY_IN_PROCESS_COUNT must be an integer but has value: "'${NORMAL_MONKEY_IN_PROCESS_COUNT}'" setting to 1
    NORMAL_MONKEY_IN_PROCESS_COUNT=1
fi

if ! [[ "${OFFLINE_ONLINE_MONKEY_COUNT}" =~ ^[0-9]+$ ]] ; then
    echo OFFLINE_ONLINE_MONKEY_COUNT must be an integer but has value: "'${OFFLINE_ONLINE_MONKEY_COUNT}'" setting to 1
    OFFLINE_ONLINE_MONKEY_COUNT=1
fi

if ! [[ "${OFFLINE_ONLINE_MONKEY_IN_PROCESS_COUNT}" =~ ^[0-9]+$ ]] ; then
    echo OFFLINE_ONLINE_MONKEY_IN_PROCESS_COUNT must be an integer but has value: "'${OFFLINE_ONLINE_MONKEY_IN_PROCESS_COUNT}'" setting to 1
    OFFLINE_ONLINE_MONKEY_IN_PROCESS_COUNT=1
fi

if ! [[ "${CLIENT_VIEWER_MONKEY_COUNT}" =~ ^[0-9]+$ ]] ; then
    echo CLIENT_VIEWER_MONKEY_COUNT must be an integer but has value: "'${CLIENT_VIEWER_MONKEY_COUNT}'" setting to 1
    CLIENT_VIEWER_MONKEY_COUNT=1
fi

if ! [[ "${CLIENT_VIEWER_MONKEY_IN_PROCESS_COUNT}" =~ ^[0-9]+$ ]] ; then
    echo CLIENT_VIEWER_MONKEY_IN_PROCESS_COUNT must be an integer but has value: "'${CLIENT_VIEWER_MONKEY_IN_PROCESS_COUNT}'" setting to 1
    CLIENT_VIEWER_MONKEY_IN_PROCESS_COUNT=1
fi

if [[ "${VERBOSE}" == "yes" ]] ; then
    echo
    echo parsed args:
    echo
    echo USE_BUILDER = ${USE_BUILDER}
    echo NOCACHE = ${NOCACHE}
    echo
    echo START_NODES = ${START_NODES}
    echo START_NORMAL_MONKEY = ${START_NORMAL_MONKEY}
    echo START_OFFLINE_ONLINE_MONKEY = ${START_OFFLINE_ONLINE_MONKEY}
    echo START_CLIENT_VIEWER_MONKEY = ${START_CLIENT_VIEWER_MONKEY}
    echo
    echo NODES_COUNT = ${NODES_COUNT}
    echo EXTERNAL_NODES = ${EXTERNAL_NODES}
    echo NORMAL_MONKEY_COUNT = ${NORMAL_MONKEY_COUNT}
    echo NORMAL_MONKEY_IN_PROCESS_COUNT = ${NORMAL_MONKEY_IN_PROCESS_COUNT}
    echo OFFLINE_ONLINE_MONKEY_COUNT = ${OFFLINE_ONLINE_MONKEY_COUNT}
    echo OFFLINE_ONLINE_MONKEY_IN_PROCESS_COUNT = ${OFFLINE_ONLINE_MONKEY_IN_PROCESS_COUNT}
    echo CLIENT_VIEWER_MONKEY_COUNT = ${CLIENT_VIEWER_MONKEY_COUNT}
    echo CLIENT_VIEWER_MONKEY_IN_PROCESS_COUNT = ${CLIENT_VIEWER_MONKEY_IN_PROCESS_COUNT}
    echo
fi
