# 简介
Spring框架源码拥有约108万行代码，如果要把所有的代码都看一遍，是需要花费大量时间和精力，而且很容易跟进一个方法绕进去，所以我们需要抓住Spring源码主干源码和Spring源码对各种设计模式的运用，以及怎么有条不紊的整合各种框架实现可扩展，各种框架是怎么无缝衔接的织入Spring框架的，比如Spring整合mybatis、nacos是在哪里织入Spring的等等。

ps：idea 插件 Statistic 可以统计框架有多少行代码
# 本章主要内容
1. Spring源码的整体脉络梳理
2. 什么是BeanFactory
3. BeanFactory和ApplicationContext的区别
4. 图诉SpringIoc的加载过程

> 先将类加载成Bean定义，加载Bean定义有一个步骤，首先读取到我们的配置类，通过我们的配置类，扫描到配置了@Component等注解的类，然后注册成Bean定义，ApplicationContext可以调用BeanFactoryPostProcessor修改Bean定义，调用BeanDefinitionRegistryPostProcessor注册成Bean定义，通过BeanFactory调用getBean()，然后会实例化，填充属性（解析@Autowired、@Value）,初始化调用各种Aware，调用initMethod方法，生产过程中会有很多扩展点，最终放在一个Map里面
5. 图诉Bean的生命周期
6. 图诉Spring中的扩展接口

# 前置知识
- Spring框架的使用
- java编程基础、反射、设计模式、动态代理等

# 学习资料
- 设计模式：https://mp.weixin.qq.com/s/12fLnRxKCYABSItkKvU8Fw
# 概念
IOC 控制反转：用来解决层层之间的耦合

Spring是一个IOC的容器，容器里面管理的是各种Bean，IOC控制反转，是一种设计思想，DI是它的实现。

# 脉络

1. 我们知道IOC管理了很多Bean，那么怎么将Bean注入IOC，而成为一个Bean?或者我们怎么将一个类怎么变成一个Bean交给IOC管理?

Spring应用的时候一般会
- 第一步：配置我们的类，不管是xml或者@注解、javaconfig方式配置。
- 第二步：加载Spring上下文，一般2种方式。xml：new ClassPathXmlApplicationContext("xml")  注解：new AnnotationConfigApplicationContext(Config.class)
- 第三步：getBean()

通过以上应该我们只知道将类注入到IOC容器中

2. 当我们的一个类变成一个Bean的最核心的一个类是什么？

> 是BeanFactory

BeanFactory是Spring顶层核心接口，使用了简单工厂模式（可参考前面提供的设计模式简单学习一下），负责生产bean。


以下面简单demo为了例查看：
```
@Configuration
@ComponentScan
public class MainApp {
	public static void main(String[] args) {
		ApplicationContext context=new AnnotationConfigApplicationContext(MainApp.class);
		UserServiceImpl bean = context.getBean("userServiceImpl",UserServiceImpl.class);
		bean.sayHi();


}
```

getBean()方法

```
@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType);
	}
```
Spring上下文（AnnotationConfigApplicationContext）不生产bean，通知`getBeanFactory()`去生产Bean，`getBean()`方法即可以获取Bean，也可以生产Bean，这个方法有2个作用。


查看`BeanFactory`源码，以idea为例，双击`shift`,搜索`BeanFactory`找到该类，快捷键 `Ctrl + F12`查看该类所有的方法。

<img src="https://note.youdao.com/yws/api/personal/file/3575962939AA42A6A2A060B61E5D2055?method=download&shareKey=4c960aba9c185b572f288eced0ec750e" width = "600" />

由一个工程类传入不同类型的参数，动态决定应该创建哪一个产品类

3. 生产获取Bean的工厂有了，那么我怎么读取到Bean呢？
我们有可能是xml配置的，有可能是注解配置的，这样就需要一个 统一的抽象的 读取后存放至一个接口类里面，那个这个统一的接口是什么呢？

BeanFactory只负责生产获取Bean，再负责读取就有些不太符合我们单一指责原则。

所以这个统一的接口就是`BeanDefinition`，Spring顶层核心接口，封装了生产Bean的一切原材料。具体哪些原材料，请看下面提供的信息。


BeanDefinition接口的核心之类AbstractBeanDefinition

<img src="https://processon.com/chart_image/5f79449de401fd06fd76ff5e.png" width = "600" />

或者访问：http://processon.com/chart_image/5f79449de401fd06fd76ff5e.png

---


下面以一个生活的例子描述下这个几个的关系：

比如我们买新房子要装修，比如需要定制一个衣柜和橱柜
- 第一步：需要找到一个定制衣柜的`衣柜店`，定制`衣柜`。衣柜店不会生产定制衣柜，而是通知一个`工厂`来生产定制衣柜
- 第二步：衣柜店会有一个`设计师`，按照你的要求设计出一个`图纸`，比如：会有`颜色款式`等的要求，衣柜店负责人通知工厂按照图纸制作一个衣柜。
- 第三步：工厂按照图纸要求制作衣柜，制作好后，送到衣柜店，等待`你`来取走。


