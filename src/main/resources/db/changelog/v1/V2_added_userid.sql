alter table orders add column user_id bigint not null default 0;

alter table cart_positions add column user_id bigint not null default 0;
