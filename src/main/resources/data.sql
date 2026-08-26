-- =============================================
-- Auth 相關資料
-- =============================================

-- auth_role (角色) - 四個角色與 docs/requirements/specification/master/User.md 的角色定義表一致
INSERT INTO auth_role (role_code, role_name, created_at, updated_at, created_by, updated_by) VALUES (N'ADMIN', N'系統管理員', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO auth_role (role_code, role_name, created_at, updated_at, created_by, updated_by) VALUES (N'LEADER', N'組長', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO auth_role (role_code, role_name, created_at, updated_at, created_by, updated_by) VALUES (N'SALES', N'業務員', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO auth_role (role_code, role_name, created_at, updated_at, created_by, updated_by) VALUES (N'WAREHOUSE', N'庫務', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- auth_user (使用者) - 密碼為 BCrypt 加密後的 "password123"
INSERT INTO auth_user (user_code, email, user_name, password, branch_code, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'A001', N'admin@example.com', N'管理員', N'$2a$10$aRi0uOtcUAdiokog5Fpq3OT0VmN.eNMhqFmeIMb.buX7uZMA7Whxa', N'1000', N'0912345678', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO auth_user (user_code, email, user_name, password, branch_code, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'U001', N'user1@example.com', N'王小明', N'$2a$10$aRi0uOtcUAdiokog5Fpq3OT0VmN.eNMhqFmeIMb.buX7uZMA7Whxa', N'1000', N'0923456789', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO auth_user (user_code, email, user_name, password, branch_code, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'U002', N'user2@example.com', N'李小華', N'$2a$10$aRi0uOtcUAdiokog5Fpq3OT0VmN.eNMhqFmeIMb.buX7uZMA7Whxa', N'1100', N'0934567890', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO auth_user (user_code, email, user_name, password, branch_code, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'U003', N'user3@example.com', N'二狗子', N'$2a$10$aRi0uOtcUAdiokog5Fpq3OT0VmN.eNMhqFmeIMb.buX7uZMA7Whxa', N'1100', N'0934567891', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- auth_user_branch_role (使用者營業所角色關聯)
INSERT INTO auth_user_branch_role (user_code, branch_code, role_code, created_at, updated_at, created_by, updated_by) VALUES (N'A001', N'1000', N'ADMIN', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO auth_user_branch_role (user_code, branch_code, role_code, created_at, updated_at, created_by, updated_by) VALUES (N'U001', N'1000', N'SALES', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO auth_user_branch_role (user_code, branch_code, role_code, created_at, updated_at, created_by, updated_by) VALUES (N'U002', N'1100', N'LEADER', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO auth_user_branch_role (user_code, branch_code, role_code, created_at, updated_at, created_by, updated_by) VALUES (N'U003', N'1100', N'WAREHOUSE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- =============================================
-- Master 主檔資料
-- =============================================

-- sales_org (銷售組織)
INSERT INTO sales_org (sales_org_code, sales_org_name, status, created_at, updated_at, created_by, updated_by) VALUES (N'SO001', N'台北銷售組織', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO sales_org (sales_org_code, sales_org_name, status, created_at, updated_at, created_by, updated_by) VALUES (N'SO002', N'台中銷售組織', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- factory (工廠/供應商) - 味全不同地區廠區
INSERT INTO factory (factory_code, factory_name, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'F001', N'味全台北廠', N'台北市內湖區瑞光路513巷26號', N'02-26578900', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO factory (factory_code, factory_name, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'F002', N'味全桃園廠', N'桃園市中壢區中園路220號', N'03-4526789', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO factory (factory_code, factory_name, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'F003', N'味全台中廠', N'台中市大里區工業路11號', N'04-24961234', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO factory (factory_code, factory_name, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'F004', N'味全高雄廠', N'高雄市仁武區水管路100號', N'07-3721234', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- product (產品) - 10筆味全產品資料
INSERT INTO product (product_code, product_name, base_unit, base_price, status, created_at, updated_at, created_by, updated_by) VALUES (N'P001', N'林鳳營鮮乳(936ml)', N'瓶', 75.00, N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product (product_code, product_name, base_unit, base_price, status, created_at, updated_at, created_by, updated_by) VALUES (N'P002', N'林鳳營鮮乳(1857ml)', N'瓶', 135.00, N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product (product_code, product_name, base_unit, base_price, status, created_at, updated_at, created_by, updated_by) VALUES (N'P003', N'林鳳營低脂鮮乳(1857ml)', N'瓶', 130.00, N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product (product_code, product_name, base_unit, base_price, status, created_at, updated_at, created_by, updated_by) VALUES (N'P004', N'林鳳營優酪乳-原味(500ml)', N'瓶', 45.00, N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product (product_code, product_name, base_unit, base_price, status, created_at, updated_at, created_by, updated_by) VALUES (N'P005', N'每日C柳橙汁(1400ml)', N'瓶', 99.00, N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product (product_code, product_name, base_unit, base_price, status, created_at, updated_at, created_by, updated_by) VALUES (N'P006', N'每日C葡萄柚汁(1400ml)', N'瓶', 99.00, N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product (product_code, product_name, base_unit, base_price, status, created_at, updated_at, created_by, updated_by) VALUES (N'P007', N'貝納頌咖啡-經典拿鐵(375ml)', N'瓶', 42.00, N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product (product_code, product_name, base_unit, base_price, status, created_at, updated_at, created_by, updated_by) VALUES (N'P008', N'貝納頌咖啡-榛果風味(375ml)', N'瓶', 42.00, N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product (product_code, product_name, base_unit, base_price, status, created_at, updated_at, created_by, updated_by) VALUES (N'P009', N'36法郎典藏咖啡(360ml)', N'瓶', 55.00, N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product (product_code, product_name, base_unit, base_price, status, created_at, updated_at, created_by, updated_by) VALUES (N'P010', N'木瓜牛乳(936ml)', N'瓶', 65.00, N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- product_factory (產品工廠關聯)
INSERT INTO product_factory (product_code, factory_code, is_default, created_at, updated_at, created_by, updated_by) VALUES (N'P001', N'F001', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_factory (product_code, factory_code, is_default, created_at, updated_at, created_by, updated_by) VALUES (N'P002', N'F004', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_factory (product_code, factory_code, is_default, created_at, updated_at, created_by, updated_by) VALUES (N'P003', N'F002', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_factory (product_code, factory_code, is_default, created_at, updated_at, created_by, updated_by) VALUES (N'P004', N'F001', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_factory (product_code, factory_code, is_default, created_at, updated_at, created_by, updated_by) VALUES (N'P005', N'F003', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_factory (product_code, factory_code, is_default, created_at, updated_at, created_by, updated_by) VALUES (N'P006', N'F001', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_factory (product_code, factory_code, is_default, created_at, updated_at, created_by, updated_by) VALUES (N'P007', N'F003', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_factory (product_code, factory_code, is_default, created_at, updated_at, created_by, updated_by) VALUES (N'P008', N'F004', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_factory (product_code, factory_code, is_default, created_at, updated_at, created_by, updated_by) VALUES (N'P009', N'F002', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_factory (product_code, factory_code, is_default, created_at, updated_at, created_by, updated_by) VALUES (N'P010', N'F001', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- product_unit_conversion (產品單位換算)
INSERT INTO product_unit_conversion (product_code, from_unit, to_unit, conversion_rate, created_at, updated_at, created_by, updated_by) VALUES (N'P001', N'箱', N'瓶', 12.0000, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_unit_conversion (product_code, from_unit, to_unit, conversion_rate, created_at, updated_at, created_by, updated_by) VALUES (N'P002', N'箱', N'瓶', 6.0000, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_unit_conversion (product_code, from_unit, to_unit, conversion_rate, created_at, updated_at, created_by, updated_by) VALUES (N'P003', N'箱', N'瓶', 6.0000, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_unit_conversion (product_code, from_unit, to_unit, conversion_rate, created_at, updated_at, created_by, updated_by) VALUES (N'P004', N'箱', N'瓶', 12.0000, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_unit_conversion (product_code, from_unit, to_unit, conversion_rate, created_at, updated_at, created_by, updated_by) VALUES (N'P005', N'箱', N'瓶', 6.0000, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_unit_conversion (product_code, from_unit, to_unit, conversion_rate, created_at, updated_at, created_by, updated_by) VALUES (N'P006', N'箱', N'瓶', 6.0000, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_unit_conversion (product_code, from_unit, to_unit, conversion_rate, created_at, updated_at, created_by, updated_by) VALUES (N'P007', N'箱', N'瓶', 24.0000, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_unit_conversion (product_code, from_unit, to_unit, conversion_rate, created_at, updated_at, created_by, updated_by) VALUES (N'P008', N'箱', N'瓶', 24.0000, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_unit_conversion (product_code, from_unit, to_unit, conversion_rate, created_at, updated_at, created_by, updated_by) VALUES (N'P009', N'箱', N'瓶', 24.0000, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO product_unit_conversion (product_code, from_unit, to_unit, conversion_rate, created_at, updated_at, created_by, updated_by) VALUES (N'P010', N'箱', N'瓶', 12.0000, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- customer (客戶)
INSERT INTO customer (customer_code, customer_name, sales_org_code, user_code, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'C001', N'全家便利商店-信義店', N'SO001', N'U001', N'台北市信義區信義路四段100號', N'02-27001234', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO customer (customer_code, customer_name, sales_org_code, user_code, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'C002', N'7-ELEVEN-忠孝店', N'SO001', N'U001', N'台北市大安區忠孝東路四段200號', N'02-27711234', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO customer (customer_code, customer_name, sales_org_code, user_code, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'C003', N'萊爾富-松山店', N'SO001', N'U001', N'台北市松山區南京東路五段50號', N'02-27651234', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO customer (customer_code, customer_name, sales_org_code, user_code, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'C004', N'全聯福利中心-北屯店', N'SO002', N'U002', N'台中市北屯區文心路四段500號', N'04-22341234', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO customer (customer_code, customer_name, sales_org_code, user_code, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'C005', N'家樂福-台中店', N'SO002', N'U002', N'台中市西屯區台灣大道四段600號', N'04-23521234', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO customer (customer_code, customer_name, sales_org_code, user_code, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'C006', N'頂好超市-霧峰店', N'SO002', N'U002', N'台中市霧峰區中正路800號', N'04-23391234', N'INACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- =============================================
-- Branch 相關資料
-- =============================================

-- branch (營業所)
INSERT INTO branch (branch_code, sales_org_code, branch_name, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'1000', N'SO001', N'信義總部', N'台北市信義區忠孝東路一段1號', N'02-12345678', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO branch (branch_code, sales_org_code, branch_name, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'1100', N'SO002', N'北屯營業所', N'台中市北屯區陳平路1號', N'04-23456789', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO branch (branch_code, sales_org_code, branch_name, address, phone, status, created_at, updated_at, created_by, updated_by) VALUES (N'1200', N'SO002', N'霧峰營業所', N'台中市霧峰區樹仁路25號', N'04-14567890', N'INACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- location (儲位)
INSERT INTO location (location_code, location_name, branch_code, user_code, location_type, status, created_at, updated_at, created_by, updated_by) VALUES (N'1000', N'信義總部倉庫', N'1000', NULL, N'WAREHOUSE', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO location (location_code, location_name, branch_code, user_code, location_type, status, created_at, updated_at, created_by, updated_by) VALUES (N'1011', N'王小明', N'1000', N'U001', N'CAR', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO location (location_code, location_name, branch_code, user_code, location_type, status, created_at, updated_at, created_by, updated_by) VALUES (N'1100', N'北屯營業所倉庫', N'1100', NULL, N'WAREHOUSE', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO location (location_code, location_name, branch_code, user_code, location_type, status, created_at, updated_at, created_by, updated_by) VALUES (N'1110', N'李小華', N'1100', N'U002', N'CAR', N'ACTIVE', N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- branch_product_list (營業所商品清單)
INSERT INTO branch_product_list (branch_code, product_code, product_name, unit, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1000', N'P001', N'林鳳營鮮乳(936ml)', N'瓶', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO branch_product_list (branch_code, product_code, product_name, unit, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1000', N'P002', N'林鳳營鮮乳(1857ml)', N'瓶', 2, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO branch_product_list (branch_code, product_code, product_name, unit, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1000', N'P004', N'林鳳營優酪乳-原味(500ml)', N'瓶', 3, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO branch_product_list (branch_code, product_code, product_name, unit, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1000', N'P005', N'每日C柳橙汁(1400ml)', N'瓶', 4, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO branch_product_list (branch_code, product_code, product_name, unit, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1000', N'P006', N'每日C葡萄柚汁(1400ml)', N'瓶', 5, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO branch_product_list (branch_code, product_code, product_name, unit, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1100', N'P001', N'林鳳營鮮乳(936ml)', N'瓶', 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO branch_product_list (branch_code, product_code, product_name, unit, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1100', N'P003', N'林鳳營低脂鮮乳(1857ml)', N'瓶', 2, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO branch_product_list (branch_code, product_code, product_name, unit, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1100', N'P007', N'貝納頌咖啡-經典拿鐵(375ml)', N'瓶', 3, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO branch_product_list (branch_code, product_code, product_name, unit, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1100', N'P009', N'36法郎典藏咖啡(360ml)', N'瓶', 4, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO branch_product_list (branch_code, product_code, product_name, unit, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1100', N'P010', N'木瓜牛乳(936ml)', N'瓶', 5, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'SYSTEM', N'SYSTEM');

-- branch_purchase_frozen (營業所訂貨凍結狀態)
INSERT INTO branch_purchase_frozen (branch_code, purchase_date, status, frozen_at, frozen_by, created_at, updated_at, created_by, updated_by) VALUES (N'1000', N'2023-10-01', N'FROZEN', N'2023-09-30 18:00:00', N'U001', N'2023-09-30 18:00:00', N'2023-09-30 18:00:00', N'U001', N'U001');
INSERT INTO branch_purchase_frozen (branch_code, purchase_date, status, frozen_at, frozen_by, confirmed_at, confirmed_by, created_at, updated_at, created_by, updated_by) VALUES (N'1000', N'2023-10-02', N'CONFIRMED', N'2023-10-01 18:00:00', N'U001', N'2023-10-01 20:00:00', N'U001', N'2023-10-01 18:00:00', N'2023-10-01 20:00:00', N'U001', N'U001');

-- =============================================
-- Purchase 相關資料
-- =============================================

-- sales_purchase_order (業務訂貨單)
INSERT INTO sales_purchase_order (purchase_no, branch_code, location_code, purchase_date, purchase_user, created_at, updated_at, created_by, updated_by) VALUES (N'SPO-20231001-001', N'1000', N'1011', N'2023-10-01', N'U001', N'2023-09-30 10:00:00', N'2023-09-30 10:00:00', N'U001', N'U001');
INSERT INTO sales_purchase_order (purchase_no, branch_code, location_code, purchase_date, purchase_user, created_at, updated_at, created_by, updated_by) VALUES (N'SPO-20231002-002', N'1000', N'1011', N'2023-10-02', N'U001', N'2023-10-01 10:00:00', N'2023-10-01 10:00:00', N'U001', N'U001');
INSERT INTO sales_purchase_order (purchase_no, branch_code, location_code, purchase_date, purchase_user, created_at, updated_at, created_by, updated_by) VALUES (N'SPO-20231003-003', N'1100', N'1110', N'2023-10-03', N'U002', N'2023-10-02 10:00:00', N'2023-10-02 10:00:00', N'U002', N'U002');

-- sales_purchase_order_detail (業務訂貨單明細)
INSERT INTO sales_purchase_order_detail (purchase_no, item_no, product_code, unit, qty, confirmed_qty, status, created_at, updated_at, created_by, updated_by) VALUES (N'SPO-20231001-001', 1, N'P001', N'瓶', 100, 100, N'AGGREGATED', N'2023-09-30 10:00:00', N'2023-09-30 10:00:00', N'U001', N'U001');
INSERT INTO sales_purchase_order_detail (purchase_no, item_no, product_code, unit, qty, confirmed_qty, status, created_at, updated_at, created_by, updated_by) VALUES (N'SPO-20231001-001', 2, N'P004', N'瓶', 50, 50, N'AGGREGATED', N'2023-09-30 10:00:00', N'2023-09-30 10:00:00', N'U001', N'U001');
INSERT INTO sales_purchase_order_detail (purchase_no, item_no, product_code, unit, qty, confirmed_qty, status, created_at, updated_at, created_by, updated_by) VALUES (N'SPO-20231002-002', 1, N'P005', N'瓶', 200, 180, N'AGGREGATED', N'2023-10-01 10:00:00', N'2023-10-01 10:00:00', N'U001', N'U001');
INSERT INTO sales_purchase_order_detail (purchase_no, item_no, product_code, unit, qty, confirmed_qty, status, created_at, updated_at, created_by, updated_by) VALUES (N'SPO-20231003-003', 1, N'P001', N'瓶', 150, 0, N'PENDING', N'2023-10-02 10:00:00', N'2023-10-02 10:00:00', N'U002', N'U002');
INSERT INTO sales_purchase_order_detail (purchase_no, item_no, product_code, unit, qty, confirmed_qty, status, created_at, updated_at, created_by, updated_by) VALUES (N'SPO-20231003-003', 2, N'P007', N'瓶', 30, 0, N'PENDING', N'2023-10-02 10:00:00', N'2023-10-02 10:00:00', N'U002', N'U002');

-- sales_purchase_list (業務自訂訂貨清單)
INSERT INTO sales_purchase_list (location_code, product_code, unit, qty, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1011', N'P001', N'瓶', 50, 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'U001', N'U001');
INSERT INTO sales_purchase_list (location_code, product_code, unit, qty, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1011', N'P004', N'個', 30, 2, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'U001', N'U001');
INSERT INTO sales_purchase_list (location_code, product_code, unit, qty, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1011', N'P006', N'瓶', 100, 3, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'U001', N'U001');
INSERT INTO sales_purchase_list (location_code, product_code, unit, qty, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1110', N'P001', N'瓶', 80, 1, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'U002', N'U002');
INSERT INTO sales_purchase_list (location_code, product_code, unit, qty, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1110', N'P003', N'瓶', 40, 2, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'U002', N'U002');
INSERT INTO sales_purchase_list (location_code, product_code, unit, qty, sort_order, created_at, updated_at, created_by, updated_by) VALUES (N'1110', N'P009', N'瓶', 60, 3, N'2023-01-01 00:00:00', N'2023-01-01 00:00:00', N'U002', N'U002');

-- =============================================
-- Sequence 相關資料
-- =============================================

-- document_sequence (單據序號表) - 此表不需要 audit 欄位
INSERT INTO document_sequence (sequence_type, sequence_date, current_no) VALUES (N'SPO', N'2023-10-01', 1);
INSERT INTO document_sequence (sequence_type, sequence_date, current_no) VALUES (N'SPO', N'2023-10-02', 2);
INSERT INTO document_sequence (sequence_type, sequence_date, current_no) VALUES (N'SPO', N'2023-10-03', 3);
INSERT INTO document_sequence (sequence_type, sequence_date, current_no) VALUES (N'FDO', N'2026-02-25', 2);

-- =============================================
-- Receive / Inventory 相關資料
-- =============================================

-- branch_purchase_order (BPO 測試資料)
INSERT INTO branch_purchase_order (bpo_no, branch_code, factory_code, purchase_date, status, created_at, updated_at, created_by, updated_by) VALUES (N'BPO-20260224-001', N'1000', N'F001', N'2026-02-24', N'PENDING', N'2026-02-24 20:00:00', N'2026-02-24 20:00:00', N'A001', N'A001');
INSERT INTO branch_purchase_order (bpo_no, branch_code, factory_code, purchase_date, status, created_at, updated_at, created_by, updated_by) VALUES (N'BPO-20260224-002', N'1000', N'F003', N'2026-02-24', N'RECEIVED', N'2026-02-24 20:00:00', N'2026-02-25 14:00:00', N'A001', N'A001');

-- branch_purchase_order_detail (BPOD 測試資料)
INSERT INTO branch_purchase_order_detail (bpo_no, item_no, product_code, product_name, unit, qty, created_at, updated_at, created_by, updated_by) VALUES (N'BPO-20260224-001', 1, N'P001', N'林鳳營鮮乳(936ml)', N'瓶', 100, N'2026-02-24 20:00:00', N'2026-02-24 20:00:00', N'A001', N'A001');
INSERT INTO branch_purchase_order_detail (bpo_no, item_no, product_code, product_name, unit, qty, created_at, updated_at, created_by, updated_by) VALUES (N'BPO-20260224-001', 2, N'P004', N'林鳳營優酪乳-原味(500ml)', N'瓶', 50, N'2026-02-24 20:00:00', N'2026-02-24 20:00:00', N'A001', N'A001');
INSERT INTO branch_purchase_order_detail (bpo_no, item_no, product_code, product_name, unit, qty, created_at, updated_at, created_by, updated_by) VALUES (N'BPO-20260224-001', 3, N'P006', N'每日C葡萄柚汁(1400ml)', N'瓶', 80, N'2026-02-24 20:00:00', N'2026-02-24 20:00:00', N'A001', N'A001');
INSERT INTO branch_purchase_order_detail (bpo_no, item_no, product_code, product_name, unit, qty, created_at, updated_at, created_by, updated_by) VALUES (N'BPO-20260224-002', 1, N'P005', N'每日C柳橙汁(1400ml)', N'瓶', 60, N'2026-02-24 20:00:00', N'2026-02-24 20:00:00', N'A001', N'A001');
INSERT INTO branch_purchase_order_detail (bpo_no, item_no, product_code, product_name, unit, qty, created_at, updated_at, created_by, updated_by) VALUES (N'BPO-20260224-002', 2, N'P007', N'貝納頌咖啡-經典拿鐵(375ml)', N'瓶', 120, N'2026-02-24 20:00:00', N'2026-02-24 20:00:00', N'A001', N'A001');

-- factory_delivery_order (FDO 測試資料 - 1 筆 PENDING, 1 筆 RECEIVED)
INSERT INTO factory_delivery_order (fdo_no, bpo_no, branch_code, factory_code, delivery_date, status, received_at, received_by, remark, created_at, updated_at, created_by, updated_by) VALUES (N'FDO-20260225-001', N'BPO-20260224-001', N'1000', N'F001', N'2026-02-25', N'PENDING', NULL, NULL, NULL, N'2026-02-25 08:00:00', N'2026-02-25 08:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO factory_delivery_order (fdo_no, bpo_no, branch_code, factory_code, delivery_date, status, received_at, received_by, remark, created_at, updated_at, created_by, updated_by) VALUES (N'FDO-20260225-002', N'BPO-20260224-002', N'1000', N'F003', N'2026-02-25', N'RECEIVED', N'2026-02-25 14:30:00', N'A001', NULL, N'2026-02-25 08:00:00', N'2026-02-25 14:30:00', N'SYSTEM', N'A001');

-- factory_delivery_order_detail (FDO Detail 測試資料)
-- FDO-001 PENDING (未收貨)
INSERT INTO factory_delivery_order_detail (fdo_no, item_no, product_code, product_name, batch_no, expiry_date, unit, qty, received_qty, created_at, updated_at, created_by, updated_by) VALUES (N'FDO-20260225-001', 1, N'P001', N'林鳳營鮮乳(936ml)', N'BAT-20260225-001', N'2026-03-27', N'瓶', 100, NULL, N'2026-02-25 08:00:00', N'2026-02-25 08:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO factory_delivery_order_detail (fdo_no, item_no, product_code, product_name, batch_no, expiry_date, unit, qty, received_qty, created_at, updated_at, created_by, updated_by) VALUES (N'FDO-20260225-001', 2, N'P004', N'林鳳營優酪乳-原味(500ml)', N'BAT-20260225-002', N'2026-03-27', N'瓶', 50, NULL, N'2026-02-25 08:00:00', N'2026-02-25 08:00:00', N'SYSTEM', N'SYSTEM');
INSERT INTO factory_delivery_order_detail (fdo_no, item_no, product_code, product_name, batch_no, expiry_date, unit, qty, received_qty, created_at, updated_at, created_by, updated_by) VALUES (N'FDO-20260225-001', 3, N'P006', N'每日C葡萄柚汁(1400ml)', N'BAT-20260225-003', N'2026-03-27', N'瓶', 80, NULL, N'2026-02-25 08:00:00', N'2026-02-25 08:00:00', N'SYSTEM', N'SYSTEM');
-- FDO-002 RECEIVED (已收貨)
INSERT INTO factory_delivery_order_detail (fdo_no, item_no, product_code, product_name, batch_no, expiry_date, unit, qty, received_qty, created_at, updated_at, created_by, updated_by) VALUES (N'FDO-20260225-002', 1, N'P005', N'每日C柳橙汁(1400ml)', N'BAT-20260225-004', N'2026-03-27', N'瓶', 60, 60, N'2026-02-25 08:00:00', N'2026-02-25 14:30:00', N'SYSTEM', N'A001');
INSERT INTO factory_delivery_order_detail (fdo_no, item_no, product_code, product_name, batch_no, expiry_date, unit, qty, received_qty, created_at, updated_at, created_by, updated_by) VALUES (N'FDO-20260225-002', 2, N'P007', N'貝納頌咖啡-經典拿鐵(375ml)', N'BAT-20260225-005', N'2026-03-27', N'瓶', 120, 120, N'2026-02-25 08:00:00', N'2026-02-25 14:30:00', N'SYSTEM', N'A001');

-- inventory (庫存 - RECEIVED 那筆的入庫結果)
INSERT INTO inventory (branch_code, location_code, location_type, product_code, batch_no, expiry_date, qty, keep_qty, return_qty, created_at, updated_at, created_by, updated_by) VALUES (N'1000', N'1000', N'WAREHOUSE', N'P005', N'BAT-20260225-004', N'2026-03-27', 60, 0, 0, N'2026-02-25 14:30:00', N'2026-02-25 14:30:00', N'SYSTEM', N'SYSTEM');
INSERT INTO inventory (branch_code, location_code, location_type, product_code, batch_no, expiry_date, qty, keep_qty, return_qty, created_at, updated_at, created_by, updated_by) VALUES (N'1000', N'1000', N'WAREHOUSE', N'P007', N'BAT-20260225-005', N'2026-03-27', 120, 0, 0, N'2026-02-25 14:30:00', N'2026-02-25 14:30:00', N'SYSTEM', N'SYSTEM');
