-- Seeded automatically by Spring Boot via spring.sql.init.mode=always (default for H2)
-- JPA creates the schema first (ddl-auto=create-drop), then this file runs.

INSERT INTO categories (id, name, description) VALUES
  (1, 'Electronics',   'Phones, laptops, and accessories'),
  (2, 'Books',         'Fiction, non-fiction, and technical titles'),
  (3, 'Home & Garden', 'Furniture, tools, and outdoor');

INSERT INTO products (id, name, description, price, stock, status, category_id) VALUES
  (1,  'Laptop Pro 15',    '15-inch developer laptop',       129999, 50, 'ACTIVE', 1),
  (2,  'Mechanical Kbd',   'Cherry MX Brown switches',        7999,  200,'ACTIVE', 1),
  (3,  'USB-C Hub',        '7-in-1 USB-C hub',                2499,  500,'ACTIVE', 1),
  (4,  'Clean Code',       'Robert C. Martin',                1299,  80, 'ACTIVE', 2),
  (5,  'Designing DApps',  'System design fundamentals',      1499,  60, 'ACTIVE', 2),
  (6,  'Draft Book',       'Not published yet',               999,   0,  'DRAFT',  2),
  (7,  'Ergonomic Chair',  'Lumbar support, mesh back',      24999,  30, 'ACTIVE', 3),
  (8,  'Standing Desk',    'Height adjustable, 140cm',       34999,  20, 'ACTIVE', 3);