如果再定制橱柜，跟上面步骤差不多

- 衣柜店的角色就是 `ClassPathXmlApplicationContext`
- 橱柜店的角色就是`AnnotationConfigApplicationContext`
- 而你就是 一个带有 `@Component`的class
- 工厂就是`BeanFactory`
- 颜色和款式就是`@Lazy或者@Scope`等
- 衣柜就是`Bean`
- 图纸就是`BeanDefinition`
- 设计师就是`BeanDefinitionRegistry`

衣柜店肯定不止一个设计师，也有`销售推广`等，推广人员去市场寻找在`潜在客户`，比如推广人员在杭州市西湖区某个小区发传单推广，小区一共有1000个，1000个人就是`潜在客户`推广人员挨家挨户的介绍产品，但是最终只有10个人买了衣柜,这10个客户就是`真实客户`。

- 推广人员这个角色就是`BeanDefinitionReader`
- 市场就是`xml`和`配置类`，这里可以看做上面例子的杭州市西湖区某小区。比如:配置类配置了`org.spring.study`这个包，这个包下有1000个类，而只有10个类配置了`@Component`、`@Service`等注解
- 潜在客户就是`BeanDefinitionSacnner`
- 真实客户就是`bean`


4. BeanFactory和Application有什么区别？

类结构图：
 <img src="https://note.youdao.com/yws/api/personal/file/C86769A26A9243108F400A8BBBF987F9?method=download&shareKey=b062445f1cd45d39a464c9abe475f58d" width = "600" />


共同：都有生产bean的能力

区别：  

Feature | BeanFactory| ApplicationContext
---|---|---
Bean Bean实例化/装配 | Yes| Yes
集成的生命周期管理 |No|Yes
自动注册 BeanPostProcessor |No| Yes
自动注册 BeanFactoryPostProcessor |No| Yes
便利的 MessageSource 访问 (国际化) | No| Yes
内置ApplicationEvent 发布机制 | No| Yes

Spring中文文档：
https://github.com/DocsHome/spring-docs/blob/master/pages/core/overview.md

6. Bean的生产过程并不是一步到位的，有哪些步骤呢？涉及到Bean的生命周期，有哪些周期？

- 第一步：实例化，刚才可以看到我们`BeanDefinition`里面有参数`beanClass`，直接通过`反射`就可以拿到`bean`
- 第二步：填充属性，解析@Autowired、@Value等等
- 第三步：初始化，可配置一些`initMethodName`,`destroyMethodName`等
- 第四步：bean基本已成成形，会`put`到一个`Map`里面，那么这个`Map`的`key`就是beanname，`value`存的就是bean的实例，而这个`Map`就是我们大名鼎鼎的`单例池`也是`一级缓存`
- 第五步：getBean()的时候，就是直接在`Map`里面获取实例

在初始化的是时候回回调大量的XXXAware接口
- BeanNameAware
- BeanClassLoaderAware
- BeanFactoryAware
- EnvironmentAware
- EmbeddedValueResolverAware
- ResourceLoaderAware
- ApplicationEventPublisherAware
- MessageSourceAware
- ApplicationContextAware
- ServletContextAware


7. Spring实例化的方式有哪些？
- 反射：@Component等的方式，在实例化的过程，是由Spring控制的
- 工厂：@Bean标记的方法是工厂方法，工厂可以通过new()来自己控制，比较灵活

8. 扩展点（敲黑板，看重点）

因为这个2个扩展节点知识比较多，非常非常非常重要，重要的时说三遍，另写文章，先眼熟一下类名。

在注册Bean定义的时候，还可以修改，比如设计师在设计衣柜的时候，可以修好图纸，只要还没有执行getBean()方法，就好像图纸给了工厂了，但是工厂因为效益好，还没有来的急做，这时候客户突然说，我想换个颜色等，然后设计师改好后，就从新发给工厂

Spring提供扩展接口，让我可以对`BeanDefinition`(bean定义)进行扩展，这个接口就是`BeanFactoryPostProcessor`

除了修改还可以注册`BeanDefinitionRegistryPostProcessor`

Spring除了IOC的实现不用扩展点，剩下的很多功能都是基于扩展点集成的。这就是Spring生态可以做的非常好的原因。

除了Bean定义有扩展点以外，Bean的生命周期也有扩展点`BeanPostPorcessor`（Bean的后置处理器），在Bean的生命周期中一共会调用9次Bean的后置处理器。

9. 整体脉络图


 <img src="https://note.youdao.com/yws/api/personal/file/C631D3C84A9D4B11BCA35391E5E6C199?method=download&shareKey=ffb3da8f4bd29b8b75432a3b0fdf07db" width = "600" />
 
在线查看：
https://www.processon.com/view/link/5f606b33e0b34d080d49f0a2

    









