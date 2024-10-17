FROM amazonlinux:2023

ENV JAVA_VERSION=23
ENV BB_VERSION=1.12.194
ENV DTLV_VERSION=0.9.12
ENV TARGET_ARCH=aarch64

# Install necessary packages

RUN dnf install -y \
    autoconf \
    automake \
    bash \
    gcc \
    git \
    glibc-devel \
    gzip \
    libstdc++-static \
    readline-devel \
    tar \
    unzip \
    zlib-devel

# Install GraalVM

RUN curl -Lo /graalvm.tar.gz https://download.oracle.com/graalvm/${JAVA_VERSION}/latest/graalvm-jdk-${JAVA_VERSION}_linux-${TARGET_ARCH}_bin.tar.gz
RUN mkdir /graalvm
RUN tar -xzvf /graalvm.tar.gz -C /graalvm --strip-components=1
ENV JAVA_HOME=/graalvm
ENV PATH="/graalvm/bin:$PATH"

# Install rlwrap

RUN curl -Lo /rlwrap.tar.gz https://github.com/hanslub42/rlwrap/releases/download/0.46.1/rlwrap-0.46.1.tar.gz
RUN mkdir /rlwrap
RUN tar -xzvf /rlwrap.tar.gz -C /rlwrap --strip-components=1
WORKDIR /rlwrap
RUN ls -lah
RUN autoreconf --install
RUN ./configure
RUN make
RUN make install
WORKDIR /

# Install Clojure

RUN curl -Lo /clojure-install.sh https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh
RUN chmod +x /clojure-install.sh
RUN /clojure-install.sh

# Unpack Babashka

RUN curl -Lo /babashka.tar.gz https://github.com/babashka/babashka/releases/download/v${BB_VERSION}/babashka-${BB_VERSION}-linux-${TARGET_ARCH}-static.tar.gz
RUN tar -xzvf /babashka.tar.gz
RUN ls -lah
RUN chmod +x /bb

# Install Leiningen

# RUN curl -Lo /lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
# RUN mv /lein /usr/bin/
# RUN chmod +x /usr/bin/lein
# RUN lein

# Decompress Datalevin

RUN curl -Lo /dtlv.zip https://github.com/juji-io/datalevin/releases/download/${DTLV_VERSION}/dtlv-${DTLV_VERSION}-ubuntu-latest-${TARGET_ARCH}.zip
RUN unzip -d / /dtlv.zip

# Copy project

COPY . /app

# Build uberjar

WORKDIR /app
RUN clojure -P
RUN clojure -X:jar :jar lambda.jar :main-class bbotiscaf.core

# Build native image

# RUN native-image \
#     --initialize-at-build-time \
#     --no-fallback \
#     -jar target/bbotiscaf-0.1.0.jar \
#     -H:Name=lambda \
#     -H:+ReportExceptionStackTraces \
#     -H:Class=bbotiscaf.core \
#     -H:ConfigurationFileDirectories=META-INF/native-image
