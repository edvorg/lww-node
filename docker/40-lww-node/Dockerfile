FROM lww-set-builder

RUN mkdir /lww-set

RUN useradd -m -g users lww-set

RUN chown -R lww-set /lww-set

USER lww-set

ENV JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true

ADD .release /lww-set

ADD .lein-env /lww-set/.lein-env
