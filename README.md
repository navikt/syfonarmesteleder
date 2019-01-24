# Syfonarmesteleder
Denne appen tilbyr et endepunkt for å hente ut hvorvidt arbeidsgiver forskutterer lønn hvis en ansatt er sykmeldt. 

## Bygge og kjøre appen lokalt
1. Kjør `./gradlew clean shadowJar`
2. Bygg dockerimage og start appen med `docker build -t app_name .` og
`docker run -p 8080:8080 syfonarmesteleder`
