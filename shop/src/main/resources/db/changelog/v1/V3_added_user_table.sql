create table if not exists users
(
    id         bigint primary key generated always as identity,
    username      varchar(50) unique not null,
    password       varchar(256) not null,
    role          varchar(20) not null
    );

