SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- M37: agent downstream page becomes processed-order overview

UPDATE sys_menu
SET menu_name = CONVERT(UNHEX('E5A484E79086E680BBE8A788') USING utf8mb4),
    update_by = 'admin',
    update_time = NOW(),
    remark = 'M37-agent-process-overview'
WHERE menu_id = 900100000119;

UPDATE sys_menu
SET visible = '1',
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = 900100000149;

SET FOREIGN_KEY_CHECKS = 1;
