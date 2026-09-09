/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-11.4.4-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: verifynum
-- ------------------------------------------------------
-- Server version	11.4.4-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user` (
  `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) DEFAULT '' COMMENT '手机号码',
  `sex` char(1) DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `points` bigint(20) DEFAULT 0 COMMENT '用户积分',
  `rel_template` varchar(50) DEFAULT NULL COMMENT '可用服务模板',
  `rel_mark_template` bigint(20) DEFAULT NULL COMMENT '标记业务模板ID',
  `agent_level` tinyint(4) DEFAULT NULL COMMENT '代理等级0管理员1一级2二级3三级',
  `parent_user_id` bigint(20) DEFAULT NULL COMMENT '直属上级用户ID',
  `status` char(1) DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` smallint(6) DEFAULT 0 COMMENT '删除标志（0代表存在 1代表删除）',
  `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `pwd_update_date` datetime DEFAULT NULL COMMENT '密码最后更新时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`) USING BTREE,
  KEY `idx_sys_user_agent_parent` (`agent_level`,`parent_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=104390189581000118 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=DYNAMIC COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES
(1,103,'admin','极客','00','ry@163.com','15888888888','1','avatar/admin/1/20260305112747-avatar.jpeg','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',9904,'15',NULL,0,NULL,'0',0,'127.0.0.1','2026-07-25 21:28:08','2025-11-27 14:34:19','admin','2025-11-27 14:34:19','admin','2026-07-25 21:28:08','管理员'),
(2,105,'ry','若依','00','ry@qq.com','15666666666','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',25,'12',NULL,NULL,NULL,'0',1,'127.0.0.1','2025-11-27 14:34:19','2025-11-27 14:34:19','admin','2025-11-27 14:34:19','admin','2026-03-19 09:08:28','测试员1'),
(93499740984000118,NULL,'test','测试','00','','','0','','$2a$10$lYjkD1ovqfq5xJ/Rw58NGuXfB91FsCSZ6iGrjhFnt.Q7zg4bekOTi',1100,'12',NULL,NULL,NULL,'0',1,'127.0.0.1','2026-03-19 21:00:39','2026-03-19 17:10:42','admin','2026-03-19 17:10:01','admin','2026-03-19 21:00:39',NULL),
(104144371718000167,NULL,'110125','110125','00','','','0','','$2a$10$EJiYP7eXDduBJbqFrVdrNODdbfDctB95zMrUdWYpSdBzHVKuKAtQq',49973,'15',NULL,NULL,NULL,'0',0,'127.0.0.1','2026-07-25 21:27:30',NULL,'admin','2026-07-20 22:00:31','admin','2026-07-25 21:27:30',NULL),
(104390088199000115,NULL,'11011111','111','00','','','0','','$2a$10$L/DYMhAQeT0DmCBcNuM7dO25QhcJfR0dXdsw8xXMSgri2XGhxNoZi',0,NULL,NULL,1,1,'0',1,'127.0.0.1','2026-07-23 18:16:55',NULL,'admin','2026-07-23 18:15:48','','2026-07-23 18:16:55',NULL),
(104390189581000117,NULL,'11011112','12121','00','','','0','','$2a$10$4SMULPVFP5pmAiodoY6kHuj7Sub3emoJkD92DGjdgLn3jcimBwXcK',0,NULL,NULL,2,104390088199000115,'0',1,'127.0.0.1','2026-07-23 18:19:13',NULL,'11011111','2026-07-23 18:17:29','11011111','2026-07-23 18:19:13',NULL);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=DYNAMIC COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES
(1,1),
(104144371718000167,3);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role` (
  `role_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort` int(11) NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(4) DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(4) DEFAULT 1 COMMENT '部门树选择项是否关联显示',
  `status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` smallint(6) DEFAULT 0 COMMENT '删除标志（0代表存在 1代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=DYNAMIC COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES
(1,'超级管理员','admin',1,'1',1,1,'0',0,'admin','2025-11-27 14:34:19','',NULL,'超级管理员'),
(2,'普通角色','common',2,'2',1,1,'0',0,'admin','2025-11-27 14:34:19','admin','2026-07-25 23:59:35','普通角色-用户端'),
(3,'标记用户','mark_user',30,'1',1,1,'0',0,'admin','2026-07-23 00:24:26','admin','2026-07-25 02:42:02','最小标记用户角色'),
(4,'标记代理','mark_agent',31,'1',1,1,'0',0,'admin','2026-07-23 00:24:26','admin','2026-07-26 00:00:43','处理标记-代理');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int(11) DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(50) DEFAULT '' COMMENT '路由名称',
  `is_frame` int(11) DEFAULT 1 COMMENT '是否为外链（0是 1否）',
  `is_cache` int(11) DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=104500000000000202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=DYNAMIC COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES
(1,'系统管理',0,1,'system','','','',1,0,'M','0','0','','system','admin','2025-11-27 14:34:19','',NULL,'系统管理目录'),
(2,'系统监控',0,2,'monitor','','','',1,0,'M','0','0','','monitor','admin','2025-11-27 14:34:19','',NULL,'系统监控目录'),
(3,'系统工具',0,3,'tool','','','',1,0,'M','0','0','','tool','admin','2025-11-27 14:34:19','admin','2026-03-19 21:01:14','系统工具目录'),
(100,'用户管理',1,1,'user','system/user/index','','',1,0,'C','0','0','system:user:list','user','admin','2025-11-27 14:34:19','admin','2026-07-20 21:59:13','用户管理菜单'),
(101,'角色管理',1,2,'role','system/role/index','','',1,0,'C','0','0','system:role:list','peoples','admin','2025-11-27 14:34:19','',NULL,'角色管理菜单'),
(102,'菜单管理',1,3,'menu','system/menu/index','','',1,0,'C','0','0','system:menu:list','tree-table','admin','2025-11-27 14:34:19','',NULL,'菜单管理菜单'),
(103,'部门管理',3,4,'dept','system/dept/index','','',1,0,'C','0','0','system:dept:list','tree','admin','2025-11-27 14:34:19','admin','2026-07-20 21:57:39','部门管理菜单'),
(104,'岗位管理',3,5,'post','system/post/index','','',1,0,'C','0','0','system:post:list','post','admin','2025-11-27 14:34:19','admin','2026-07-20 21:57:45','岗位管理菜单'),
(105,'字典管理',3,6,'dict','system/dict/index','','',1,0,'C','0','0','system:dict:list','dict','admin','2025-11-27 14:34:19','admin','2026-07-20 21:57:50','字典管理菜单'),
(106,'参数设置',3,7,'config','system/config/index','','',1,0,'C','0','0','system:config:list','edit','admin','2025-11-27 14:34:19','admin','2026-07-20 21:57:56','参数设置菜单'),
(107,'通知公告',1,8,'notice','system/notice/index','','',1,0,'C','0','0','system:notice:list','message','admin','2025-11-27 14:34:19','',NULL,'通知公告菜单'),
(108,'日志管理',1,9,'log','','','',1,0,'M','0','0','','log','admin','2025-11-27 14:34:19','',NULL,'日志管理菜单'),
(109,'在线用户',2,1,'online','monitor/online/index','','',1,0,'C','0','0','monitor:online:list','online','admin','2025-11-27 14:34:19','',NULL,'在线用户菜单'),
(110,'定时任务',2,2,'job','monitor/job/index','','',1,0,'C','0','0','monitor:job:list','job','admin','2025-11-27 14:34:19','',NULL,'定时任务菜单'),
(111,'数据监控',2,3,'druid','monitor/druid/index','','',1,0,'C','0','0','monitor:druid:list','druid','admin','2025-11-27 14:34:19','',NULL,'数据监控菜单'),
(112,'服务监控',2,4,'server','monitor/server/index','','',1,0,'C','0','0','monitor:server:list','server','admin','2025-11-27 14:34:19','',NULL,'服务监控菜单'),
(114,'缓存列表',2,6,'cacheList','monitor/cache/list','','',1,0,'C','0','0','monitor:cache:list','redis-list','admin','2025-11-27 14:34:19','',NULL,'缓存列表菜单'),
(115,'表单构建',3,1,'build','tool/build/index','','',1,0,'C','0','0','tool:build:list','build','admin','2025-11-27 14:34:19','',NULL,'表单构建菜单'),
(116,'代码生成',3,2,'gen','tool/gen/index','','',1,0,'C','0','0','tool:gen:list','code','admin','2025-11-27 14:34:35','',NULL,'代码生成菜单'),
(117,'系统接口',3,3,'swagger','tool/swagger/index','','',1,0,'C','0','0','tool:swagger:list','swagger','admin','2025-11-27 14:34:19','',NULL,'系统接口菜单'),
(500,'操作日志',108,1,'operlog','monitor/operlog/index','','',1,0,'C','0','0','monitor:operlog:list','form','admin','2025-11-27 14:34:19','',NULL,'操作日志菜单'),
(501,'登录日志',108,2,'logininfor','monitor/logininfor/index','','',1,0,'C','0','0','monitor:logininfor:list','logininfor','admin','2025-11-27 14:34:19','',NULL,'登录日志菜单'),
(1000,'用户查询',100,1,'','','','',1,0,'F','0','0','system:user:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1001,'用户新增',100,2,'','','','',1,0,'F','0','0','system:user:add','#','admin','2025-11-27 14:34:19','',NULL,''),
(1002,'用户修改',100,3,'','','','',1,0,'F','0','0','system:user:edit','#','admin','2025-11-27 14:34:19','',NULL,''),
(1003,'用户删除',100,4,'','','','',1,0,'F','0','0','system:user:remove','#','admin','2025-11-27 14:34:19','',NULL,''),
(1004,'用户导出',100,5,'','','','',1,0,'F','0','0','system:user:export','#','admin','2025-11-27 14:34:19','',NULL,''),
(1005,'用户导入',100,6,'','','','',1,0,'F','0','0','system:user:import','#','admin','2025-11-27 14:34:19','',NULL,''),
(1006,'重置密码',100,7,'','','','',1,0,'F','0','0','system:user:resetPwd','#','admin','2025-11-27 14:34:19','',NULL,''),
(1007,'角色查询',101,1,'','','','',1,0,'F','0','0','system:role:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1008,'角色新增',101,2,'','','','',1,0,'F','0','0','system:role:add','#','admin','2025-11-27 14:34:19','',NULL,''),
(1009,'角色修改',101,3,'','','','',1,0,'F','0','0','system:role:edit','#','admin','2025-11-27 14:34:19','',NULL,''),
(1010,'角色删除',101,4,'','','','',1,0,'F','0','0','system:role:remove','#','admin','2025-11-27 14:34:19','',NULL,''),
(1011,'角色导出',101,5,'','','','',1,0,'F','0','0','system:role:export','#','admin','2025-11-27 14:34:19','',NULL,''),
(1012,'菜单查询',102,1,'','','','',1,0,'F','0','0','system:menu:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1013,'菜单新增',102,2,'','','','',1,0,'F','0','0','system:menu:add','#','admin','2025-11-27 14:34:19','',NULL,''),
(1014,'菜单修改',102,3,'','','','',1,0,'F','0','0','system:menu:edit','#','admin','2025-11-27 14:34:19','',NULL,''),
(1015,'菜单删除',102,4,'','','','',1,0,'F','0','0','system:menu:remove','#','admin','2025-11-27 14:34:19','',NULL,''),
(1016,'部门查询',103,1,'','','','',1,0,'F','0','0','system:dept:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1017,'部门新增',103,2,'','','','',1,0,'F','0','0','system:dept:add','#','admin','2025-11-27 14:34:19','',NULL,''),
(1018,'部门修改',103,3,'','','','',1,0,'F','0','0','system:dept:edit','#','admin','2025-11-27 14:34:19','',NULL,''),
(1019,'部门删除',103,4,'','','','',1,0,'F','0','0','system:dept:remove','#','admin','2025-11-27 14:34:19','',NULL,''),
(1020,'岗位查询',104,1,'','','','',1,0,'F','0','0','system:post:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1021,'岗位新增',104,2,'','','','',1,0,'F','0','0','system:post:add','#','admin','2025-11-27 14:34:19','',NULL,''),
(1022,'岗位修改',104,3,'','','','',1,0,'F','0','0','system:post:edit','#','admin','2025-11-27 14:34:19','',NULL,''),
(1023,'岗位删除',104,4,'','','','',1,0,'F','0','0','system:post:remove','#','admin','2025-11-27 14:34:19','',NULL,''),
(1024,'岗位导出',104,5,'','','','',1,0,'F','0','0','system:post:export','#','admin','2025-11-27 14:34:19','',NULL,''),
(1025,'字典查询',105,1,'#','','','',1,0,'F','0','0','system:dict:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1026,'字典新增',105,2,'#','','','',1,0,'F','0','0','system:dict:add','#','admin','2025-11-27 14:34:19','',NULL,''),
(1027,'字典修改',105,3,'#','','','',1,0,'F','0','0','system:dict:edit','#','admin','2025-11-27 14:34:19','',NULL,''),
(1028,'字典删除',105,4,'#','','','',1,0,'F','0','0','system:dict:remove','#','admin','2025-11-27 14:34:19','',NULL,''),
(1029,'字典导出',105,5,'#','','','',1,0,'F','0','0','system:dict:export','#','admin','2025-11-27 14:34:19','',NULL,''),
(1030,'参数查询',106,1,'#','','','',1,0,'F','0','0','system:config:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1031,'参数新增',106,2,'#','','','',1,0,'F','0','0','system:config:add','#','admin','2025-11-27 14:34:19','',NULL,''),
(1032,'参数修改',106,3,'#','','','',1,0,'F','0','0','system:config:edit','#','admin','2025-11-27 14:34:19','',NULL,''),
(1033,'参数删除',106,4,'#','','','',1,0,'F','0','0','system:config:remove','#','admin','2025-11-27 14:34:19','',NULL,''),
(1034,'参数导出',106,5,'#','','','',1,0,'F','0','0','system:config:export','#','admin','2025-11-27 14:34:19','',NULL,''),
(1035,'公告查询',107,1,'#','','','',1,0,'F','0','0','system:notice:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1036,'公告新增',107,2,'#','','','',1,0,'F','0','0','system:notice:add','#','admin','2025-11-27 14:34:19','',NULL,''),
(1037,'公告修改',107,3,'#','','','',1,0,'F','0','0','system:notice:edit','#','admin','2025-11-27 14:34:19','',NULL,''),
(1038,'公告删除',107,4,'#','','','',1,0,'F','0','0','system:notice:remove','#','admin','2025-11-27 14:34:19','',NULL,''),
(1039,'操作查询',500,1,'#','','','',1,0,'F','0','0','monitor:operlog:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1040,'操作删除',500,2,'#','','','',1,0,'F','0','0','monitor:operlog:remove','#','admin','2025-11-27 14:34:19','',NULL,''),
(1041,'日志导出',500,3,'#','','','',1,0,'F','0','0','monitor:operlog:export','#','admin','2025-11-27 14:34:19','',NULL,''),
(1042,'登录查询',501,1,'#','','','',1,0,'F','0','0','monitor:logininfor:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1043,'登录删除',501,2,'#','','','',1,0,'F','0','0','monitor:logininfor:remove','#','admin','2025-11-27 14:34:19','',NULL,''),
(1044,'日志导出',501,3,'#','','','',1,0,'F','0','0','monitor:logininfor:export','#','admin','2025-11-27 14:34:19','',NULL,''),
(1045,'账户解锁',501,4,'#','','','',1,0,'F','0','0','monitor:logininfor:unlock','#','admin','2025-11-27 14:34:19','',NULL,''),
(1046,'在线查询',109,1,'#','','','',1,0,'F','0','0','monitor:online:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1047,'批量强退',109,2,'#','','','',1,0,'F','0','0','monitor:online:batchLogout','#','admin','2025-11-27 14:34:19','',NULL,''),
(1048,'单条强退',109,3,'#','','','',1,0,'F','0','0','monitor:online:forceLogout','#','admin','2025-11-27 14:34:19','',NULL,''),
(1049,'任务查询',110,1,'#','','','',1,0,'F','0','0','monitor:job:query','#','admin','2025-11-27 14:34:19','',NULL,''),
(1050,'任务新增',110,2,'#','','','',1,0,'F','0','0','monitor:job:add','#','admin','2025-11-27 14:34:19','',NULL,''),
(1051,'任务修改',110,3,'#','','','',1,0,'F','0','0','monitor:job:edit','#','admin','2025-11-27 14:34:19','',NULL,''),
(1052,'任务删除',110,4,'#','','','',1,0,'F','0','0','monitor:job:remove','#','admin','2025-11-27 14:34:19','',NULL,''),
(1053,'状态修改',110,5,'#','','','',1,0,'F','0','0','monitor:job:changeStatus','#','admin','2025-11-27 14:34:19','',NULL,''),
(1054,'任务导出',110,6,'#','','','',1,0,'F','0','0','monitor:job:export','#','admin','2025-11-27 14:34:19','',NULL,''),
(1055,'生成查询',116,1,'#','','','',1,0,'F','0','0','tool:gen:query','#','admin','2025-11-27 14:34:35','',NULL,''),
(1056,'生成修改',116,2,'#','','','',1,0,'F','0','0','tool:gen:edit','#','admin','2025-11-27 14:34:35','',NULL,''),
(1057,'生成删除',116,3,'#','','','',1,0,'F','0','0','tool:gen:remove','#','admin','2025-11-27 14:34:35','',NULL,''),
(1058,'导入代码',116,4,'#','','','',1,0,'F','0','0','tool:gen:import','#','admin','2025-11-27 14:34:35','',NULL,''),
(1059,'预览代码',116,5,'#','','','',1,0,'F','0','0','tool:gen:preview','#','admin','2025-11-27 14:34:35','',NULL,''),
(1060,'生成代码',116,6,'#','','','',1,0,'F','0','0','tool:gen:code','#','admin','2025-11-27 14:34:35','',NULL,''),
(2074,'文件管理',3,1,'file','system/file/index','','',1,0,'C','0','0','system:file:list','excel','admin','2025-11-27 14:34:46','admin','2026-07-20 21:57:29','文件管理菜单'),
(2075,'文件查询',2074,1,'#','','','',1,0,'F','0','0','system:file:query','#','admin','2025-11-27 14:34:46','',NULL,''),
(2076,'文件新增',2074,2,'#','','','',1,0,'F','0','0','system:file:add','#','admin','2025-11-27 14:34:46','',NULL,''),
(2077,'文件修改',2074,3,'#','','','',1,0,'F','0','0','system:file:edit','#','admin','2025-11-27 14:34:46','',NULL,''),
(2078,'文件删除',2074,4,'#','','','',1,0,'F','0','0','system:file:remove','#','admin','2025-11-27 14:34:46','',NULL,''),
(2079,'文件导出',2074,5,'#','','','',1,0,'F','0','0','system:file:export','#','admin','2025-11-27 14:34:46','',NULL,''),
(900100000001,'标记业务管理',0,8,'mark','','','',1,0,'M','0','0','','guide','admin','2026-07-23 00:24:26','admin','2026-07-23 00:26:49','最小标记业务入口'),
(900100000101,'用户标记',900100000001,2,'userOrder','server/mark/user/index','','MarkUserOrder',1,1,'C','1','0','server:markUser:order:list','list','admin','2026-07-23 00:24:26','',NULL,'用户标记入口'),
(900100000102,'用户端',900100000001,2,'userClient','','','MarkUserClient',1,0,'M','0','0','','user','admin','2026-07-23 00:34:47','',NULL,'用户端9平台目录'),
(900100000103,'代理处理',900100000001,1,'agentOrder','','','MarkAgentOrder',1,0,'M','0','0','','edit','admin','2026-07-23 00:24:26','',NULL,'9平台代理处理目录'),
(900100000104,'接口管理',900100000001,3,'interfaceManagement','server/platformConfig/index','','MarkInterfaceManagement',1,0,'C','0','0','server:platformConfig:list','connection','admin','2026-07-23 03:07:01','admin','2026-07-23 03:07:01','标记查询接口与平台编码绑定管理'),
(900100000105,'平台管理',900100000001,4,'platformManagement','server/mark/platformManagement/index','','MarkPlatformManagement',1,0,'C','0','0','server:markPlatform:list','switch','admin','2026-07-23 15:04:30','',NULL,'?????????'),
(900100000106,'资金明细',900100000108,1,'fundDetails','server/mark/fund/index','','MarkFundDetails',1,0,'C','0','0','server:markUser:wallet:log:list','money','admin','2026-07-23 16:14:51','',NULL,'当前用户标记业务资金明细'),
(900100000107,'资金明细(下级)',900100000108,2,'fundDetailsSubordinate','server/mark/fundSubordinate/index','','MarkFundDetailsSubordinate',1,0,'C','0','0','server:markAgent:wallet:list','money','admin','2026-07-23 16:14:51','',NULL,'代理下级用户标记业务资金明细'),
(900100000108,'资金管理',900100000001,5,'fundManagement','','','MarkFundManagement',1,0,'M','0','0','','money','admin','2026-07-23 16:32:55','',NULL,'标记业务资金管理目录'),
(900100000109,'代理管理',900100000001,6,'agentManagement','','','MarkAgentManagement',1,0,'M','0','0','','peoples','admin','2026-07-23 17:54:19','',NULL,'标记业务代理分级管理目录'),
(900100000110,'一级代理',900100000109,1,'level1','server/mark/agentManagement/index','{\"agentLevel\":\"1\"}','MarkAgentLevel1',1,0,'C','0','0','server:markAgentManagement:list','user','admin','2026-07-23 17:54:19','',NULL,'标记业务代理等级入口'),
(900100000111,'二级代理',900100000109,2,'level2','server/mark/agentManagement/index','{\"agentLevel\":\"2\"}','MarkAgentLevel2',1,0,'C','0','0','server:markAgentManagement:list','user','admin','2026-07-23 17:54:19','',NULL,'标记业务代理等级入口'),
(900100000112,'三级代理',900100000109,3,'level3','server/mark/agentManagement/index','{\"agentLevel\":\"3\"}','MarkAgentLevel3',1,0,'C','0','0','server:markAgentManagement:list','user','admin','2026-07-23 17:54:19','',NULL,'标记业务代理等级入口'),
(900100000113,'自动巡检',900100000001,6,'autoInspection','','','MarkAutoInspection',1,0,'M','0','0','','timer','admin','2026-07-23 20:13:24','',NULL,'待处理订单自动巡检'),
(900100000114,'巡检策略',900100000113,1,'policy','server/mark/inspection/policy','','MarkInspectionPolicy',1,0,'C','0','0','server:markInspection:policy:list','setting','admin','2026-07-23 20:13:24','',NULL,''),
(900100000115,'执行日志',900100000113,2,'log','server/mark/inspection/log','','MarkInspectionLog',1,0,'C','0','0','server:markInspection:log:list','list','admin','2026-07-23 20:13:24','',NULL,''),
(900100000204,'泰迪熊',900100000102,1,'taidixiong','server/mark/user/index','{\"platformCode\":\"taidixiong\",\"platformName\":\"泰迪熊\"}','MarkUserTaidixiong',1,1,'C','0','0','server:markUser:order:list','phone','admin','2026-07-23 00:24:26','admin','2026-07-24 10:15:47','用户9平台入口'),
(900100000205,'腾讯',900100000102,4,'tengxun','server/mark/user/tencent','{\"platformCode\":\"tengxun\",\"platformName\":\"腾讯\"}','MarkUserTengxun',1,1,'C','0','0','server:markUser:order:list','qq','admin','2026-07-23 00:24:26','admin','2026-07-24 10:16:43','用户9平台入口'),
(900100000206,'360',900100000102,5,'360fugai','server/mark/user/qihu360','{\"platformCode\":\"360fugai\",\"platformName\":\"360\"}','MarkUser360',1,1,'C','0','0','server:markUser:order:list','size','admin','2026-07-23 00:24:26','admin','2026-07-24 10:18:27','用户9平台入口'),
(900100000207,'百度',900100000102,6,'baidu','server/mark/user/baidu','{\"platformCode\":\"baidu\",\"platformName\":\"百度\"}','MarkUserBaidu',1,1,'C','0','0','server:markUser:order:list','international','admin','2026-07-23 00:24:26','admin','2026-07-24 10:18:51','用户9平台入口'),
(900100000208,'搜狗',900100000102,7,'sogou','server/mark/user/xiaomi','{\"platformCode\":\"sogou\",\"platformName\":\"搜狗\"}','MarkUserSogou',1,1,'C','0','0','server:markUser:order:list','juhe','admin','2026-07-23 00:24:26','admin','2026-07-24 15:07:45','用户9平台入口'),
(900100000209,'移动高频',900100000102,8,'yidonggaopin','server/mark/user/xiaomi','{\"platformCode\":\"yidonggaopin\",\"platformName\":\"移动高频\"}','MarkUserYidong',1,1,'C','0','0','server:markUser:order:list','list','admin','2026-07-23 00:24:26','',NULL,'用户9平台入口'),
(900100000210,'电话邦',900100000102,9,'dianhuabang','server/mark/user/xiaomi','{\"platformCode\":\"dianhuabang\",\"platformName\":\"电话邦\"}','MarkUserDianhuabang',1,1,'C','0','0','server:markUser:order:list','list','admin','2026-07-23 00:24:26','',NULL,'用户9平台入口'),
(900100000211,'联通管家',900100000102,10,'ltgj','server/mark/user/xiaomi','{\"platformCode\":\"ltgj\",\"platformName\":\"联通管家\"}','MarkUserLtgj',1,1,'C','0','0','server:markUser:order:list','list','admin','2026-07-23 00:24:26','',NULL,'用户9平台入口'),
(900100000212,'小米手机',900100000102,11,'xiaomi','server/mark/user/xiaomi','{\"platformCode\":\"xiaomi\",\"platformName\":\"小米手机\"}','MarkUserXiaomi',1,1,'C','0','0','server:markUser:order:list','star','admin','2026-07-23 00:24:26','admin','2026-07-24 15:08:24','用户9平台入口'),
(900100000213,'TdGaopin高频',900100000102,2,'tdGaopin','server/mark/user/index','{\"platformCode\":\"td_gaopin\",\"platformName\":\"TdGaopin高频\"}','MarkUserTdGaopin',1,1,'C','0','0','server:markUser:order:list','user','admin','2026-07-25 03:04:18','',NULL,'用户端独立平台'),
(900100000214,'Taidixiong二次',900100000102,3,'tdSecond','server/mark/user/tencent','{\"platformCode\":\"td_second\",\"platformName\":\"Taidixiong二次\"}','MarkUserTdSecond',1,1,'C','0','0','server:markUser:order:list','user','admin','2026-07-25 03:04:18','admin','2026-07-25 03:10:50','用户端独立平台'),
(900100000302,'泰迪熊',900100000103,1,'agentTaidixiong','server/mark/agent/process/platform','{\"platformCode\":\"taidixiong\",\"platformName\":\"泰迪熊\"}','MarkAgentTaidixiong',1,0,'C','0','0','server:markAgent:order:query','edit','admin','2026-07-23 00:24:26','',NULL,'代理9平台处理入口'),
(900100000303,'腾讯',900100000103,2,'agentTengxun','server/mark/agent/process/platform','{\"platformCode\":\"tengxun\",\"platformName\":\"腾讯\"}','MarkAgentTengxun',1,0,'C','0','0','server:markAgent:order:query','edit','admin','2026-07-23 00:24:26','',NULL,'代理9平台处理入口'),
(900100000304,'360',900100000103,3,'agent360','server/mark/agent/process/platform','{\"platformCodes\":\"360fugai,qihu_second\",\"platformName\":\"360\"}','MarkAgent360',1,0,'C','0','0','server:markAgent:order:query','edit','admin','2026-07-23 00:24:26','',NULL,'代理9平台处理入口'),
(900100000305,'百度',900100000103,4,'agentBaidu','server/mark/agent/process/platform','{\"platformCode\":\"baidu\",\"platformName\":\"百度\"}','MarkAgentBaidu',1,0,'C','0','0','server:markAgent:order:query','edit','admin','2026-07-23 00:24:26','',NULL,'代理9平台处理入口'),
(900100000306,'搜狗',900100000103,5,'agentSogou','server/mark/agent/process/platform','{\"platformCode\":\"sogou\",\"platformName\":\"搜狗\"}','MarkAgentSogou',1,0,'C','0','0','server:markAgent:order:query','edit','admin','2026-07-23 00:24:26','',NULL,'代理9平台处理入口'),
(900100000307,'移动高频',900100000103,6,'agentYidong','server/mark/agent/process/platform','{\"platformCode\":\"yidonggaopin\",\"platformName\":\"移动高频\"}','MarkAgentYidong',1,0,'C','0','0','server:markAgent:order:query','edit','admin','2026-07-23 00:24:26','',NULL,'代理9平台处理入口'),
(900100000308,'电话邦',900100000103,7,'agentDianhuabang','server/mark/agent/process/platform','{\"platformCode\":\"dianhuabang\",\"platformName\":\"电话邦\"}','MarkAgentDianhuabang',1,0,'C','0','0','server:markAgent:order:query','edit','admin','2026-07-23 00:24:26','',NULL,'代理9平台处理入口'),
(900100000309,'联通管家',900100000103,8,'agentLtgj','server/mark/agent/process/platform','{\"platformCode\":\"ltgj\",\"platformName\":\"联通管家\"}','MarkAgentLtgj',1,0,'C','0','0','server:markAgent:order:query','edit','admin','2026-07-23 00:24:26','',NULL,'代理9平台处理入口'),
(900100000310,'小米手机',900100000103,9,'agentXiaomi','server/mark/agent/process/platform','{\"platformCode\":\"xiaomi\",\"platformName\":\"小米手机\"}','MarkAgentXiaomi',1,0,'C','0','0','server:markAgent:order:query','edit','admin','2026-07-23 00:24:26','',NULL,'代理9平台处理入口'),
(900100000311,'TdGaopin高频',900100000103,2,'agentTdGaopin','server/mark/agent/process/platform','{\"platformCode\":\"td_gaopin\",\"platformName\":\"TdGaopin高频\"}','MarkAgentTdGaopin',1,0,'C','0','0','server:markAgent:order:query','edit','admin','2026-07-23 01:38:46','',NULL,'代理内部平台独立处理入口'),
(900100000312,'Taidixiong二次',900100000103,3,'agentTdSecond','server/mark/agent/process/platform','{\"platformCode\":\"td_second\",\"platformName\":\"Taidixiong二次\"}','MarkAgentTdSecond',1,0,'C','0','0','server:markAgent:order:query','edit','admin','2026-07-23 01:38:46','admin','2026-07-25 02:51:16','代理内部平台独立处理入口'),
(900100000314,'360二次',900100000103,6,'agentQihuSecond','server/mark/agent/process/platform','{\"platformCode\":\"qihu_second\",\"platformName\":\"360二次\"}','MarkAgentQihuSecond',1,0,'C','1','1','server:markAgent:order:query','edit','admin','2026-07-23 01:38:46','',NULL,'代理内部平台独立处理入口'),
(900100001001,'用户查询',900100000101,1,'#','','','',1,0,'F','0','0','server:markUser:order:query','#','admin','2026-07-23 00:24:26','',NULL,''),
(900100001002,'用户新增',900100000101,2,'#','','','',1,0,'F','0','0','server:markUser:order:add','#','admin','2026-07-23 00:24:26','',NULL,''),
(900100001003,'用户预查询',900100000101,3,'#','','','',1,0,'F','0','0','server:markUser:order:precheck','#','admin','2026-07-23 00:24:26','',NULL,''),
(900100001004,'用户提交消除',900100000101,4,'#','','','',1,0,'F','0','0','server:markUser:order:clear','#','admin','2026-07-23 00:24:26','',NULL,''),
(900100001005,'用户钱包',900100000101,5,'#','','','',1,0,'F','0','0','server:markUser:wallet:list','#','admin','2026-07-23 00:24:26','',NULL,''),
(900100001006,'用户流水',900100000101,6,'#','','','',1,0,'F','0','0','server:markUser:wallet:log:list','#','admin','2026-07-23 00:24:26','',NULL,''),
(900100001007,'用户单价',900100000101,7,'#','','','',1,0,'F','0','0','server:markUser:price:list','#','admin','2026-07-23 00:24:26','',NULL,''),
(900100001008,'代理查询',900100000103,8,'#','','','',1,0,'F','0','0','server:markAgent:order:query','#','admin','2026-07-23 00:24:26','',NULL,''),
(900100001009,'代理处理',900100000103,9,'#','','','',1,0,'F','0','0','server:markAgent:item:feedback','#','admin','2026-07-23 00:24:26','',NULL,''),
(900100001010,'接口连接测试',900100000104,6,'#','','','',1,0,'F','0','0','server:platformConfig:test','#','admin','2026-07-23 01:19:11','',NULL,'查询接口连接测试权限'),
(900100001011,'平台新增',900100000105,1,'#','','','',1,0,'F','0','0','server:markPlatform:add','#','admin','2026-07-23 15:04:30','',NULL,''),
(900100001012,'平台编辑',900100000105,2,'#','','','',1,0,'F','0','0','server:markPlatform:edit','#','admin','2026-07-23 15:04:30','',NULL,''),
(900100001013,'平台删除',900100000105,3,'#','','','',1,0,'F','0','0','server:markPlatform:remove','#','admin','2026-07-23 15:04:30','',NULL,''),
(900100001014,'平台开关',900100000105,4,'#','','','',1,0,'F','0','0','server:markPlatform:status','#','admin','2026-07-23 15:04:30','',NULL,''),
(900100001015,'代理新增',900100000109,1,'#','','','',1,0,'F','0','0','server:markAgentManagement:add','#','admin','2026-07-23 18:01:58','',NULL,''),
(900100001016,'代理状态',900100000109,2,'#','','','',1,0,'F','0','0','server:markAgentManagement:edit','#','admin','2026-07-23 18:01:58','',NULL,''),
(900100001017,'初始化密码',900100000109,3,'#','','','',1,0,'F','0','0','server:markAgentManagement:resetPwd','#','admin','2026-07-23 18:01:58','',NULL,''),
(900100001018,'积分分配',900100000109,4,'#','','','',1,0,'F','0','0','server:markAgentManagement:allocate','#','admin','2026-07-23 18:01:58','',NULL,''),
(900100001021,'新增策略',900100000114,1,'#','','','',1,0,'F','0','0','server:markInspection:policy:add','#','admin','2026-07-23 20:13:24','',NULL,''),
(900100001022,'编辑策略',900100000114,2,'#','','','',1,0,'F','0','0','server:markInspection:policy:edit','#','admin','2026-07-23 20:13:24','',NULL,''),
(900100001023,'删除策略',900100000114,3,'#','','','',1,0,'F','0','0','server:markInspection:policy:remove','#','admin','2026-07-23 20:13:24','',NULL,''),
(900100001024,'执行巡检',900100000114,4,'#','','','',1,0,'F','0','0','server:markInspection:policy:run','#','admin','2026-07-23 20:13:24','',NULL,''),
(92281941572000140,'号码查询',0,6,'number',NULL,NULL,'',1,0,'M','0','0',NULL,'component','admin','2026-03-05 14:53:22','admin','2026-03-09 21:51:12',''),
(92281941572000165,'聚合配置',92281941572000140,3,'aggConfig','server/aggConfig/index',NULL,'',1,0,'C','0','0','server:aggConfig:list','juhe','admin','2026-03-09 21:50:04','admin','2026-03-10 23:05:52','聚合配置菜单'),
(92281941572000166,'聚合配置查询',92281941572000165,1,'#','',NULL,'',1,0,'F','0','0','server:aggConfig:query','#','admin','2026-03-09 21:50:04','',NULL,''),
(92281941572000167,'聚合配置新增',92281941572000165,2,'#','',NULL,'',1,0,'F','0','0','server:aggConfig:add','#','admin','2026-03-09 21:50:04','',NULL,''),
(92281941572000168,'聚合配置修改',92281941572000165,3,'#','',NULL,'',1,0,'F','0','0','server:aggConfig:edit','#','admin','2026-03-09 21:50:04','',NULL,''),
(92281941572000169,'聚合配置删除',92281941572000165,4,'#','',NULL,'',1,0,'F','0','0','server:aggConfig:remove','#','admin','2026-03-09 21:50:04','',NULL,''),
(92281941572000170,'聚合配置导出',92281941572000165,5,'#','',NULL,'',1,0,'F','0','0','server:aggConfig:export','#','admin','2026-03-09 21:50:04','',NULL,''),
(92281941572000171,'API调用记录',92281941572000140,5,'apiRecord','server/apiRecord/index',NULL,'',1,0,'C','0','0','server:apiRecord:list','druid','admin','2026-03-09 21:50:09','admin','2026-03-10 23:06:09','接口查询记录通用菜单'),
(92281941572000172,'API查询记录通用查询',92281941572000171,1,'#','',NULL,'',1,0,'F','0','0','server:apiRecord:query','#','admin','2026-03-09 21:50:09','',NULL,''),
(92281941572000173,'API查询记录通用新增',92281941572000171,2,'#','',NULL,'',1,0,'F','0','0','server:apiRecord:add','#','admin','2026-03-09 21:50:09','',NULL,''),
(92281941572000174,'API查询记录通用修改',92281941572000171,3,'#','',NULL,'',1,0,'F','0','0','server:apiRecord:edit','#','admin','2026-03-09 21:50:09','',NULL,''),
(92281941572000175,'API查询记录通用删除',92281941572000171,4,'#','',NULL,'',1,0,'F','0','0','server:apiRecord:remove','#','admin','2026-03-09 21:50:09','',NULL,''),
(92281941572000176,'API查询记录通用导出',92281941572000171,5,'#','',NULL,'',1,0,'F','0','0','server:apiRecord:export','#','admin','2026-03-09 21:50:09','',NULL,''),
(92281941572000177,'平台API配置',92281941572000140,2,'platformConfig','server/numberPlatformConfig/index',NULL,'NumberPlatformConfig',1,0,'C','0','0','server:platformConfig:list','api_manager','admin','2026-03-09 21:50:13','admin','2026-07-23 03:07:01','号码查询平台API配置'),
(92281941572000178,'查询平台url配置查询',92281941572000177,1,'#','',NULL,'',1,0,'F','0','0','server:platformConfig:query','#','admin','2026-03-09 21:50:13','',NULL,''),
(92281941572000179,'查询平台url配置新增',92281941572000177,2,'#','',NULL,'',1,0,'F','0','0','server:platformConfig:add','#','admin','2026-03-09 21:50:13','',NULL,''),
(92281941572000180,'查询平台url配置修改',92281941572000177,3,'#','',NULL,'',1,0,'F','0','0','server:platformConfig:edit','#','admin','2026-03-09 21:50:13','',NULL,''),
(92281941572000181,'查询平台url配置删除',92281941572000177,4,'#','',NULL,'',1,0,'F','0','0','server:platformConfig:remove','#','admin','2026-03-09 21:50:13','',NULL,''),
(92281941572000182,'查询平台url配置导出',92281941572000177,5,'#','',NULL,'',1,0,'F','0','0','server:platformConfig:export','#','admin','2026-03-09 21:50:13','',NULL,''),
(92281941572000183,'用户积分',92281941572000140,6,'point','server/point/index',NULL,'',1,0,'C','1','1','server:point:list','score','admin','2026-03-09 21:50:16','admin','2026-03-10 23:06:19','用户积分关联菜单'),
(92281941572000184,'用户积分关联查询',92281941572000183,1,'#','',NULL,'',1,0,'F','0','0','server:point:query','#','admin','2026-03-09 21:50:16','',NULL,''),
(92281941572000185,'用户积分关联新增',92281941572000183,2,'#','',NULL,'',1,0,'F','0','0','server:point:add','#','admin','2026-03-09 21:50:16','',NULL,''),
(92281941572000186,'用户积分关联修改',92281941572000183,3,'#','',NULL,'',1,0,'F','0','0','server:point:edit','#','admin','2026-03-09 21:50:16','',NULL,''),
(92281941572000187,'用户积分关联删除',92281941572000183,4,'#','',NULL,'',1,0,'F','0','0','server:point:remove','#','admin','2026-03-09 21:50:16','',NULL,''),
(92281941572000188,'用户积分关联导出',92281941572000183,5,'#','',NULL,'',1,0,'F','0','0','server:point:export','#','admin','2026-03-09 21:50:16','',NULL,''),
(92281941572000189,'积分流水',92281941572000140,7,'pointRecord','server/pointRecord/index',NULL,'',1,0,'C','0','0','server:pointRecord:list','score','admin','2026-03-09 21:50:20','admin','2026-03-10 23:06:24','积分流水记录菜单'),
(92281941572000190,'积分流水记录查询',92281941572000189,1,'#','',NULL,'',1,0,'F','0','0','server:pointRecord:query','#','admin','2026-03-09 21:50:20','',NULL,''),
(92281941572000191,'积分流水记录新增',92281941572000189,2,'#','',NULL,'',1,0,'F','0','0','server:pointRecord:add','#','admin','2026-03-09 21:50:20','',NULL,''),
(92281941572000192,'积分流水记录修改',92281941572000189,3,'#','',NULL,'',1,0,'F','0','0','server:pointRecord:edit','#','admin','2026-03-09 21:50:20','',NULL,''),
(92281941572000193,'积分流水记录删除',92281941572000189,4,'#','',NULL,'',1,0,'F','0','0','server:pointRecord:remove','#','admin','2026-03-09 21:50:20','',NULL,''),
(92281941572000194,'积分流水记录导出',92281941572000189,5,'#','',NULL,'',1,0,'F','0','0','server:pointRecord:export','#','admin','2026-03-09 21:50:20','',NULL,''),
(92281941572000195,'模板配置',92281941572000140,4,'template','server/template/index',NULL,'',1,0,'C','0','0','server:template:list','template','admin','2026-03-09 21:50:24','admin','2026-03-10 23:05:57','查询模板定义菜单'),
(92281941572000196,'查询模板定义查询',92281941572000195,1,'#','',NULL,'',1,0,'F','0','0','server:template:query','#','admin','2026-03-09 21:50:24','',NULL,''),
(92281941572000197,'查询模板定义新增',92281941572000195,2,'#','',NULL,'',1,0,'F','0','0','server:template:add','#','admin','2026-03-09 21:50:24','',NULL,''),
(92281941572000198,'查询模板定义修改',92281941572000195,3,'#','',NULL,'',1,0,'F','0','0','server:template:edit','#','admin','2026-03-09 21:50:24','',NULL,''),
(92281941572000199,'查询模板定义删除',92281941572000195,4,'#','',NULL,'',1,0,'F','0','0','server:template:remove','#','admin','2026-03-09 21:50:24','',NULL,''),
(92281941572000200,'查询模板定义导出',92281941572000195,5,'#','',NULL,'',1,0,'F','0','0','server:template:export','#','admin','2026-03-09 21:50:24','',NULL,''),
(92743589188000191,'API调试',92281941572000140,8,'apiTest','server/apiTest/index',NULL,'',1,0,'C','0','0',NULL,'api_manager','admin','2026-03-10 23:07:29','admin','2026-03-10 23:09:00',''),
(93406258420000169,'单次查询(测试)',92281941572000140,10,'userSingle','server/userApi/single',NULL,'',1,0,'C','0','0',NULL,'logininfor','admin','2026-03-18 15:11:58','admin','2026-07-20 21:49:12',''),
(93406316776000182,'批量查询(测试)',92281941572000140,11,'userBatch','server/userApi/batch',NULL,'',1,0,'C','0','0',NULL,'server','admin','2026-03-18 15:12:57','admin','2026-07-20 21:49:17',''),
(93500210590000124,'查询按钮权限',0,99,'',NULL,NULL,'',1,0,'F','0','0','server:apiServer:singleQuery','#','admin','2026-03-19 17:17:51','',NULL,''),
(93822910010000183,'用户功能',0,7,'userModel',NULL,NULL,'',1,0,'M','1','1',NULL,'peoples','admin','2026-03-23 10:56:10','admin','2026-07-20 21:56:32',''),
(93823086376000137,'单次查询',92281941572000140,1,'userSingleNew','server/user/single',NULL,'',1,0,'C','0','0',NULL,'edit','admin','2026-03-23 10:59:06','admin','2026-07-20 21:51:48',''),
(93823137356000167,'批量查询',92281941572000140,2,'userBatchNew','server/user/batch',NULL,'',1,0,'C','0','0',NULL,'drag','admin','2026-03-23 10:59:57','admin','2026-07-20 21:51:56',''),
(94849155875000178,'查询记录',92281941572000140,3,'userRecord','server/user/singleRecord',NULL,'',1,0,'C','0','0',NULL,'documentation','admin','2026-04-04 08:00:16','admin','2026-07-20 21:52:02',''),
(94849186247000160,'批量记录',92281941572000140,4,'batchRecord','server/user/batchRecord',NULL,'',1,0,'C','0','0',NULL,'list','admin','2026-04-04 08:00:46','admin','2026-07-20 21:52:07',''),
(94849186247000161,'免费查询设置',0,8,'freeQuerySettings',NULL,NULL,'',1,0,'M','0','0',NULL,'component','admin','2026-07-20 23:12:44','',NULL,'免费查询页面设置目录'),
(94849186247000162,'定时任务',94849186247000161,1,'schedule','server/freeQuerySettings/schedule/index',NULL,'',1,0,'C','0','0','freeQuery:schedule:list','job','admin','2026-07-20 23:21:14','',NULL,'免费查询定时任务设置'),
(94849186247000163,'数据统计',94849186247000161,3,'statistics','server/freeQuerySettings/statistics/index',NULL,'',1,0,'C','0','0','freeQuery:statistics:list','chart','admin','2026-07-20 23:21:14','',NULL,'免费查询数据统计'),
(94849186247000164,'消息通知',94849186247000161,6,'notification','server/freeQuerySettings/notification/index',NULL,'',1,0,'C','0','0','freeQuery:notification:list','message','admin','2026-07-20 23:21:14','',NULL,'免费查询消息通知设置'),
(94849186247000165,'API接口',94849186247000161,7,'api','server/freeQuerySettings/api/index',NULL,'',1,0,'C','0','0','freeQuery:api:list','api_manager','admin','2026-07-20 23:21:14','',NULL,'免费查询开放API设置'),
(94849186247000166,'定时任务查询',94849186247000162,1,'#','',NULL,'',1,0,'F','0','0','freeQuery:schedule:query','#','admin','2026-07-20 23:36:21','',NULL,''),
(94849186247000167,'定时任务新增',94849186247000162,2,'#','',NULL,'',1,0,'F','0','0','freeQuery:schedule:add','#','admin','2026-07-20 23:36:21','',NULL,''),
(94849186247000168,'定时任务修改',94849186247000162,3,'#','',NULL,'',1,0,'F','0','0','freeQuery:schedule:edit','#','admin','2026-07-20 23:36:21','',NULL,''),
(94849186247000169,'定时任务删除',94849186247000162,4,'#','',NULL,'',1,0,'F','0','0','freeQuery:schedule:remove','#','admin','2026-07-20 23:36:21','',NULL,''),
(94849186247000170,'定时任务启停',94849186247000162,5,'#','',NULL,'',1,0,'F','0','0','freeQuery:schedule:changeStatus','#','admin','2026-07-20 23:36:21','',NULL,''),
(94849186247000171,'免费平台管理',94849186247000161,2,'platform','server/freeQuerySettings/platform/index',NULL,'',1,0,'C','0','0','freeQuery:platform:list','list','admin','2026-07-21 04:14:59','',NULL,'免费查询平台展示与优先级管理'),
(94849186247000172,'免费平台修改',94849186247000171,1,'#','',NULL,'',1,0,'F','0','0','freeQuery:platform:edit','#','admin','2026-07-21 04:14:59','',NULL,''),
(94849186247000173,'免费平台启停',94849186247000171,2,'#','',NULL,'',1,0,'F','0','0','freeQuery:platform:status','#','admin','2026-07-21 04:14:59','',NULL,''),
(94849186247000174,'免费平台排序',94849186247000171,3,'#','',NULL,'',1,0,'F','0','0','freeQuery:platform:sort','#','admin','2026-07-21 04:14:59','',NULL,''),
(94849186247000175,'IP查询记录',94849186247000161,4,'ipRecord','server/freeQuerySettings/ipRecord/index',NULL,'',1,0,'C','0','0','freeQuery:ipRecord:list','log','admin','2026-07-21 04:29:30','',NULL,'免费查询IP审计记录'),
(94849186247000176,'IP查询记录详情',94849186247000175,1,'#','',NULL,'',1,0,'F','0','0','freeQuery:ipRecord:query','#','admin','2026-07-21 04:29:30','',NULL,''),
(94849186247000177,'IP访问策略',94849186247000161,5,'ipPolicy','server/freeQuerySettings/ipPolicy/index',NULL,'',1,0,'C','0','0','freeQuery:ipPolicy:list','lock','admin','2026-07-21 04:38:37','',NULL,'免费查询IP限额与访问频率策略'),
(94849186247000178,'IP访问策略修改',94849186247000177,1,'#','',NULL,'',1,0,'F','0','0','freeQuery:ipPolicy:edit','#','admin','2026-07-21 04:38:37','',NULL,''),
(104500000000000201,'功能',92281941572000140,2,'features','server/user/features',NULL,'',1,0,'C','0','0',NULL,'setting','admin','2026-07-25 02:17:56','admin','2026-07-25 02:17:56','单次查询功能设置');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=DYNAMIC COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES
(1,1),
(1,2),
(1,3),
(1,100),
(1,101),
(1,102),
(1,103),
(1,104),
(1,105),
(1,106),
(1,107),
(1,108),
(1,109),
(1,110),
(1,111),
(1,112),
(1,114),
(1,115),
(1,116),
(1,117),
(1,500),
(1,501),
(1,1000),
(1,1001),
(1,1002),
(1,1003),
(1,1004),
(1,1005),
(1,1006),
(1,1007),
(1,1008),
(1,1009),
(1,1010),
(1,1011),
(1,1012),
(1,1013),
(1,1014),
(1,1015),
(1,1016),
(1,1017),
(1,1018),
(1,1019),
(1,1020),
(1,1021),
(1,1022),
(1,1023),
(1,1024),
(1,1025),
(1,1026),
(1,1027),
(1,1028),
(1,1029),
(1,1030),
(1,1031),
(1,1032),
(1,1033),
(1,1034),
(1,1035),
(1,1036),
(1,1037),
(1,1038),
(1,1039),
(1,1040),
(1,1041),
(1,1042),
(1,1043),
(1,1044),
(1,1045),
(1,1046),
(1,1047),
(1,1048),
(1,1049),
(1,1050),
(1,1051),
(1,1052),
(1,1053),
(1,1054),
(1,1055),
(1,1056),
(1,1057),
(1,1058),
(1,1059),
(1,1060),
(1,2074),
(1,2075),
(1,2076),
(1,2077),
(1,2078),
(1,2079),
(1,900100000001),
(1,900100000101),
(1,900100000102),
(1,900100000103),
(1,900100000104),
(1,900100000105),
(1,900100000106),
(1,900100000107),
(1,900100000108),
(1,900100000109),
(1,900100000110),
(1,900100000111),
(1,900100000112),
(1,900100000113),
(1,900100000114),
(1,900100000115),
(1,900100000204),
(1,900100000205),
(1,900100000206),
(1,900100000207),
(1,900100000208),
(1,900100000209),
(1,900100000210),
(1,900100000211),
(1,900100000212),
(1,900100000213),
(1,900100000214),
(1,900100000302),
(1,900100000303),
(1,900100000304),
(1,900100000305),
(1,900100000306),
(1,900100000307),
(1,900100000308),
(1,900100000309),
(1,900100000310),
(1,900100000311),
(1,900100000312),
(1,900100000314),
(1,900100001001),
(1,900100001002),
(1,900100001003),
(1,900100001004),
(1,900100001005),
(1,900100001006),
(1,900100001007),
(1,900100001008),
(1,900100001009),
(1,900100001010),
(1,900100001011),
(1,900100001012),
(1,900100001013),
(1,900100001014),
(1,900100001015),
(1,900100001016),
(1,900100001017),
(1,900100001018),
(1,900100001021),
(1,900100001022),
(1,900100001023),
(1,900100001024),
(1,92281941572000140),
(1,92281941572000165),
(1,92281941572000166),
(1,92281941572000167),
(1,92281941572000168),
(1,92281941572000169),
(1,92281941572000170),
(1,92281941572000171),
(1,92281941572000172),
(1,92281941572000173),
(1,92281941572000174),
(1,92281941572000175),
(1,92281941572000176),
(1,92281941572000177),
(1,92281941572000178),
(1,92281941572000179),
(1,92281941572000180),
(1,92281941572000181),
(1,92281941572000182),
(1,92281941572000183),
(1,92281941572000184),
(1,92281941572000185),
(1,92281941572000186),
(1,92281941572000187),
(1,92281941572000188),
(1,92281941572000189),
(1,92281941572000190),
(1,92281941572000191),
(1,92281941572000192),
(1,92281941572000193),
(1,92281941572000194),
(1,92281941572000195),
(1,92281941572000196),
(1,92281941572000197),
(1,92281941572000198),
(1,92281941572000199),
(1,92281941572000200),
(1,92743589188000191),
(1,93406258420000169),
(1,93406316776000182),
(1,93500210590000124),
(1,93822910010000183),
(1,93823086376000137),
(1,93823137356000167),
(1,94849155875000178),
(1,94849186247000160),
(2,900100000001),
(2,900100000102),
(2,900100000106),
(2,900100000108),
(2,900100000204),
(2,900100000205),
(2,900100000206),
(2,900100000207),
(2,900100000208),
(2,900100000209),
(2,900100000210),
(2,900100000211),
(2,900100000212),
(2,900100000213),
(2,900100000214),
(2,92281941572000140),
(2,93823086376000137),
(3,900100000001),
(3,900100000101),
(3,900100000102),
(3,900100000106),
(3,900100000108),
(3,900100000204),
(3,900100000205),
(3,900100000206),
(3,900100000207),
(3,900100000208),
(3,900100000209),
(3,900100000210),
(3,900100000211),
(3,900100000212),
(3,900100000213),
(3,900100000214),
(3,900100001001),
(3,900100001002),
(3,900100001003),
(3,900100001004),
(3,900100001005),
(3,900100001006),
(3,900100001007),
(3,92281941572000140),
(3,93823086376000137),
(4,900100000001),
(4,900100000103),
(4,900100000106),
(4,900100000107),
(4,900100000108),
(4,900100000302),
(4,900100000303),
(4,900100000304),
(4,900100000305),
(4,900100000306),
(4,900100000307),
(4,900100000308),
(4,900100000309),
(4,900100000310),
(4,900100000311),
(4,900100000312),
(4,900100000314),
(4,900100001008),
(4,900100001009);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mark_user_platform_quota`
--

DROP TABLE IF EXISTS `mark_user_platform_quota`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mark_user_platform_quota` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `platform_code` varchar(64) NOT NULL,
  `platform_name` varchar(128) DEFAULT NULL,
  `remain_count` bigint(20) NOT NULL DEFAULT 0,
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mark_user_platform_quota` (`user_id`,`platform_code`),
  KEY `idx_mark_user_platform_quota_platform_code` (`platform_code`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户平台余量';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mark_user_platform_quota`
--

LOCK TABLES `mark_user_platform_quota` WRITE;
/*!40000 ALTER TABLE `mark_user_platform_quota` DISABLE KEYS */;
INSERT INTO `mark_user_platform_quota` VALUES
(1,104144371718000167,'baidu','Baidu百度',998,'admin','2026-07-23 00:52:50','110125','2026-07-24 01:55:59',NULL),
(2,104144371718000167,'taidixiong','TdGaopin高频',994,'admin','2026-07-24 00:02:28','110125','2026-07-24 10:26:15',NULL),
(3,104144371718000167,'tengxun','腾讯速解',999,'admin','2026-07-24 01:47:53','tencent-auto','2026-07-24 01:49:00',NULL),
(4,104144371718000167,'360fugai','360Fugai覆盖',995,'admin','2026-07-24 01:47:55','110125','2026-07-25 21:27:57',NULL),
(5,104144371718000167,'sogou','Sougou',999,'admin','2026-07-24 01:48:00','admin','2026-07-24 01:48:00',NULL),
(6,104144371718000167,'yidonggaopin','YidongGaopin移动',999,'admin','2026-07-24 01:48:03','admin','2026-07-24 01:48:03',NULL),
(7,104144371718000167,'dianhuabang','Dianhuabang',999,'admin','2026-07-24 01:48:05','admin','2026-07-24 01:48:05',NULL),
(8,104144371718000167,'xiaomi','Xiaomi小米',997,'admin','2026-07-24 01:48:07','110125','2026-07-24 15:04:02',NULL),
(9,104144371718000167,'ltgj','LiantongGuanjia',999,'admin','2026-07-24 04:27:24','admin','2026-07-24 04:27:24',NULL),
(10,104144371718000167,'td_second','Taidixiong二次',100,'admin','2026-07-25 03:24:14','admin','2026-07-25 03:24:14',NULL),
(11,104144371718000167,'td_gaopin','TdGaopin高频',100,'admin','2026-07-25 03:24:19','admin','2026-07-25 03:24:19',NULL);
/*!40000 ALTER TABLE `mark_user_platform_quota` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mark_wallet_log`
--

DROP TABLE IF EXISTS `mark_wallet_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mark_wallet_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `order_id` bigint(20) DEFAULT NULL,
  `order_item_id` bigint(20) DEFAULT NULL,
  `platform_code` varchar(64) DEFAULT NULL,
  `platform_name` varchar(128) DEFAULT NULL,
  `biz_type` varchar(32) NOT NULL,
  `change_amount` bigint(20) NOT NULL,
  `balance_before` bigint(20) NOT NULL,
  `balance_after` bigint(20) NOT NULL,
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_mark_wallet_log_user_id` (`user_id`),
  KEY `idx_mark_wallet_log_order_id` (`order_id`),
  KEY `idx_mark_wallet_log_order_item_id` (`order_item_id`),
  KEY `idx_mark_wallet_log_biz_type` (`biz_type`),
  KEY `idx_mark_wallet_log_create_time` (`create_time`),
  KEY `idx_mark_wallet_log_platform_code` (`platform_code`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='标记钱包流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mark_wallet_log`
--

LOCK TABLES `mark_wallet_log` WRITE;
/*!40000 ALTER TABLE `mark_wallet_log` DISABLE KEYS */;
INSERT INTO `mark_wallet_log` VALUES
(1,104144371718000167,NULL,NULL,'baidu','??','ADJUST',1,0,1,'admin','2026-07-23 00:52:50','账户充值'),
(2,104144371718000167,NULL,NULL,'baidu','??','ADJUST',-1,1,0,'admin','2026-07-23 00:52:50','账户扣减'),
(3,104144371718000167,NULL,NULL,'baidu','??','ADJUST',1,0,1,'admin','2026-07-23 00:53:21','账户充值'),
(4,104144371718000167,NULL,NULL,'baidu','??','ADJUST',-1,1,0,'admin','2026-07-23 00:53:21','账户扣减'),
(5,104144371718000167,NULL,NULL,'taidixiong','TdGaopin高频','ADJUST',1000,0,1000,'admin','2026-07-24 00:02:28','账户充值'),
(6,104144371718000167,1,NULL,'taidixiong','TdGaopin高频','DEDUCT',-1,1000,999,'110125','2026-07-24 00:26:43','用户下单扣费'),
(7,104144371718000167,NULL,NULL,'tengxun','Tengxun腾讯','ADJUST',999,0,999,'admin','2026-07-24 01:47:53','账户充值'),
(8,104144371718000167,NULL,NULL,'360fugai','360Fugai覆盖','ADJUST',999,0,999,'admin','2026-07-24 01:47:55','账户充值'),
(9,104144371718000167,NULL,NULL,'baidu','Baidu百度','ADJUST',999,0,999,'admin','2026-07-24 01:47:58','账户充值'),
(10,104144371718000167,NULL,NULL,'sogou','Sougou','ADJUST',999,0,999,'admin','2026-07-24 01:48:00','账户充值'),
(11,104144371718000167,NULL,NULL,'yidonggaopin','YidongGaopin移动','ADJUST',999,0,999,'admin','2026-07-24 01:48:03','账户充值'),
(12,104144371718000167,NULL,NULL,'dianhuabang','Dianhuabang','ADJUST',999,0,999,'admin','2026-07-24 01:48:05','账户充值'),
(13,104144371718000167,NULL,NULL,'xiaomi','Xiaomi小米','ADJUST',999,0,999,'admin','2026-07-24 01:48:07','账户充值'),
(14,104144371718000167,2,2,'tengxun','腾讯速解','DEDUCT',-1,999,998,'110125','2026-07-24 01:48:27','腾讯号码提交扣费'),
(15,104144371718000167,2,2,'tengxun','腾讯速解','REFUND',1,998,999,'tencent-auto','2026-07-24 01:49:00','腾讯后台提交失败退回'),
(16,104144371718000167,3,NULL,'360fugai','360Fugai覆盖','DEDUCT',-1,999,998,'110125','2026-07-24 01:55:52','用户下单扣费'),
(17,104144371718000167,4,NULL,'baidu','Baidu百度','DEDUCT',-1,999,998,'110125','2026-07-24 01:55:59','用户下单扣费'),
(18,104144371718000167,5,NULL,'360fugai','360Fugai覆盖','DEDUCT',-1,998,997,'110125','2026-07-24 02:44:46','用户下单扣费'),
(19,104144371718000167,6,NULL,'360fugai','360Fugai覆盖','DEDUCT',-1,997,996,'110125','2026-07-24 02:47:20','用户下单扣费'),
(20,104144371718000167,6,6,'360fugai','360Fugai覆盖','REFUND',1,996,997,'admin','2026-07-24 02:51:56','明细失败自动退款'),
(21,104144371718000167,7,NULL,'360fugai','360Fugai覆盖','DEDUCT',-1,997,996,'110125','2026-07-24 02:57:32','用户下单扣费'),
(22,104144371718000167,8,NULL,'taidixiong','TdGaopin高频','DEDUCT',-3,999,996,'110125','2026-07-24 03:09:42','用户下单扣费'),
(23,104144371718000167,8,8,'taidixiong','TdGaopin高频','REFUND',1,996,997,'admin','2026-07-24 03:33:49','明细失败自动退款'),
(24,104144371718000167,8,10,'taidixiong','TdGaopin高频','REFUND',1,997,998,'admin','2026-07-24 03:33:49','明细失败自动退款'),
(25,104144371718000167,8,9,'taidixiong','TdGaopin高频','REFUND',1,998,999,'admin','2026-07-24 03:33:49','明细失败自动退款'),
(26,104144371718000167,1,1,'taidixiong','TdGaopin高频','REFUND',1,999,1000,'admin','2026-07-24 03:33:49','明细失败自动退款'),
(27,104144371718000167,8,10,'taidixiong','TdGaopin高频','DEDUCT',-1,1000,999,'admin','2026-07-24 03:33:54','代理修改状态成功重新扣费'),
(28,104144371718000167,8,9,'taidixiong','TdGaopin高频','DEDUCT',-1,999,998,'admin','2026-07-24 03:33:54','代理修改状态成功重新扣费'),
(29,104144371718000167,1,1,'taidixiong','TdGaopin高频','DEDUCT',-1,998,997,'admin','2026-07-24 03:33:54','代理修改状态成功重新扣费'),
(30,104144371718000167,8,8,'taidixiong','TdGaopin高频','DEDUCT',-1,997,996,'admin','2026-07-24 03:33:54','代理修改状态成功重新扣费'),
(31,104144371718000167,NULL,NULL,'ltgj','LiantongGuanjia','ADJUST',999,0,999,'admin','2026-07-24 04:27:24','账户充值'),
(32,104144371718000167,9,NULL,'taidixiong','TdGaopin高频','DEDUCT',-1,996,995,'110125','2026-07-24 10:13:19','用户下单扣费'),
(33,104144371718000167,10,NULL,'taidixiong','TdGaopin高频','DEDUCT',-1,995,994,'110125','2026-07-24 10:26:15','用户下单扣费'),
(34,104144371718000167,11,NULL,'xiaomi','Xiaomi小米','DEDUCT',-1,999,998,'110125','2026-07-24 14:21:17','用户下单扣费'),
(35,104144371718000167,12,NULL,'xiaomi','Xiaomi小米','DEDUCT',-1,998,997,'110125','2026-07-24 15:04:02','用户下单扣费'),
(36,104144371718000167,NULL,NULL,'td_second','Taidixiong二次','ADJUST',100,0,100,'admin','2026-07-25 03:24:14','账户充值'),
(37,104144371718000167,NULL,NULL,'td_gaopin','TdGaopin高频','ADJUST',100,0,100,'admin','2026-07-25 03:24:19','账户充值'),
(38,104144371718000167,13,NULL,'360fugai','360Fugai覆盖','DEDUCT',-1,996,995,'110125','2026-07-25 21:27:57','用户下单扣费');
/*!40000 ALTER TABLE `mark_wallet_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mark_agent_point_transaction`
--

DROP TABLE IF EXISTS `mark_agent_point_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mark_agent_point_transaction` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `transaction_no` varchar(64) NOT NULL,
  `request_no` varchar(64) NOT NULL,
  `transaction_type` varchar(32) NOT NULL,
  `platform_code` varchar(64) NOT NULL,
  `platform_name` varchar(128) DEFAULT NULL,
  `amount` bigint(20) NOT NULL,
  `from_user_id` bigint(20) DEFAULT NULL,
  `to_user_id` bigint(20) NOT NULL,
  `operator_user_id` bigint(20) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `status` varchar(16) NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mark_agent_point_request` (`request_no`),
  UNIQUE KEY `uk_mark_agent_point_transaction` (`transaction_no`),
  KEY `idx_mark_agent_point_users` (`from_user_id`,`to_user_id`),
  KEY `idx_mark_agent_point_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代理积分充值转账交易';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mark_agent_point_transaction`
--

LOCK TABLES `mark_agent_point_transaction` WRITE;
/*!40000 ALTER TABLE `mark_agent_point_transaction` DISABLE KEYS */;
/*!40000 ALTER TABLE `mark_agent_point_transaction` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-07-26  0:37:22
