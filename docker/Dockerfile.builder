FROM maven:3.6.1 as builder

WORKDIR /builder

#cahed step in case no dependencies changed
COPY pom.xml /builder/pom.xml

RUN ["mvn", "dependency:resolve"]
RUN ["mvn", "verify"]

COPY src /builder/src

RUN ["mvn", "test", "package"]
