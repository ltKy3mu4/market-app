-- Clear existing data
DELETE FROM order_positions;
DELETE FROM orders;
DELETE FROM items;

ALTER SEQUENCE orders_id_seq RESTART WITH 1;
ALTER SEQUENCE order_positions_id_seq RESTART WITH 1;
ALTER SEQUENCE items_id_seq RESTART WITH 1;

-- Insert test items
INSERT INTO items (title, description, img_path, price)
VALUES ('Test Item 1', 'Test Description 1', '/images/test1.jpg', 50.0),
       ('Another Test Item', 'Another Description', '/images/test2.jpg', 25.0),
       ('Third Item', 'Third Description', '/images/test3.jpg', 75.0);
