DELETE FROM cart_positions;
DELETE FROM items;

ALTER SEQUENCE cart_positions_id_seq RESTART WITH 1;
ALTER SEQUENCE items_id_seq RESTART WITH 1;

INSERT INTO items (title, description, img_path, price)
VALUES ('Test Item 1', 'Test Description 1', '/images/test1.jpg', 50.0),
       ('Another Test Item', 'Another Description', '/images/test2.jpg', 25.0),
       ('Third Item', 'Third Description', '/images/test3.jpg', 75.0);

INSERT INTO cart_positions (item_id, count, user_id)
VALUES ( 1, 2, 0),
       ( 2, 1, 0);