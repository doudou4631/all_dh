-- Reuse only the Tencent-style user page shell for Taidixiong second.
-- The component selects the dedicated td_second APIs from the route platformCode.
UPDATE sys_menu
SET component = 'server/mark/user/tencent',
    query = '{"platformCode":"td_second","platformName":"Taidixiong二次"}',
    route_name = 'MarkUserTdSecond',
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = 900100000214;
