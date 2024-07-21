# ************************************************************
# Sequel Pro SQL dump
# Version 5446
#
# https://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 5.7.44)
# Database: gpress_starter
# Generation Time: 2024-07-21 08:44:47 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table gp_sys_option
# ------------------------------------------------------------

DROP TABLE IF EXISTS `gp_sys_option`;

CREATE TABLE `gp_sys_option` (
  `sys_opt_id` bigint(16) NOT NULL,
  `opt_group` varchar(48) DEFAULT NULL COMMENT 'the option group',
  `opt_key` varchar(48) NOT NULL COMMENT 'the option key',
  `opt_value` varchar(512) DEFAULT NULL COMMENT 'the option value',
  `description` varchar(128) DEFAULT NULL COMMENT 'description',
  `modifier_uid` bigint(11) DEFAULT NULL,
  `modify_time` datetime DEFAULT NULL,
  `del_flag` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`sys_opt_id`),
  KEY `opt_key_idx` (`opt_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

LOCK TABLES `gp_sys_option` WRITE;
/*!40000 ALTER TABLE `gp_sys_option` DISABLE KEYS */;

INSERT INTO `gp_sys_option` (`sys_opt_id`, `opt_group`, `opt_key`, `opt_value`, `description`, `modifier_uid`, `modify_time`, `del_flag`)
VALUES
	(11,'BASIC','query.max.rows','20000','最大查询记录数量',1,'2018-08-30 11:48:21',NULL),
	(12,'BASIC','system.option.ttl','300','系统参数缓存时间',1,'2018-08-30 11:48:21',0),
	(13,'SECURITY','security.hash.salt','demosalt','加密salt内容',1,'2018-08-30 11:48:21',NULL),
	(15,'BASIC','audit.enable','true','审计开关',1,'2018-08-30 11:48:21',0),
	(16,'BASIC','node.app.key','appkey1','node app key',2,'2019-06-09 19:54:49',NULL),
	(17,'BASIC','node.app.secret','1','node app secret',2,'2019-06-09 19:54:46',NULL),
	(18,'BASIC','system.version','1.0','系统版本',1,'2018-08-30 11:48:21',NULL),
	(19,'BASIC','system.app','gpress.web','应用名称',1,'2018-08-30 11:48:21',NULL),
	(20,'BASIC','valid.msg.resources','classpath:messages','消息文件路径',1,'2018-08-30 11:48:21',NULL),
	(21,'BASIC','cabinet.version.enable','true','默认是否打开版本控制',1,'2018-08-30 11:48:21',NULL),
	(22,'SECURITY','security.jwt.secret','!@#$FDD#$sd#','JWT加密密码',1,'2018-08-30 11:48:21',NULL),
	(36,'NETWORK','file.access','http://192.168.192.57:8082','文件访问地址',1,'2020-04-04 19:31:03',0),
	(38,'SECURITY','symmetric.crypto.iv','IeeJ#}pr6%nA3ydE','对称加密向量',1,'2019-04-04 15:17:44',NULL);

/*!40000 ALTER TABLE `gp_sys_option` ENABLE KEYS */;
UNLOCK TABLES;



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
