create table narmesteleder
(
  aktorid         varchar(16)  not null,
  orgnummer       varchar(16)   not null,
  nlaktorid       varchar(16)  not null,
  aktivfom        date         not null,
  nltelefonnummer varchar(32)  not null,
  nlepost         varchar(128) not null,
  agforskutterer  number(1),
  primary key (aktorid, orgnummer, nlaktorid)
);

