#! /bin/bash

if [ ! -f ./lein ] ; then
    wget -O ./lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
    chmod +x ./lein
fi

export PATH=`realpath ${LOCAL_DIR}`:${PATH}
