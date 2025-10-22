DELETE
FROM order_positions;
DELETE
FROM orders;


ALTER SEQUENCE orders_id_seq RESTART WITH 1;
ALTER SEQUENCE order_positions_id_seq RESTART WITH 1;

INSERT INTO orders (total_sum, user_id)
VALUES (150.0, 0),
       (200.0, 0);

INSERT INTO order_positions ( order_id, title, description, img_path, price, count)
VALUES ( 1, 'Test Item 1', 'Test Description 1', '/images/test1.jpg', 50.0, 2),
       ( 1, 'Test Item 2', 'Test Description 2', '/images/test2.jpg', 25.0, 2),
       ( 2, 'Second Order Item', 'Second Order Description', '/images/second.jpg', 200.0, 1);
