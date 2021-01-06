FROM navikt/java:12
COPY build/libs/syfonarmesteleder-*-all.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml \
               -Xmx896M \
               -Xms768M"
ENV APPLICATION_PROFILE="remote"
