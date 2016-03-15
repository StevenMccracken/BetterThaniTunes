alter table library drop constraint library_pk;
drop table library;
create table Library (
    title varchar(200),
    artist varchar(200),
    album varchar(200),
    yearCreated varchar(50),
    genre varchar(100),
    comment varchar(200),
    path varchar(200) not null);

alter table library add constraint library_pk primary key (path);