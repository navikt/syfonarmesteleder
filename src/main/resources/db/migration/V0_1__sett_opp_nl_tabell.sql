CREATE TABLE NARMESTE_LEDER
(
    narmeste_leder_id            VARCHAR(36) PRIMARY KEY,
    orgnummer                    VARCHAR(64) NOT NULL,
    bruker_fnr                   VARCHAR(64) NOT NULL,
    narmeste_leder_fnr           VARCHAR(64) NOT NULL,
    narmeste_leder_telefonnummer VARCHAR(64) NOT NULL,
    narmeste_leder_epost         VARCHAR(64) NOT NULL,
    aktiv_fom                    TIMESTAMP   NOT NULL,
    aktiv_tom                    TIMESTAMP
);

CREATE TABLE FORSKUTTERING
(
    bruker_fnr                VARCHAR(64) NOT NULL ,
    orgnummer                 VARCHAR(64) NOT NULL ,
    arbeidsgiver_forskutterer BOOLEAN   NOT NULL,
    sist_oppdatert            TIMESTAMP NOT NULL,
    PRIMARY KEY (bruker_fnr, orgnummer)
)
