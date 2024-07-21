# 数据库部署和初始化

1. 将数据库初始化数据导入Mysql实例

2. 调整gp.app.api中的数据库访问设置 /src/main/filters/env-dev.properties

    
    database setting
    DATABASE_USE=root
    DATABASE_PWD=12345

*tips*

可以通过AesCryptor对密码进行加密处理