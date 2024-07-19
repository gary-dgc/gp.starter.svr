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

