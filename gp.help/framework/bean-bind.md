# Bean的绑定处理机制及实现

> [Spring is bloated and it becomes too heavy](https://doc.networknt.com/architecture/spring-is-bloated/#spring-is-bloated-and-it-becomes-too-heavy)
> 
> When Spring was out, it was only a small core with IoC contain and it was fast and easy to use. Now, I cannot even count 
> how many Spring Components available today. In order to complete with JEE, Spring basically implemented all replacements 
> of JEE and these are heavy components.

如果进行过Springboot开发那么一定熟悉spring的对象绑定机制。在structs的时代，开发人员基本都是不停的手动创建对象，调用操作，JVM自动garbage进行回收。
spring普及后，对象绑定无疑改变了开发者在对象使用时的构建方式。但是我们也应该看到spring在某种程度上的放纵了开发者的随意性，典型的就是通过lazyload等方式
来避免交叉依赖等问题。本质上这应该是开发的设计问题，有很多的设计模式可以针对不同场景进行代码实现，在spring的环境中，开发者已经懒得在代码规划上进行多一点
的思考。

light-4j也提供了单实例对象的注册机制，仅限于顺序的通过默认构造函数进行对象初始化。实话实说这个特性有些过于鸡肋。

为了能够保留spring环境中的开发体验，但是又不想让绑定变得过于复杂，于是尝试构建一个简化版的Bean绑定机制。主要实现以下两个开发场景：

1. 支持通过构建顺序控制，可以让java对象类按指定顺序进行对象创建，并完成相应的内部变量绑定
2. 支持对class进行绑定变量的检查，按深度优先方式进行对象创建，并完成对象绑定

上述场景中不考虑注解过程中的反向依赖和交叉依赖问题。仅关注解决简单场景的依赖绑定。为了实现绑定定义两个注解：

* BindComponent - 类注解标记该类可以注册为Bean，该注解支持key, type, priority三个设置

* BindAutowired - Field注解标记该成员变量可以绑定一个对象，该注解支持key, type两个设置

## 1 顺序构建场景说明

对于场景1按如下场景进行处理：

1. 指定需要执行绑定的包路径，并扫描该包下的全部Bean类，识别出全部标记BindComponent的Bean类
2. 对Bean类依据priority进行排序，越小，优先级越高
3. 按排序后的Bean列表，对每个Bean进行初始化（通过默认构造函数创建）
   1. 排除Interface和Abstract类
   2. 排除没有默认构造函数类
   3. 创建对象并对对象的全部成员变量进行遍历
      1. 过滤标记BindAutowired的成员变量
      2. 对成员变量按key和type从缓存中查找对象，并进行绑定
   4. 将对象放入缓存
4. 处理结束

上述处理适用于主程序启动后的一次性初始化绑定过程，该过程清晰简单，易于排查问题。

## 2 类对象绑定场景说明

对于场景2按如下场景进行处理：

1. 通过ASM的ClassVisitor机制对类继续依赖分析，解析该类所以依赖的全部类对象。
   1. 检测是否存在循环依赖
   2. 检测是否存在交叉依赖
2. 通过遍历后得到的依赖树，按深度优先遍历，并进行绑定或构建
   1. 从缓存中查找对象，存在则执行绑定
   2. 如果未找到对象，那么进行bean对象构建并缓存，继续执行绑定
3. 处理结束，返回结果

上述场景适用于在场景1完成后的，随机性绑定需求。这个场景不要求根对象类必须有BindComponent注解，只要具有默认构造能力的类都可以通过
这个场景对内部的绑定变量进行装配绑定。

实际上上述两个场景可以满足绝大部分的实际工作需要，如果有特殊的场景需要通过设计模式来进行处理更好，至少可以确保代码结构的简单化。