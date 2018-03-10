FROM ubuntu:16.04

RUN apt-get -y clean all && \
    sed -i 's%us.archive.ubuntu.com%mirrors.gigenet.com/ubuntuarchive/%' /etc/apt/sources.list && \
    apt-get -y update && \
    apt-get -y upgrade && \
    apt-get -y install software-properties-common python-software-properties git default-jdk wget telnet zip netcat-traditional fakeroot gosu iputils-ping curl language-pack-en && \
    locale-gen en_US.UTF-8

RUN wget -O /usr/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein

RUN apt-get -y install docker.io

RUN chmod +x /usr/bin/lein
