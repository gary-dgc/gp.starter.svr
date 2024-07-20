# BEAN数据读写机制的设计及实现机制

在Web开发中Map对象和Bean之间的数据读写、复制是非常高频的开发操作。比如MapUtils可以支持
从Map中读取数据的同时进行类型转化，这确实可以提供好的开发体验，但是不可否认绝大多数的工具类
都是基于java类的反射机制进行的读写操作，用来屏蔽看似冗余的代码（于性能而言并非冗余），也可能
有人会说高性能的CPU完全可以忽略这样的性能损耗，对此看法不在讨论范围。

为了最大程度提供Bean对象的读写性能，GP框架采用另一种思路来保证bean属性的读写。

为每个java对象类创建一个代理的对象，该对象支持如下接口

``` 
public interface BeanProxy {
    /** 读取对象属性 */
    Object getProperty(Object bean, String property);
    /** 写入对象属性 */
    void setProperty(Object bean, String property, Object value);
}
```

上述接口的属性名称可以支持camelCase和snakeCase两种，这也提供了操作的灵活性。在此基础上为
每个bean类提供一个代理工具对象，以hardcode方式进行读写操作而不是反射机制实现读写。

现在面对一个问题，代理的原理很简单，但是为每个Bean构建一个代理工具对象也是难以接收的做法，
对此问题是通过ASM的字节码开发实现的。具体的处理机制如下：

1. 对输入的Bean类进行成员属性遍历，获得属性的名称和类型
2. 根据变量名称，确定相应的cameCase和snakeCase名称
3. 利用ASM的ClassVisitor功能，利用属性名称动态构建Beanproxy实现类。
4. 通过classLoader对新类的字节码进行加载并实例化。
5. 处理结束

上述过程对于使用者是无感的，在需要进行Map转化或属性读写时可以按如下操作

```
    public static void copyToMap(Object src, Map<String, Object> prop, Predicate<String> predicate) {
    	
    	BeanMeta srcDef = BeanAccessor.getBeanMeta(src.getClass());
    	// 获取Bean的代理工具对象
    	BeanProxy srcProxy = srcDef.getBeanProxy();
    	for(String field : srcDef.getSnakeProperties()) {
    		    		
	        if(predicate != null && !predicate.test(field)) {
		        continue;
	        }
    	
            Object value = srcProxy.getProperty(src, field);
    		
    	    prop.put(field, value);
			
    	}
    }

```