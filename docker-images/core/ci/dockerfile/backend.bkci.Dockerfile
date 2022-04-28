FROM blueking/jdk:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

ENV LANG="en_US.UTF-8"

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    yum install mysql -y && \
    wget "https://github.com/bkdevops-projects/devops-jre/raw/main/linux/jre.zip?v=1" -P /data/workspace/agent-package/jre/linux/ &&\
    wget "https://github.com/bkdevops-projects/devops-jre/raw/main/windows/jre.zip?v=1" -P /data/workspace/agent-package/jre/windows/ &&\
    wget "https://github.com/bkdevops-projects/devops-jre/raw/main/macos/jre.zip?v=1" -P /data/workspace/agent-package/jre/macos/ 

COPY ./ci /data/workspace/
COPY ./dockerfile/backend.bkci.sh /data/workspace/

WORKDIR /data/workspace
