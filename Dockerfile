FROM navikt/java:12-appdynamics
COPY build/libs/syfonarmesteleder-*-all.jar app.jar
ENV APPD_ENABLED=true
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml"
ENV APPLICATION_PROFILE="remote"
