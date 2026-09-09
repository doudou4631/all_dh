-- Number-query authorization follows the role's checked page menus.
UPDATE sys_menu
SET perms = 'server:apiServer:singleQuery'
WHERE menu_id = 93823086376000137
  AND menu_name = '单次查询';

UPDATE sys_menu
SET perms = 'server:apiServer:batchQuery'
WHERE menu_id = 93823137356000167
  AND menu_name = '批量查询';
