-- V4: Insert 5 sample products for all 9 categories (45 total)
INSERT INTO products (name, description, price, stock_qty, category, sku)
VALUES
  -- Electronics
  ('Smartphone X', '6.5 inch display, 128GB storage', 999.99, 100, 'Electronics', 'ELEC-001'),
  ('Smartwatch Series 5', 'Health tracking smartwatch', 299.99, 50, 'Electronics', 'ELEC-002'),
  ('4K OLED TV', '55 inch smart TV with HDR', 1499.99, 20, 'Electronics', 'ELEC-003'),
  ('Noise Cancelling Headphones', 'Over-ear wireless headphones', 349.99, 75, 'Electronics', 'ELEC-004'),
  ('Portable Bluetooth Speaker', 'Waterproof outdoor speaker', 59.99, 120, 'Electronics', 'ELEC-005'),

  -- Clothing
  ('Men''s Classic T-Shirt', '100% cotton casual tee', 19.99, 200, 'Clothing', 'CLO-001'),
  ('Women''s Running Jacket', 'Lightweight breathable windbreaker', 45.99, 150, 'Clothing', 'CLO-002'),
  ('Denim Jeans', 'Slim fit indigo wash', 55.99, 100, 'Clothing', 'CLO-003'),
  ('Cozy Winter Beanie', 'Wool knit hat', 15.99, 300, 'Clothing', 'CLO-004'),
  ('Graphic Hoodie', 'Premium heavy cotton blend', 65.99, 80, 'Clothing', 'CLO-005'),

  -- Books
  ('The Great Gatsby', 'F. Scott Fitzgerald classic', 9.99, 500, 'Books', 'BOK-001'),
  ('Introduction to Algorithms', 'CS textbook 4th edition', 85.00, 40, 'Books', 'BOK-002'),
  ('Clean Code', 'A Handbook of Agile Software Craftsmanship', 45.50, 60, 'Books', 'BOK-003'),
  ('The Pragmatic Programmer', 'Your journey to mastery', 42.00, 75, 'Books', 'BOK-004'),
  ('Design Patterns', 'Elements of Reusable Object-Oriented Software', 55.99, 50, 'Books', 'BOK-005'),

  -- Home
  ('Ceramic Coffee Mug', '12oz minimalist design', 14.99, 150, 'Home', 'HOM-001'),
  ('Throw Blanket', 'Soft fleece blanket 50x60', 29.99, 100, 'Home', 'HOM-002'),
  ('Scented Candle', 'Lavender and vanilla soy wax', 19.99, 200, 'Home', 'HOM-003'),
  ('Desk Lamp', 'LED adjustable reading light', 35.99, 80, 'Home', 'HOM-004'),
  ('Memory Foam Pillow', 'Ergonomic neck support', 45.99, 60, 'Home', 'HOM-005'),

  -- Sports
  ('Yoga Mat', '6mm non-slip exercise mat', 25.99, 120, 'Sports', 'SPR-001'),
  ('Dumbbell Set', 'Adjustable weights up to 50lbs', 199.99, 30, 'Sports', 'SPR-002'),
  ('Resistance Bands', 'Pack of 5 varying resistance', 15.99, 250, 'Sports', 'SPR-003'),
  ('Tennis Racket', 'Lightweight carbon fiber frame', 89.99, 45, 'Sports', 'SPR-004'),
  ('Basketball', 'Official size indoor/outdoor ball', 35.99, 80, 'Sports', 'SPR-005'),

  -- Food
  ('Organic Coffee Beans', 'Dark roast 1lb bag', 18.99, 100, 'Food', 'FOD-001'),
  ('Green Tea Bags', 'Box of 50 organic matcha tea bags', 12.99, 150, 'Food', 'FOD-002'),
  ('Dark Chocolate Bar', '70% cocoa with sea salt', 4.99, 300, 'Food', 'FOD-003'),
  ('Raw Honey', '16oz pure unfiltered local honey', 14.99, 80, 'Food', 'FOD-004'),
  ('Protein Powder', 'Vanilla whey isolate 2lbs', 35.99, 60, 'Food', 'FOD-005'),

  -- Accessories
  ('Leather Wallet', 'Slim RFID blocking bifold', 45.99, 100, 'Accessories', 'ACC-001'),
  ('Sunglasses', 'Polarized aviator style', 29.99, 150, 'Accessories', 'ACC-002'),
  ('Mechanical Keyboard', 'Tenkeyless with cherry MX switches', 120.00, 40, 'Accessories', 'ACC-003'),
  ('Laptop Sleeve', 'Padded 15-inch case', 25.99, 90, 'Accessories', 'ACC-004'),
  ('Ergonomic Mouse Pad', 'Wrist rest gel support', 15.99, 200, 'Accessories', 'ACC-005'),

  -- Other
  ('AA Batteries', 'Pack of 24 alkaline batteries', 12.99, 500, 'Other', 'OTH-001'),
  ('Desk Organizer', 'Mesh wire office supplies holder', 18.99, 150, 'Other', 'OTH-002'),
  ('Utility Knife', 'Retractable box cutter', 9.99, 250, 'Other', 'OTH-003'),
  ('Microfiber Cloths', 'Pack of 12 cleaning towels', 14.99, 300, 'Other', 'OTH-004'),
  ('Extension Cord', '10ft heavy duty power strip', 22.99, 120, 'Other', 'OTH-005'),

  -- Testing
  ('Test Item A', 'Auto-generated dummy product A', 1.00, 999, 'Testing', 'TST-001'),
  ('Test Item B', 'Auto-generated dummy product B', 2.00, 999, 'Testing', 'TST-002'),
  ('Test Item C', 'Auto-generated dummy product C', 3.00, 999, 'Testing', 'TST-003'),
  ('Test Item D', 'Auto-generated dummy product D', 4.00, 999, 'Testing', 'TST-004'),
  ('Test Item E', 'Auto-generated dummy product E', 5.00, 999, 'Testing', 'TST-005')

ON CONFLICT (sku) DO NOTHING;
