# Groupress框架演示项目

## 简介

Web应用开发从最早的Servlet开发到现在微服务开发，应用项目的代码结构到技术构成已经发生了巨大的变化。
在目前应用开发中前-后分离已经成为十分普遍，实际工作中Spring-boot以其无可争议的普及程度，无疑成为
此中翘楚。随着基于spring-boot开发逐渐深入，由于长期关注与产品级系统开发，渐渐思考是否有必要构建一
个轻量化的框架，用以保证产品的持续性，而不必疲惫的跟着spring-boot去不断升级和重构，这也成为我开发
groupress这个所谓的“轮子”的初衷。

Spring框架经过多年的发展，它的优点无须赘述，至于缺点见仁见智，在此不做评述。在构思框架的过程中，发
现了[light-4j](https://github.com/networknt/light-4j)，于是决定基于light-4j构建groupress框架。

*** 

## 框架目标及特点

* 性能优先、稳定性优先
* 轻量化优先，避免冗余
* 迭代持续性优先
* 透明化代码构成

***

- [ ] [Groupress框架介绍](./gp.help/framework.md)
  
  * 框架基本结构（三方依赖说明）
    * 开发包整体结构及说明
    * 开发包依赖关系
    * 其他说明
  * Bean声明及自动绑定设计及实现机制
  * Bean数据复制的设计及实现机制
  * 数据事物的处理及实现机制
  * DAO的设计及实现
    * BaseDAO介绍
    * ExtendDAO介绍
  * Service数据服务的设计及实现
    * 基础服务的设计实现
    * LinkerService设计服务实现
    * ActionService设计服务实现
  * API服务的设计及实现机制 
  * 接口服务校验的设计及实现
  * 异步事件的处理机制及设计实现
  * 应用启动器介绍
    * AppLauncher介绍（Daemon下的远程命令模式）
    * Light4j的启动过程介绍
  
- [ ] [启动项目结构](./gp.help/project.md)

  * 应用依赖说明及模块命名规范
  * 应用环境说明
  * 应用启动过程说明
  * 如何生成DAO
  * 如何生成服务
  * 如何实现业务设计
  * 如何实现接口服务
  * 事件的捕捉和处理

- [ ] [启动项目部署及发布](./gp.help/deploy.md)

To make it easy for you to get started with GitLab, here's a list of recommended next steps.

Already a pro? Just edit this README.md and make it your own. Want to make it easy? [Use the template at the bottom](#editing-this-readme)!

## Add your files

- [ ] [Create](https://docs.gitlab.com/ee/user/project/repository/web_editor.html#create-a-file) or [upload](https://docs.gitlab.com/ee/user/project/repository/web_editor.html#upload-a-file) files
- [ ] [Add files using the command line](https://docs.gitlab.com/ee/gitlab-basics/add-file.html#add-a-file-using-the-command-line) or push an existing Git repository with the following command:

```
cd existing_repo
git remote add origin https://gitlab.com/g4497/gp-starter-svr.git
git branch -M main
git push -uf origin main
```
