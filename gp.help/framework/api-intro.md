# API服务的设计及实现机制

GP框架是在light-4j基础上扩展的服务开发框架，因此最终需要通过API实现对外的服务处理。熟悉spring开发
那么一定知道DisptatchServlet的作用，它接收全部请求，依据路径和method对请求进行路由分发。GP框架采用
类似的设计实现API请求分发。

在light-4j中，提供了一类处理器即：MiddlewareHandler,它可以对全部的请求进行过滤处理，但是同DispatchServlet
不同，它更多承担过滤器的作用。在底层undertow中有RoutingHandler专门负责请求的路由分发工作。

在GP框架中，主要解决以下问题：
1. 框架中认证登录等相关代码的统一注册处理
2. 请求鉴权处理主要放在FilterHandler中，它是基于MiddlewareHandler开发的过滤器
3. 应用级业务API服务的检查和注册

## 框架中认证登录等相关代码的统一注册处理

![](./api-intro-auth.png)

登录相关处理有如下场景：

* 路径：/opapi/authorize

1. 账户+密码方式

    参数：
       
        {
            grant_type: passowrd,
            client_id: xxx,
            client_secret: yyy,
            username: xxxx,
            password: xxx,
            device: zzz
        }

    返回值：

        {
            access_token: xxxx,
            token_type: xxx,
            expire_in: zzz,
            refresh_token: mmm
        }

2. 授权码方式

   参数：

        {
            grant_type: authorization_code,
            client_id: xxx,
            client_secret: yyy,
            code: xxxx,
            redirect_uri: xxx,
            csrf: zzz
        }

   返回值：

        {
            access_token: xxxx,
            token_type: xxx,
            expire_in: zzz,
            refresh_token: mmm
        }

2. 客户端认证

   参数：

        {
            grant_type: client_credentials,
            client_id: xxx,
            client_secret: yyy,
            scope: xxxx
        }

   返回值：

        {
            access_token: xxxx,
            token_type: xxx,
            expire_in: zzz,
        }

* /opapi/ping 检查请求处理

* /opapi/sign-off 退出处理

其他相关的认证处理，稍后补充。目前框架也支持定制的认证处理逻辑

## 请求鉴权处理

GP框架提供默认两类请求控制：/opapi 和 /gpapi,两个路径分别对应开放API和业务API。
请求鉴权处理通过FilterHandler实现，该过滤器会检查请求header中的Authorization值是否通过校验来验证
是否为合法请求，将请求交由RouterHandler继续处理。

## GP框架中API服务的检查和处理

