create table created
(
    id        int,
    source_id int,
    target_id int,
    `date`    string,
    weight    double,
    constraint pk_125388334_1631529779071_0
        primary key (id) disable novalidate
)

create table knows
(
    id        int,
    source_id int,
    target_id int,
    `date`    string,
    weight    double,
    constraint pk_264243214_1631529791712_0
        primary key (id) disable novalidate
)

create table person
(
    id   int,
    name varchar(20),
    age  int,
    city varchar(10)
)


create table software
(
    id    int,
    name  string,
    lang  string,
    price double,
    constraint pk_193650557_1631529807431_0
        primary key (id) disable novalidate
)

