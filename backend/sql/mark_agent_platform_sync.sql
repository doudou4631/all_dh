-- Keep the three teddy platforms as independent user and agent entries.
UPDATE sys_menu SET order_num = 1, query = '{"platformCode":"taidixiong","platformName":"泰迪熊"}'
WHERE menu_id = 900100000302;
UPDATE sys_menu SET order_num = 2 WHERE menu_id = 900100000303;
UPDATE sys_menu SET order_num = 3, query = '{"platformCodes":"360fugai,qihu_second","platformName":"360"}'
WHERE menu_id = 900100000304;
UPDATE sys_menu SET order_num = 4 WHERE menu_id = 900100000305;
UPDATE sys_menu SET order_num = 5 WHERE menu_id = 900100000306;
UPDATE sys_menu SET order_num = 6 WHERE menu_id = 900100000307;
UPDATE sys_menu SET order_num = 7 WHERE menu_id = 900100000308;
UPDATE sys_menu SET order_num = 8 WHERE menu_id = 900100000309;
UPDATE sys_menu SET order_num = 9 WHERE menu_id = 900100000310;
UPDATE sys_menu SET visible = '0', status = '0' WHERE menu_id IN (900100000311, 900100000312);
UPDATE sys_menu SET visible = '1', status = '1' WHERE menu_id = 900100000314;
