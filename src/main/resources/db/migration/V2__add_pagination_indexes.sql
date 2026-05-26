DROP INDEX idx_products_status;
DROP INDEX idx_products_seller_id;
DROP INDEX idx_orders_customer_id;
DROP INDEX idx_order_items_seller_id;

CREATE INDEX idx_products_status_created_at_id
    ON products (status, created_at DESC, id DESC);

CREATE INDEX idx_products_seller_id_created_at_id
    ON products (seller_id, created_at DESC, id DESC);

CREATE INDEX idx_orders_customer_id_created_at_id
    ON orders (customer_id, created_at DESC, id DESC);

CREATE INDEX idx_orders_created_at_id
    ON orders (created_at DESC, id DESC);

CREATE INDEX idx_order_items_seller_id_order_id
    ON order_items (seller_id, order_id);
