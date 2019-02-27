create table narmesteleder
(
  aktorid         varchar(11)  not null,
  orgnummer       varchar(9)   not null,
  nlaktorid       varchar(11)  not null,
  aktivfom        date         not null,
  nltelefonnummer varchar(20)  not null default '',
  nlepost         varchar(128) not null default '',
  agforskutterer  bool         not null default false,
  primary key (aktorid, orgnummer)
);

