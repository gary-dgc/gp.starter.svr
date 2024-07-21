# ************************************************************
# Sequel Pro SQL dump
# Version 5446
#
# https://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 5.7.44)
# Database: gpress_starter
# Generation Time: 2024-07-21 08:00:03 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table gp_audit
# ------------------------------------------------------------

DROP TABLE IF EXISTS `gp_audit`;

CREATE TABLE `gp_audit` (
  `audit_id` bigint(20) NOT NULL COMMENT 'audit snowflake id',
  `instance_id` bigint(20) NOT NULL COMMENT 'audit snowflake id',
  `client` varchar(128) DEFAULT NULL COMMENT 'client device id',
  `host` varchar(32) DEFAULT NULL COMMENT 'client host ip address',
  `app` varchar(32) DEFAULT NULL COMMENT 'application abbr which launch request',
  `path` varchar(255) DEFAULT NULL COMMENT 'the api path',
  `version` varchar(32) DEFAULT NULL COMMENT 'version of application',
  `device` varchar(255) DEFAULT NULL COMMENT 'the device information',
  `subject` varchar(16) NOT NULL COMMENT 'principal subject',
  `operation` varchar(32) NOT NULL COMMENT 'request operation',
  `object_id` varchar(48) DEFAULT NULL COMMENT 'operation target object',
  `predicates` varchar(2048) DEFAULT NULL COMMENT 'the predicates json string',
  `state` varchar(10) DEFAULT NULL COMMENT 'state of request',
  `message` longtext COMMENT 'message of request',
  `audit_time` datetime DEFAULT NULL COMMENT 'audit time',
  `elapsed_time` int(11) DEFAULT NULL COMMENT 'fulfill request time expense in millisecond',
  `modifier_uid` bigint(11) DEFAULT NULL,
  `modify_time` datetime DEFAULT NULL,
  `del_flag` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`audit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
