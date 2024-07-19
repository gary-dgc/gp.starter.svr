# 其他开发详细说明

## gp-netty

该包封装了一个简版WebMiniServer和一个Webdav服务器

重要开发包说明：

    /com.gp
    |--/netty WebMiniServer实现代码
    |--/webdav Webdav服务器实现代码（基于netty实现）

## gp-jgroups

该包封装了模块级应用间消息同步代码，基于jgroups实现

重要开发包说明：

    /com.gp
    |--/common 通用数据格式定义
    |--/jgrp jgroup收发处理实现代码
    |--/msg 消息封装代码

## gp-sync-base

该包封装了应用间远程主-从同步实现代码

重要开发包说明：

    /com.gp
    |--/client 同步客户端实现代码
    |--/event 事件消息处理代码
    |--/message 消息封装代码
    |--/oper 消息处理实现基本代码
    |--/trans 文件转收实现代码


