FROM eclipse-temurin:21

RUN apt-get update -y && apt-get upgrade -y
RUN apt-get install -y curl maven

RUN curl -L -o /usr/local/bin/mcrcon https://github.com/Tiiffi/mcrcon/releases/download/v0.7.1/mcrcon-0.7.1-linux-x86-64
RUN chmod +x /usr/local/bin/mcrcon

WORKDIR /server

COPY server .


COPY ../../ /code/

CMD ["java", "-jar", "paper.jar"]