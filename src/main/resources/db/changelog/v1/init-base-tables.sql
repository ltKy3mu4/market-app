create table if not exists items
(
    id         bigint primary key generated always as identity,
    title      varchar(256) not null,
    description       text,
    img_path text not null,
    price double precision not null check (price > 0)
);

create table if not exists orders
(
    id      bigint primary key generated always as identity,
    total_sum double precision not null
);

create table if not exists order_positions
(
    id      bigint primary key generated always as identity,
    order_id bigint not null,
    title text not null,
    description text,
    img_path text not null,
    price double precision CHECK (price > 0),
    count int,

    constraint order_id_fk foreign key (order_id) references orders (id)
);

create table if not exists cart_positions
(
    id   bigint primary key generated always as identity,
    item_id bigint not null,
    count int CHECK(count > 0),

    constraint item_id_fk foreign key (item_id) references items (id)
);