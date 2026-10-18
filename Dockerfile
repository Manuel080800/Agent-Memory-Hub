FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml pom.xml
COPY core/pom.xml core/pom.xml
COPY api/pom.xml api/pom.xml
COPY webapp/pom.xml webapp/pom.xml
COPY core/src core/src
COPY api/src api/src
COPY webapp/src webapp/src

RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM quay.io/wildfly/wildfly:41.0.1.Final-jdk21

USER root

RUN mkdir -p /opt/jboss/wildfly/modules/org/postgresql/main /run/amh/tls \
    && chown -R jboss:root /opt/jboss/wildfly/modules/org/postgresql /run/amh

COPY --from=build --chown=jboss:root /root/.m2/repository/org/postgresql/postgresql/42.7.8/postgresql-42.7.8.jar /opt/jboss/wildfly/modules/org/postgresql/main/postgresql-42.7.8.jar
COPY --chown=jboss:root docker/wildfly/postgresql-module.xml /opt/jboss/wildfly/modules/org/postgresql/main/module.xml
COPY --chown=jboss:root docker/wildfly/configure.cli /opt/jboss/wildfly/configure.cli
COPY --chown=jboss:root docker/wildfly/entrypoint.sh /opt/jboss/wildfly/entrypoint.sh
COPY --from=build --chown=jboss:root /workspace/api/target/api.war /opt/jboss/wildfly/standalone/deployments/api.war
COPY --from=build --chown=jboss:root /workspace/webapp/target/webapp.war /opt/jboss/wildfly/standalone/deployments/webapp.war

RUN chmod 0750 /opt/jboss/wildfly/entrypoint.sh

USER jboss

EXPOSE 8443

ENTRYPOINT ["/opt/jboss/wildfly/entrypoint.sh"]
