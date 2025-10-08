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

-- -- Insert test orders (keep your existing order data)
-- INSERT INTO orders (id, total_sum)
-- VALUES (1, 150.0),
--        (2, 200.0);
--
-- -- Insert test order positions (keep your existing order positions data)
-- INSERT INTO order_positions (id, order_id, title, description, img_path, price, count)
-- VALUES (1, 1, 'Test Item 1', 'Test Description 1', '/images/test1.jpg', 50.0, 2),
--        (2, 1, 'Test Item 2', 'Test Description 2', '/images/test2.jpg', 25.0, 2),
--        (3, 2, 'Second Order Item', 'Second Order Description', '/images/second.jpg', 200.0, 1);