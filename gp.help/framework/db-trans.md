# 数据库事务的处理及实现机制

groupress框架采用jdbi和hikariCP构建数据库的读写控制层，其中Jdbi提供了数据库的事物控制管理。

熟悉spring开发那么一定对@Transactional注解的使用非常了解。注解的使用确实为开发提供了极大的
便捷性，但是灵活性也带来了新的问题，估计项目经理对开发中的不定时出现的@Transactional一定深恶痛绝。
我见过在Service类上和DAO类上同时存在@Transactional的情况，也是让人无语（关于此话题另作讨论）。

对上述情况，GP框架尝试解决以下几个问题：

1. 对事务的起点进行收敛控制，避免事务不可控的嵌入情况。
2. 提供事务注解使用便捷性，实现类似spring的注解方式开启事务

## 事务注解的实现

在详细说GP框架的处理事务机制之前，先介绍下jdbi是如何进行事务控制的。

```
    // jdbi中Handle的事务启动方法
    public <R, X extends Exception> R inTransaction(TransactionIsolationLevel level, HandleCallback<R, X> callback) throws X {
        ......
    }
    
    // 实际调用的代码
    handle.inTransaction(TransactionIsolationLevel.NONE, (x)->{
        // select xxx from yyy where zzz
        // update xxx set a=b where zzz
        // insert xxx into ttt
        // delete from xxx
    })
```

从上述代码可知，jdbi是利用了lambda函数进行的事务受控代码的封装，实现了对执行过程的事务控制。
受此启发，GP框架采用如下机制实现对事务注解的实现：

1. 在应用启动时在JVM中注册ClassFileTransformer，该transformer用于对事务注解@JdbiTran进行处理
2. 类加载后利用ASM的ClassVisitor对内部全部方法遍历，查找标记@JdbiTran注解的方法
3. 遍历需要进行事务控制的方法，并对其进行转化
   1. 对原方法进行改名处理
   2. 构建一个新方法，方法中通过lambda方法实现对原方法的调用
   ```
    // 转化前原方法
    @JdbiTran
    public void demo1(){
       ....
    }
   
    // 转化后新方法
    public void demo1(){
       handle.inTransaction(TransactionIsolationLevel.NONE, (x)->{
         demo1_inner();
       })
    }
    
    public void demo1_inner(){
       ....
    }
   
   ```
   
4. 处理完成后，返回新的类字节码，重新完成类加载

## 事务起点的收敛控制

为解决开发中的事务收敛控制，GP框架通过接口标记和@JdbiTran注解配合使用，只有实现了
TranService接口的类中标记的@JdbiTran注解的方法才会进行事务方法转化，其他情况忽略处理。
这种方式在一定程度上避免了@JdbiTran的随意使用，同时也为类Transformer提供了匹配条件，
保证Transformer对应用性能无影响。

## 总结

上述做法中相当于对类的方法行为进行改变，或许有人提出这种做法侵入性很强，其实在稳定性和可靠性
不打折的情况下，入侵性是个伪命题。