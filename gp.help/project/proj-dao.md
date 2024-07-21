# 如何生成DAO



## DAO相关的代码建议放入com/gp/dao中

![](./dao-path.png)

* dao/ext中保存ExtendDAO类
* dao/info中保存Bean基本封装类
* dao中保存全部DAO操作类

## DAO工具

![](dao-tools.png)

可以通过工具完成DAO代码的自动生成，经过少量调整即可

## 启动DAO工具

    // 执行命令
    gp.tools/run-gen.sh