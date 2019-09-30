drop table if exists accounts;
drop table if exists transfers;

create table accounts
(
    id int auto_increment,
    public_identifier uuid not null,
    -- assuming we operate just one currency
    balance decimal(20,2) default 0 not null,
    in_transfers_count int default 0 not null,
    constraint table_name_pk
        primary key (id)
);
create unique index account_public_identifier_uindex
    on accounts (public_identifier);

create table transfers
(
    id uuid,
    source_account int not null,
    destination_account int not null,
    amount decimal(20,2) not null,
    status smallint default 0 not null,
    created_on timestamp default now() not null,
    updated_on timestamp default now() not null,
    constraint transaction_pk
        primary key (id),
    constraint transfer_account_source_account_fk
        foreign key (source_account) references accounts(id),
    constraint transfer_account_destination_account_fk
        foreign key (destination_account) references accounts(id)
);
