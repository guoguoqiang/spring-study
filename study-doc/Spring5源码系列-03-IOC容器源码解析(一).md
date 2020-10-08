

# 介绍

Spring 容器类的加载成一个Bean，其中大概分成2大步，第一步就是类解析成一个BeanDefinition（Bean定义），第二步就是将BeanDefinition创建成一的Bean，经过实例化、属性赋值、初始化最终的一个Bean。先大致有个概念。这次主要是第一步

# 知识笔记
- BeanFactory和FactoryBean的区别
> BeanFactory是Spring顶层核心接口，使用了简单工程模式，负责生产Bean；FactoryBean专门用来修饰普通Bean，getBean的时候获取的是getObject()所获取到的实例，是一个扩展点。


# 前置知识

- Spring框架的使用
- java编程基础、反射、设计模式、动态代理等

# 源码解析
Spring 最重要的概念是 IOC 和 AOP，其中IOC又是Spring中的根基：
<img src="https://note.youdao.com/yws/api/personal/file/6026819842004229AF898D26DE52F9EC?method=download&shareKey=a0ee494da0926583b13ac0f51240b44d" width = "600" />


加载Spring容器最常用的2中方式
- AnnotationConfigApplicationContext 
- ClassPathXmlApplicationContext

本文主要以`AnnotationConfigApplicationContext`方式解析，这种设计理念更先进

前置条件：
- 已经按照前面的步骤自己下载编译了Spring源码
- 能自己写一个模块运行 hello world

示例demo：
```
public class Car {

	private String name;

	public Car(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
```

```
@Configuration
@ComponentScan
public class MainApp {

	@Bean
	public Car car(){
		return new Car("五菱");
	}
	public static void main(String[] args) {
		ApplicationContext context=new AnnotationConfigApplicationContext(MainApp.class);
		Car car = context.getBean("car", Car.class);
		System.out.println(car.getName());
	}
}
```
AnnotationConfigApplicationContext的结构关系：
<img src="https://note.youdao.com/yws/api/personal/file/806A57564F18472B91B8AAE5867301ED?method=download&shareKey=8b8991cb135e245bd782e7a23dcf9509" width = "800" />



## 1. 进入构造方法`AnnotationConfigApplicationContext()`


```
	//	根据参数类型可以知道，其实可以传入多个annotatedClasses，但是这种情况出现的比较少
	public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
		/**
		 * 调用无参构造函数，会先调用父类GenericApplicationContext的构造函数
		 * 父类的构造函数里面就是初始化DefaultListableBeanFactory，并且赋值给beanFactory
		 * 本类的构造函数里面，初始化了一个读取器：AnnotatedBeanDefinitionReader read，一个扫描器ClassPathBeanDefinitionScanner scanner
		 * scanner的用处不是很大，它仅仅是在我们外部手动调用 .scan 等方法才有用，常规方式是不会用到scanner对象的
		 */
		this();
		/**
		 * 把传入的类进行注册，这里有两个情况，
		 * 传入传统的配置类
		 * 传入bean（虽然一般没有人会这么做）
		 * 看到后面会知道spring把传统的带上@Configuration的配置类称之为FULL配置类，不带@Configuration的称之为Lite配置类
		 * 但是我们这里先把带上@Configuration的配置类称之为传统配置类，不带的称之为普通bean
		 */
		register(componentClasses);
		/**
		 * IOC容器刷新接口
		 */
		refresh();
	}
```
## 2. 调用父类GenericApplicationContext 构造方法

```
public GenericApplicationContext() {
		/**
		 * 调用父类的构造函数,为ApplicationContext spring上下文对象初始beanFactory
		 * 为啥是DefaultListableBeanFactory？我们去看BeanFactory接口的时候
		 * 发DefaultListableBeanFactory是最底层的实现，功能是最全的
		 */
		this.beanFactory = new DefaultListableBeanFactory();
	}
```
DefaultListableBeanFactory的关系图：
<img src="https://note.youdao.com/yws/api/personal/file/FACF89DE33E74910ADD4225838CFDC09?method=download&shareKey=6d6e294158251d62aab831c817d8e779" width = "800" />

可以看出 `DefaultListableBeanFactory`实现了`BeanFactory`和 `BeanDefinitionRegistry`接口
> DefaultListableBeanFactory是相当重要的，从字面意思就可以看出它是一个Bean的工厂，什么是Bean的工厂？当然就是用来生产和获得Bean的。

> DefaultListableBeanFactory就是我们所说的容器了，里面放着beanDefinitionMap，beanDefinitionNames，beanDefinitionMap是一个hashMap，beanName作为Key,beanDefinition作为Value，beanDefinitionNames是一个集合，里面存放了beanName

## 3. this()方法

```
//	会隐式调用父类的构造方法，初始化DefaultListableBeanFactory
	public AnnotationConfigApplicationContext() {

		/**
		 *
		 * 创建一个读取注解的Bean定义读取器
		 * 什么是bean定义？BeanDefinition
		 *
		 * 完成了spring内部BeanDefinition的注册（主要是后置处理器）
		 */
		this.reader = new AnnotatedBeanDefinitionReader(this);
		/**
		 * 创建BeanDefinition扫描器
		 * 可以用来扫描包或者类，继而转换为bd
		 *
		 * spring默认的扫描包不是这个scanner对象
		 * 而是自己new的一个ClassPathBeanDefinitionScanner
		 * spring在执行工程后置处理器ConfigurationClassPostProcessor时，去扫描包时会new一个ClassPathBeanDefinitionScanner
		 *
		 * 这里的scanner仅仅是为了程序员可以手动调用AnnotationConfigApplicationContext对象的scan方法
		 *
		 */
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}
```
实例化了一个`AnnotatedBeanDefinitionReader`和`ClassPathBeanDefinitionScanner`

重点查看AnnotatedBeanDefinitionReader

```
public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		Assert.notNull(environment, "Environment must not be null");
		//把ApplicationContext对象赋值给AnnotatedBeanDefinitionReader
		this.registry = registry;
		//用户处理条件注解 @Conditional os.name
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
		//注册一些内置的后置处理器
		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
	}
```
进入AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
> 其主要做了2件事情
> 1.注册内置BeanPostProcessor
> 2.注册相关的BeanDefinition


```
public static Set<BeanDefinitionHolder> registerAnnotationConfigProcessors(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		DefaultListableBeanFactory beanFactory = unwrapDefaultListableBeanFactory(registry);
		if (beanFactory != null) {
			if (!(beanFactory.getDependencyComparator() instanceof AnnotationAwareOrderComparator)) {
				//注册了实现Order接口的排序器
				beanFactory.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
			}
			//设置@AutoWired的候选的解析器：ContextAnnotationAutowireCandidateResolver
			// getLazyResolutionProxyIfNecessary方法，它也是唯一实现。
			//如果字段上带有@Lazy注解，表示进行懒加载 Spring不会立即创建注入属性的实例，而是生成代理对象，来代替实例
			if (!(beanFactory.getAutowireCandidateResolver() instanceof ContextAnnotationAutowireCandidateResolver)) {
				beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
			}
		}

		Set<BeanDefinitionHolder> beanDefs = new LinkedHashSet<>(8);

		/**
		 * 为我们容器中注册了解析我们配置类的后置处理器ConfigurationClassPostProcessor
		 * 名字叫:org.springframework.context.annotation.internalConfigurationAnnotationProcessor
		 */
		if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		/**
		 * 为我们容器中注册了处理@Autowired 注解的处理器AutowiredAnnotationBeanPostProcessor
		 * 名字叫:org.springframework.context.annotation.internalAutowiredAnnotationProcessor
		 */
		if (!registry.containsBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		/**
		 * 为我们容器注册处理JSR规范的注解处理器CommonAnnotationBeanPostProcessor
		 * org.springframework.context.annotation.internalCommonAnnotationProcessor
		 */
		// Check for JSR-250 support, and if present add the CommonAnnotationBeanPostProcessor.
		if (jsr250Present && !registry.containsBeanDefinition(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		/**
		 * 处理jpa注解的处理器org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor
		 */
		// Check for JPA support, and if present add the PersistenceAnnotationBeanPostProcessor.
		if (jpaPresent && !registry.containsBeanDefinition(PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition();
			try {
				def.setBeanClass(ClassUtils.forName(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME,
						AnnotationConfigUtils.class.getClassLoader()));
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(
						"Cannot load optional framework class: " + PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, ex);
			}
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		/**
		 * 处理监听方法的注解@EventListener解析器EventListenerMethodProcessor
		 */
		if (!registry.containsBeanDefinition(EVENT_LISTENER_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(EventListenerMethodProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_PROCESSOR_BEAN_NAME));
		}

		/**
		 * 注册事件监听器工厂
		 */
		if (!registry.containsBeanDefinition(EVENT_LISTENER_FACTORY_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(DefaultEventListenerFactory.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_FACTORY_BEAN_NAME));
		}

		return beanDefs;
	}
```
这里会一连串注册好几个Bean
<img src="https://note.youdao.com/yws/api/personal/file/11226AA357234466A94C702CFF5BA24B?method=download&shareKey=8c8033950cb9a3a0a690a3717075aab8" width = "800" />

在这其中最重要的一个Bean `ConfigurationClassPostProcessor`



关系 结构图
<img src="https://note.youdao.com/yws/api/personal/file/8BAC71DD5AA54E99A18986A94B05D8E4?method=download&shareKey=5ae9af9b9b62da142037891095e61f05" width = "800" />

实现了`BeanDefinitionRegistryPostProcessor`和`BeanFactoryPostProcessor`接口


```
if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
            def.setSource(source);
            beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
}
```
1. 判断容器中是否已经存在了ConfigurationClassPostProcessor Bean
1. 如果不存在（当然这里肯定是不存在的），就通过RootBeanDefinition的构造方法获得ConfigurationClassPostProcessor的BeanDefinition，RootBeanDefinition是BeanDefinition的子类
1. 执行registerPostProcessor方法，registerPostProcessor方法内部就是注册Bean，当然这里注册其他Bean也是一样的流程。

### BeanDefinition是什么？
BeanDefinition联系图
向上
<img src="https://note.youdao.com/yws/api/personal/file/5F0304733CB44732A5A3480A2811C50A?method=download&shareKey=75fcbb9ffad10d9b59a4d7a5b39dd811" width = "600" />

- BeanMetadataElement接口：BeanDefinition元数据，返回该Bean的来源
- AttributeAccessor接口：提供对BeanDefinition属性操作能力

向下
<img src="https://note.youdao.com/yws/api/personal/file/415136AB49C5430AB928A0CE92D0364C?method=download&shareKey=626de1e98a116e5d5da1691e7a2f5d05" width = "800" />

> 它是用来描述Bean的，里面存放着关于Bean的一系列信息，比如Bean的作用域，Bean所对应的Class，是否懒加载，是否Primary等等，这个BeanDefinition也相当重要，我们以后会常常和它打交道

registerPostProcessor()方法跟进

```
private static BeanDefinitionHolder registerPostProcessor(
			BeanDefinitionRegistry registry, RootBeanDefinition definition, String beanName) {

		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(beanName, definition);
		return new BeanDefinitionHolder(definition, beanName);
	}
```
<img src="https://note.youdao.com/yws/api/personal/file/73D9CBCA313C44EFA56338049FD5590F?method=download&shareKey=148985d52ea32c3eab297251d059db5f" width = "800" />


进入org.springframework.context.support.GenericApplicationContext#registerBeanDefinition的方法

进入
org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition

```
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");

		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
				((AbstractBeanDefinition) beanDefinition).validate();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Validation of bean definition failed", ex);
			}
		}

		BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
		if (existingDefinition != null) {
			// 是不是设置了不允许 相同名称Bean定义  （默认允许存在相同的）
			if (!isAllowBeanDefinitionOverriding()) {
				throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
			}
			// 权限大的优先存在，就不会被覆盖
			else if (existingDefinition.getRole() < beanDefinition.getRole()) {
				// e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
				if (logger.isInfoEnabled()) {
					logger.info("Overriding user-defined bean definition for bean '" + beanName +
							"' with a framework-generated bean definition: replacing [" +
							existingDefinition + "] with [" + beanDefinition + "]");
				}
			}
			else if (!beanDefinition.equals(existingDefinition)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Overriding bean definition for bean '" + beanName +
							"' with a different definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Overriding bean definition for bean '" + beanName +
							"' with an equivalent definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			// 覆盖
			this.beanDefinitionMap.put(beanName, beanDefinition);
		}
		else {
			if (hasBeanCreationStarted()) {
				// Cannot modify startup-time collection elements anymore (for stable iteration)
				synchronized (this.beanDefinitionMap) {
					this.beanDefinitionMap.put(beanName, beanDefinition);
					List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
					updatedDefinitions.addAll(this.beanDefinitionNames);
					updatedDefinitions.add(beanName);
					this.beanDefinitionNames = updatedDefinitions;
					removeManualSingletonName(beanName);
				}
			}
			else {
				// Still in startup registration phase
				this.beanDefinitionMap.put(beanName, beanDefinition);
				this.beanDefinitionNames.add(beanName);
				removeManualSingletonName(beanName);
			}
			this.frozenBeanDefinitionNames = null;
		}

		if (existingDefinition != null || containsSingleton(beanName)) {
			resetBeanDefinition(beanName);
		}
		else if (isConfigurationFrozen()) {
			clearByTypeCache();
		}
	}
```
第一次进来肯定为空
BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName)

<img src="https://note.youdao.com/yws/api/personal/file/755CFC0ADFE946FDA7320CCC5D9A6DD4?method=download&shareKey=178d0af9fb2f0b263567f1e221373a18" width = "800" />



存入map

```
private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
```

```
this.beanDefinitionMap.put(beanName, beanDefinition);
this.beanDefinitionNames.add(beanName);
```


key：org.springframework.context.annotation.internalConfigurationAnnotationProcessor

value：beanDefinition

> DefaultListableBeanFactory中的beanDefinitionMap，beanDefinitionNames也是相当重要的，以后会经常看到它，最好看到它，第一时间就可以反应出它里面放了什么数据
> 这里仅仅是注册，可以简单的理解为把一些原料放入工厂，工厂还没有真正的去生产。

实例化完AnnotatedBeanDefinitionReader 可以看出 beanDefinitionMap创建了 5个 beanDefinition 
<img src="https://note.youdao.com/yws/api/personal/file/A4F125697B904FD5BEC47E5159CC0787?method=download&shareKey=7d268a330310493e163d30ce4027dfc1" width = "800" />

因为Spring正在的ClassPathBeanDefinitionScanner并不是使用this()方法初始化的这个扫描器，进入下一步

## 4. register(componentClasses)方法

关键方法是doRegisterBean方法
org.springframework.context.annotation.AnnotatedBeanDefinitionReader#doRegisterBean

> 这方法的主要作用是把我们的 配置类，也是ApplicationContext context=new AnnotationConfigApplicationContext(MainApp.class);
> 传进来的 MainApp.class，注册（就是put到map里面，注册听起来高大上一点）容器中

```
private <T> void doRegisterBean(Class<T> beanClass, @Nullable String name,
			@Nullable Class<? extends Annotation>[] qualifiers, @Nullable Supplier<T> supplier,
			@Nullable BeanDefinitionCustomizer[] customizers) {
		//AnnotatedGenericBeanDefinition可以理解为一种数据结构，是用来描述Bean的，这里的作用就是把传入的标记了注解的类 存储@Configuration注解注释的类
		//转为AnnotatedGenericBeanDefinition数据结构，里面有一个getMetadata方法，可以拿到类上的注解
		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
		//判断是否需要跳过注解，spring中有一个@Condition注解，当不满足条件，这个bean就不会被解析
		if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
			return;
		}

		abd.setInstanceSupplier(supplier);
		//解析bean的作用域，如果没有设置的话，默认为单例
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
		abd.setScope(scopeMetadata.getScopeName());
		//获得beanName
		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
		//解析通用注解，填充到AnnotatedGenericBeanDefinition，解析的注解为Lazy，Primary，DependsOn，Role，Description
		AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
		if (qualifiers != null) {
			for (Class<? extends Annotation> qualifier : qualifiers) {
				if (Primary.class == qualifier) {
					abd.setPrimary(true);
				}
				else if (Lazy.class == qualifier) {
					abd.setLazyInit(true);
				}
				else {
					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
				}
			}
		}
		if (customizers != null) {
			for (BeanDefinitionCustomizer customizer : customizers) {
				customizer.customize(abd);
			}
		}

		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
		//注册，最终会调用DefaultListableBeanFactory中的registerBeanDefinition方法去注册，
		//DefaultListableBeanFactory维护着一系列信息，比如beanDefinitionNames，beanDefinitionMap
		//beanDefinitionNames是一个List<String>,用来保存beanName
		//beanDefinitionMap是一个Map,用来保存beanName和beanDefinition
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
	}
```
在这里又要说明下，以常规方式去注册配置类，此方法中除了第一个参数，其他参数都是默认值。
1. 通过AnnotatedGenericBeanDefinition的构造方法，获得配置类的BeanDefinition，这里是不是似曾相似，在注册ConfigurationClassPostProcessor类的时候，也是通过构造方法去获得BeanDefinition的，只不过当时是通过RootBeanDefinition去获得，现在是通过AnnotatedGenericBeanDefinition去获得。
1. 判断需不需要跳过注册，Spring中有一个@Condition注解，如果不满足条件，就会跳过这个类的注册。
1. 然后是解析作用域，如果没有设置的话，默认为单例。
1. 获得BeanName。
1. 解析通用注解，填充到AnnotatedGenericBeanDefinition，解析的注解为Lazy，Primary，DependsOn，Role，Description。
1. 限定符处理，不是特指@Qualifier注解，也有可能是Primary，或者是Lazy，或者是其他（理论上是任何注解，这里没有判断注解的有效性）。
1. 把AnnotatedGenericBeanDefinition数据结构和beanName封装到一个对象中（这个不是很重要，可以简单的理解为方便传参）。
1. 注册，最终会调用DefaultListableBeanFactory中的registerBeanDefinition方法去

进入BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry)方法

```
public static void registerBeanDefinition(
			BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		// Register bean definition under primary name.
		String beanName = definitionHolder.getBeanName();
		registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

		// Register aliases for bean name, if any.
		String[] aliases = definitionHolder.getAliases();
		if (aliases != null) {
			for (String alias : aliases) {
				registry.registerAlias(beanName, alias);
			}
		}
	}
```

又看到了熟悉的代码`registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition())`

就是注册到容器中

<img src="https://note.youdao.com/yws/api/personal/file/7988CADB9064431CBED03FD382DC885C?method=download&shareKey=dfa3faf231ec69f997696e97ce99ee7a" width = "800" />

## 5. refresh()方法
这是非常核心的一个方法，里面一共12大步，调用链非常深，分批次解析


```
public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			//1:刷新预处理，和主流程关系不大，就是保存了容器的启动时间，启动标志等
			prepareRefresh();

			//2:获取告诉子类初始化Bean工厂  不同工厂不同实现
			// DefaultListableBeanFactory实现了ConfigurableListableBeanFactory
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			//3:对bean工厂进行填充属性
			prepareBeanFactory(beanFactory);

			try {
				// 4:留个子类去实现该接口
				postProcessBeanFactory(beanFactory);

				// 5:调用我们的bean工厂的后置处理器. 1. 会在此将class扫描成beanDefinition  2.bean工厂的后置处理器调用
				invokeBeanFactoryPostProcessors(beanFactory);

				// 6:注册我们bean的后置处理器
				registerBeanPostProcessors(beanFactory);

				// 7:初始化国际化资源处理器.
				initMessageSource();

				// 8:创建事件多播器
				initApplicationEventMulticaster();

				// 9:这个方法同样也是留个子类实现的springboot也是从这个方法进行启动tomcat的.
				onRefresh();

				// 10:把我们的事件监听器注册到多播器上
				registerListeners();

				// 11:实例化我们剩余的单实例bean.
				finishBeanFactoryInitialization(beanFactory);

				// 12:最后容器刷新 发布刷新事件(Spring cloud也是从这里启动的)
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				destroyBeans();

				// Reset 'active' flag.
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				resetCommonCaches();
			}
		}
	}
```


目标是分析前五个小方法：

### 1. prepareRefresh()

刷新预处理，和主流程关系不大，就是保存了容器的启动时间，启动标志等。
### 2.  ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory()

这个方法和主流程关系也不是很大，可以简单的认为，就是把beanFactory取出来而已。XML模式下会在这里读取BeanDefinition

### 3. prepareBeanFactory()

对bean工厂进行填充属性

```
protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		//设置bean工厂的类加载器为当前application应用的加载器
		beanFactory.setBeanClassLoader(getClassLoader());
		//为bean工厂设置我们标准的SPEL表达式解析器对象StandardBeanExpressionResolver
		beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		//为我们的bean工厂设置了一个propertityEditor 属性资源编辑器对象(用于后面的给bean对象赋值使用)
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		//注册了一个完整的ApplicationContextAwareProcessor 后置处理器用来处理ApplicationContextAware接口的回调方法
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		/**
		 *
		 * 忽略以下接口的bean的 接口函数方法。 在populateBean时
		 * 因为以下接口都有setXXX方法， 这些方法不特殊处理将会自动注入容器中的bean
		 */
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

		/**
		 * 当注册了依赖解析后，例如当注册了对 BeanFactory.class 的解析依赖后，
		 * 当 bean 的属性注 入的时候， 一旦检测到属性为 BeanFactory 类型便会将 beanFactory 的实例注入进去。
		 * 知道为什么可以
		 * @Autowired
		 * ApplicationContext  applicationContext  就是因为这里设置了
		 */
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		//注册了一个事件监听器探测器后置处理器接口
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		// 处理aspectj的
		if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// Set a temporary ClassLoader for type matching.
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		//注册了bean工厂的内部的bean
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			//环境
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			//环境系统属性
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			//系统环境
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
	}
```

主要做了如下的操作：
1. 设置了一个类加载器
1. 设置了bean表达式解析器
1. 添加了属性编辑器的支持
1. 添加了一个后置处理器：ApplicationContextAwareProcessor，此后置处理器实现了BeanPostProcessor接口
1. 设置了一些忽略自动装配的接口
1. 设置了一些允许自动装配的接口，并且进行了赋值操作
1. 在容器中还没有XX的bean的时候，帮我们注册beanName为XX的singleton bean

### 4. postProcessBeanFactory(beanFactory)

这是一个空方法

### 5. invokeBeanFactoryPostProcessors(beanFactory)

**这是一个非常重要的方法：**

- 处理所有实现了BeanDefinitionRegistryPostProcessor处理器（带注册功能的后置处理器）
- 处理所有实现了BeanFactoryPostProcessor处理器



调用我们的bean工厂的后置处理器. 1. 会在此将class扫描成beanDefinition  2.bean工厂的后置处理器调用


```
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		//  获取两处存储BeanFactoryPostProcessor的对象 传入供接下来的调用
		//  1.当前Bean工厂，2.和我们自己调用addBeanFactoryPostProcessor的自定义BeanFactoryPostProcessor
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
		// (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
		if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}
```

让我们看看第一个小方法的第二个参数：


```
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return this.beanFactoryPostProcessors;
	}
```
> 这里获得的是BeanFactoryPostProcessor，当我看到这里的时候，愣住了，通过IDEA的查找引用功能，我发现这个集合永远都是空的，根本没有代码为这个集合添加数据，很久都没有想通，后来才知道我们在外部可以手动添加一个后置处理器，而不是交给Spring去扫描，即：


```
		AnnotationConfigApplicationContext annotationConfigApplicationContext =
				new AnnotationConfigApplicationContext(AppConfig.class);
		annotationConfigApplicationContext.addBeanFactoryPostProcessor(XXX);
```

只有这样，这个集合才不会为空，但是应该没有人这么做吧，当然也有可能是我孤陋寡闻。


#### invokeBeanFactoryPostProcessors方法：


进入PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());


```
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		//调用BeanDefinitionRegistryPostProcessor的后置处理器 Begin
		// 1、定义了一个Set，装载BeanName，后面会根据这个Set，来判断后置处理器是否被执行过了
		Set<String> processedBeans = new HashSet<>();

		// 2、判断我们的beanFactory实现了BeanDefinitionRegistry(实现了该接口就有注册和获取Bean定义的能力）
		if (beanFactory instanceof BeanDefinitionRegistry) {
			//强行把我们的bean工厂转为BeanDefinitionRegistry，因为待会需要注册Bean定义
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			// 3、定义了两个List，一个是regularPostProcessors，用来装载BeanFactoryPostProcessor，
			// 一个是registryProcessors用来装载BeanDefinitionRegistryPostProcessor

			//保存BeanFactoryPostProcessor类型的后置   BeanFactoryPostProcessor 提供修改
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			//保存BeanDefinitionRegistryPostProcessor类型的后置处理器 BeanDefinitionRegistryPostProcessor 提供注册
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			// 4、循环我们传递进来的beanFactoryPostProcessors
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				//判断我们的后置处理器是不是BeanDefinitionRegistryPostProcessor
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					//进行强制转化
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					//调用他作为BeanDefinitionRegistryPostProcessor的处理器的后置方法
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					//添加到我们用于保存的BeanDefinitionRegistryPostProcessor的集合中
					registryProcessors.add(registryProcessor);
				}
				else {
					//若没有实现BeanDefinitionRegistryPostProcessor 接口，那么他就是BeanFactoryPostProcessor
					//把当前的后置处理器加入到regularPostProcessors中
					regularPostProcessors.add(postProcessor);
				}
			}

			// 5、定义一个集合用户保存当前准备创建的BeanDefinitionRegistryPostProcessor
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// 6、去容器中获取BeanDefinitionRegistryPostProcessor的bean的处理器名称
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			// 7 、循环筛选出来的匹配BeanDefinitionRegistryPostProcessor的类型名称
			for (String ppName : postProcessorNames) {
				//判断是否实现了PriorityOrdered接口的
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					//显示的调用getBean()的方式获取出该对象然后加入到currentRegistryProcessors集合中去
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//同时也加入到processedBeans集合中去
					processedBeans.add(ppName);
				}
			}
			// 8、对currentRegistryProcessors集合中BeanDefinitionRegistryPostProcessor进行排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 9、把当前的加入到总的里面去
			registryProcessors.addAll(currentRegistryProcessors);
			/**
			 * 10、在这里典型的BeanDefinitionRegistryPostProcessor就是ConfigurationClassPostProcessor
			 * 用于进行bean定义的加载 比如我们的包扫描，@import  等等。。。。。。。。。
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// 11 、调用完之后，马上clear掉
			currentRegistryProcessors.clear();

			//---------------------------------------调用内置实现PriorityOrdered接口ConfigurationClassPostProcessor完毕--优先级No1-End----------------------------------------------------
			// 12、去容器中获取BeanDefinitionRegistryPostProcessor的bean的处理器名称（内置的和上面注册的）
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			// 循环上一步获取的BeanDefinitionRegistryPostProcessor的类型名称
			for (String ppName : postProcessorNames) {
				//表示没有被处理过,且实现了Ordered接口的
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					//显示的调用getBean()的方式获取出该对象然后加入到currentRegistryProcessors集合中去
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//同时也加入到processedBeans集合中去
					processedBeans.add(ppName);
				}
			}
			// 13、对currentRegistryProcessors集合中BeanDefinitionRegistryPostProcessor进行排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 14、把他加入到用于保存到registryProcessors中
			registryProcessors.addAll(currentRegistryProcessors);
			// 15、调用他的后置处理方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// 16、调用完之后，马上clear掉
			currentRegistryProcessors.clear();

			//-----------------------------------------调用自定义Order接口BeanDefinitionRegistryPostProcessor完毕-优先级No2-End-------------------------------------------------
			// 17、调用没有实现任何优先级接口的BeanDefinitionRegistryPostProcessor
			//定义一个重复处理的开关变量 默认值为true
			boolean reiterate = true;
			//第一次就可以进来
			while (reiterate) {
				//进入循环马上把开关变量给改为false
				reiterate = false;
				//去容器中获取BeanDefinitionRegistryPostProcessor的bean的处理器名称
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				//循环上一步获取的BeanDefinitionRegistryPostProcessor的类型名称
				for (String ppName : postProcessorNames) {
					//没有被处理过的
					if (!processedBeans.contains(ppName)) {
						//显示的调用getBean()的方式获取出该对象然后加入到currentRegistryProcessors集合中去
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						//同时也加入到processedBeans集合中去
						processedBeans.add(ppName);
						//再次设置为true
						reiterate = true;
					}
				}
				// 18、对currentRegistryProcessors集合中BeanDefinitionRegistryPostProcessor进行排序
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				// 19、把他加入到用于保存到registryProcessors中
				registryProcessors.addAll(currentRegistryProcessors);
				// 20、调用他的后置处理方法
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				//进行clear
				currentRegistryProcessors.clear();
			}

			//-------------调用没有实现任何优先级接口自定义BeanDefinitionRegistryPostProcessor完毕--End------
			// 21、调用 BeanDefinitionRegistryPostProcessor.postProcessBeanFactory方法
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			// 22、调用BeanFactoryPostProcessor 自设的（没有）
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			//23、若当前的beanFactory没有实现了BeanDefinitionRegistry 说明没有注册Bean定义的能力
			// 那么就直接调用BeanDefinitionRegistryPostProcessor.postProcessBeanFactory方法
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

//-----------------------------------------所有BeanDefinitionRegistryPostProcessor调用完毕--End--------


//-----------------------------------------处理BeanFactoryPostProcessor --Begin-----------------------

		// 24、获取容器中所有的 BeanFactoryPostProcessor
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		//保存BeanFactoryPostProcessor类型实现了priorityOrdered
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		//保存BeanFactoryPostProcessor类型实现了Ordered接口的
		List<String> orderedPostProcessorNames = new ArrayList<>();
		//保存BeanFactoryPostProcessor没有实现任何优先级接口的
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			//processedBeans包含的话，表示在上面处理BeanDefinitionRegistryPostProcessor的时候处理过了
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			//判断是否实现了PriorityOrdered 优先级最高
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			//判断是否实现了Ordered  优先级 其次
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			//没有实现任何的优先级接口的  最后调用
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		//  25、排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 26、先调用BeanFactoryPostProcessor实现了 PriorityOrdered接口的
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);
		
		
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// 27、排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		// 28、再调用BeanFactoryPostProcessor实现了 Ordered.
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// 29、调用没有实现任何方法接口的
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);
//---------------------------处理BeanFactoryPostProcessor --End-------------------------

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
//------------------- BeanFactoryPostProcessor和BeanDefinitionRegistryPostProcessor调用完毕 --End

	}
```

首先判断beanFactory是不是BeanDefinitionRegistry的实例，当然肯定是的，然后执行如下操作：

1. 定义了一个Set，装载BeanName，后面会根据这个Set，来判断后置处理器是否被执行过了。
2. 定义了两个List，一个是regularPostProcessors，用来装载BeanFactoryPostProcessor，一个是registryProcessors用来装载BeanDefinitionRegistryPostProcessor，其中BeanDefinitionRegistryPostProcessor扩展了BeanFactoryPostProcessor。BeanDefinitionRegistryPostProcessor有两个方法，一个是独有的postProcessBeanDefinitionRegistry方法，一个是父类的postProcessBeanFactory方法。
3. 循环传进来的beanFactoryPostProcessors，上面已经解释过了，一般情况下，这里永远都是空的，只有手动add beanFactoryPostProcessor，这里才会有数据。我们假设beanFactoryPostProcessors有数据，进入循环，判断postProcessor是不是BeanDefinitionRegistryPostProcessor，因为BeanDefinitionRegistryPostProcessor扩展了BeanFactoryPostProcessor，所以这里先要判断是不是BeanDefinitionRegistryPostProcessor，是的话，执行postProcessBeanDefinitionRegistry方法，然后把对象装到registryProcessors里面去，不是的话，就装到regularPostProcessors。
4. 定义了一个临时变量：currentRegistryProcessors，用来装载BeanDefinitionRegistryPostProcessor。
5. getBeanNamesForType，顾名思义，是根据类型查到BeanNames，这里有一点需要注意，就是去哪里找，点开这个方法的话，就知道是循环beanDefinitionNames去找，这个方法以后也会经常看到。这里传了BeanDefinitionRegistryPostProcessor.class，就是找到类型为BeanDefinitionRegistryPostProcessor的后置处理器，并且赋值给postProcessorNames。一般情况下，只会找到一个，就是org.springframework.context.annotation.internalConfigurationAnnotationProcessor，也就是ConfigurationAnnotationProcessor。这个后置处理器在上一节中已经说明过了，十分重要。这里有一个问题，为什么我自己写了个类，实现了BeanDefinitionRegistryPostProcessor接口，也打上了@Component注解，但是这里没有获得，因为直到这一步，Spring还没有完成扫描，扫描是在ConfigurationClassPostProcessor类中完成的，也就是下面第一个invokeBeanDefinitionRegistryPostProcessors方法。

<img src="https://note.youdao.com/yws/api/personal/file/12EB30E2B10649349EC1C953C29C3698?method=download&shareKey=1daa45d3a55861aed564ee381504c804" width = "800" />

> **ps：说明一下** `ConfigurationAnnotationProcessor`既实现了 `PriorityOrdered`接口，也实现了`Ordered`接口，前面处理过了后面就不会再次执行`invokeBeanDefinitionRegistryPostProcessors`接口处理了。

> **order值越小越先执行**

6. 进行排序，PriorityOrdered是一个排序接口，如果实现了它，就说明此后置处理器是有顺序的，所以需要排序。当然：目前这里只有一个后置处理器，就是ConfigurationClassPostProcessor。

7. 把currentRegistryProcessors合并到registryProcessors，为什么需要合并？因为一开始spring只会执行BeanDefinitionRegistryPostProcessor独有的方法，而不会执行BeanDefinitionRegistryPostProcessor父类的方法，即BeanFactoryProcessor接口中的方法，所以需要把这些后置处理器放入一个集合中，后续统一执行BeanFactoryProcessor接口中的方法。当然目前这里只有一个后置处理器，就是ConfigurationClassPostProcessor。

8. 可以理解为执行currentRegistryProcessors中的ConfigurationClassPostProcessor中的postProcessBeanDefinitionRegistry方法，这就是Spring设计思想的体现了，在这里体现的就是其中的热插拔，插件化开发的思想。Spring中很多东西都是交给插件去处理的，这个后置处理器就相当于一个插件，如果不想用了，直接不添加就是了。这个方法特别重要，我们后面会详细说来。

9. 清空currentRegistryProcessors，因为currentRegistryProcessors是一个临时变量，已经完成了目前的使命，所以需要清空，当然后面还会用到。

10. 再次根据BeanDefinitionRegistryPostProcessor获得BeanName，然后进行循环，看这个后置处理器是否被执行过了，如果没有被执行过，也实现了Ordered接口的话，把此后置处理器推送到currentRegistryProcessors和processedBeans中。
    这里就可以获得我们定义的，并且打上@Component注解的后置处理器了，因为Spring已经完成了扫描，但是这里需要注意的是，由于ConfigurationClassPostProcessor在上面已经被执行过了，所以虽然可以通过getBeanNamesForType获得，但是并不会加入到currentRegistryProcessors和processedBeans。

11. 处理排序。

12. 合并Processors，合并的理由和上面是一样的。

13. 执行我们自定义的BeanDefinitionRegistryPostProcessor。
14. 清空临时变量。

15. 在上面的方法中，仅仅是执行了实现了Ordered接口的BeanDefinitionRegistryPostProcessor，这里是执行没有实现Ordered接口的BeanDefinitionRegistryPostProcessor。

16. 上面的代码是执行子类独有的方法，这里需要再把父类的方法也执行一次。

17. 执行regularPostProcessors中的后置处理器的方法，需要注意的是，在一般情况下，regularPostProcessors是不会有数据的，只有在外面手动添加BeanFactoryPostProcessor，才会有数据。

18. 查找实现了BeanFactoryPostProcessor的后置处理器，并且执行后置处理器中的方法。和上面的逻辑差不多，不再详细说明。

    #### org.springframework.context.annotation.ConfigurationClassPostProcessor#processConfigBeanDefinitions


```
public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
		//获取IOC 容器中目前所有bean定义的名称
		String[] candidateNames = registry.getBeanDefinitionNames();
		//循环我们的上一步获取的所有的bean定义信息
		for (String beanName : candidateNames) {
			//通过bean的名称来获取我们的bean定义对象
			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
			//判断是否有没有解析过
			if (beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE) != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
				}
			}
			//进行正在的解析判断是不是完全的配置类 还是一个非正式的配置类
			else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
				//满足添加 就加入到候选的配置类集合中
				configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}

		// Return immediately if no @Configuration classes were found
		// 若没有找到配置类 直接返回
		if (configCandidates.isEmpty()) {
			return;
		}

		// Sort by previously determined @Order value, if applicable
		//对我们的配置类进行Order排序
		configCandidates.sort((bd1, bd2) -> {
			int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
			int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
			return Integer.compare(i1, i2);
		});

		// Detect any custom bean name generation strategy supplied through the enclosing application context
		// 创建我们通过@CompentScan导入进来的bean name的生成器
		// 创建我们通过@Import导入进来的bean的名称
		SingletonBeanRegistry sbr = null;
		if (registry instanceof SingletonBeanRegistry) {
			sbr = (SingletonBeanRegistry) registry;
			if (!this.localBeanNameGeneratorSet) {
				BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(
						AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
				if (generator != null) {
					//设置@CompentScan导入进来的bean的名称生成器(默认类首字母小写）也可以自己定义，一般不会
					this.componentScanBeanNameGenerator = generator;
					//设置@Import导入进来的bean的名称生成器(默认类首字母小写）也可以自己定义，一般不会
					this.importBeanNameGenerator = generator;
				}
			}
		}

		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}

		// Parse each @Configuration class
		//创建一个配置类解析器对象
		ConfigurationClassParser parser = new ConfigurationClassParser(
				this.metadataReaderFactory, this.problemReporter, this.environment,
				this.resourceLoader, this.componentScanBeanNameGenerator, registry);
		//用于保存我们的配置类BeanDefinitionHolder放入上面筛选出来的配置类
		Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
		//用于保存我们的已经解析的配置类，长度默认为解析出来默认的配置类的集合长度
		Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());
		//do while 会进行第一次解析
		do {
			//真正的解析我们的配置类
			parser.parse(candidates);
			parser.validate();
			//解析出来的配置类
			Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
			configClasses.removeAll(alreadyParsed);

			// Read the model and create bean definitions based on its content
			if (this.reader == null) {
				this.reader = new ConfigurationClassBeanDefinitionReader(
						registry, this.sourceExtractor, this.resourceLoader, this.environment,
						this.importBeanNameGenerator, parser.getImportRegistry());
			}
			// 此处才把@Bean的方法和@Import 注册到BeanDefinitionMap中
			this.reader.loadBeanDefinitions(configClasses);
			//加入到已经解析的集合中
			alreadyParsed.addAll(configClasses);

			candidates.clear();
			//判断我们ioc容器中的是不是>候选原始的bean定义的个数
			if (registry.getBeanDefinitionCount() > candidateNames.length) {
				//获取所有的bean定义
				String[] newCandidateNames = registry.getBeanDefinitionNames();
				//原始的老的候选的bean定义
				Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));
				Set<String> alreadyParsedClasses = new HashSet<>();
				//赋值已经解析的
				for (ConfigurationClass configurationClass : alreadyParsed) {
					alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
				}
				for (String candidateName : newCandidateNames) {
					//表示当前循环的还没有被解析过
					if (!oldCandidateNames.contains(candidateName)) {
						BeanDefinition bd = registry.getBeanDefinition(candidateName);
						//判断有没有被解析过
						if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
								!alreadyParsedClasses.contains(bd.getBeanClassName())) {
							candidates.add(new BeanDefinitionHolder(bd, candidateName));
						}
					}
				}
				candidateNames = newCandidateNames;
			}
		}
		//存在没有解析过的 需要循环解析
		while (!candidates.isEmpty());

		// Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
		if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
			sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
		}

		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
			// Clear cache in externally provided MetadataReaderFactory; this is a no-op
			// for a shared cache since it'll be cleared by the ApplicationContext.
			((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
		}
	}
```

1. 获得所有的BeanName，放入candidateNames数组。

2. 循环candidateNames数组，根据beanName获得BeanDefinition，判断此BeanDefinition是否已经被处理过了。

   <img src="https://note.youdao.com/yws/api/personal/file/31A5B79EC4564CFF91BBE51E087479EF?method=download&shareKey=c53f6aff7032758eac626c3fcda3573b" width = "800" />

   

3. 判断是否是配置类，如果是的话。加入到configCandidates数组，在判断的时候，还会标记配置类属于Full配置类，还是Lite配置类，这里会引发一连串的知识盲点：

3.1 当我们注册配置类的时候，可以不加@Configuration注解，直接使用@Component @ComponentScan @Import @ImportResource等注解，Spring把这种配置类称之为Lite配置类， 如果加了@Configuration注解，就称之为Full配置类。

3.2 如果我们注册了Lite配置类，我们getBean这个配置类，会发现它就是原本的那个配置类，如果我们注册了Full配置类，我们getBean这个配置类，会发现它已经不是原本那个配置类了，而是已经被cgilb代理的类了。

3.3 写一个A类，其中有一个构造方法，打印出“你好”，再写一个配置类，里面有两个被@bean注解的方法，其中一个方法new了A类，并且返回A的对象，把此方法称之为getA，第二个方法又调用了getA方法，如果配置类是Lite配置类，会发现打印了两次“你好”，也就是说A类被new了两次，如果配置类是Full配置类，会发现只打印了一次“你好”，也就是说A类只被new了一次，因为这个类被cgilb代理了，方法已经被改写。

4. 如果没有配置类直接返回。

5. 处理排序。我们找到了一个配置类，就是我们的`MainApp`上面加了@`Configuration`注解

   <img src="https://note.youdao.com/yws/api/personal/file/92CE73B56E264D679E09CAC50F4BE074?method=download&shareKey=b3a09567b603549cd51c1b148443a18f" width = "800" />

6. 解析配置类，可能是Full配置类，也有可能是Lite配置类，这个小方法是此方法的核心，稍后具体说明。

   ```
   ConfigurationClassParser parser = new ConfigurationClassParser(
         this.metadataReaderFactory, this.problemReporter, this.environment,
         this.resourceLoader, this.componentScanBeanNameGenerator, registry)
         ...
        parser.parse(candidates)； 
   ```

7. 在第6步的时候，只是注册了部分Bean，像 @Import @Bean等，是没有被注册的，这里统一对这些进行注册。


#### 下面是解析配置类的过程：


```
public void parse(Set<BeanDefinitionHolder> configCandidates) {
		// 循环配置类
		for (BeanDefinitionHolder holder : configCandidates) {
			BeanDefinition bd = holder.getBeanDefinition();
			try {
				//真正的解析我们的bean定义 :通过注解元数据 解析
				if (bd instanceof AnnotatedBeanDefinition) {
					parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
				}
				else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
					parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
				}
				else {
					parse(bd.getBeanClassName(), holder.getBeanName());
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
			}
		}

		this.deferredImportSelectorHandler.process();
	}
```

因为可以有多个配置类，所以需要循环处理。我们的配置类的BeanDefinition是AnnotatedBeanDefinition的实例，所以会进入第一个if：

<img src="https://note.youdao.com/yws/api/personal/file/F9C4D5E8D32D429492ED4D71EDD29CC9?method=download&shareKey=349573ae50096dd5e7fd34efa8f9c9c2" width = "800" />

#### org.springframework.context.annotation.ConfigurationClassParser#processConfigurationClass


```
protected void processConfigurationClass(ConfigurationClass configClass, Predicate<String> filter) throws IOException {
		if (this.cond	protected void processConfigurationClass(ConfigurationClass configClass, Predicate<String> filter) throws IOException {
		if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
			return;
		}
		//获取处我们的配置类对象
		ConfigurationClass existingClass = this.configurationClasses.get(configClass);
		if (existingClass != null) {
			//传入进来的配置类是通过其他配置类的Import导入进来的
			if (configClass.isImported()) {
				if (existingClass.isImported()) {
					//需要合并配置
					existingClass.mergeImportedBy(configClass);
				}
				// Otherwise ignore new imported config class; existing non-imported class overrides it.
				// 所以假如通过@Import导入一个 已存在的配置类 是不允许的，会忽略。
				return;
			}
			else {
				// Explicit bean definition found, probably replacing an import.
				// Let's remove the old one and go with the new one.
				this.configurationClasses.remove(configClass);
				this.knownSuperclasses.values().removeIf(configClass::equals);
			}
		}

		// Recursively process the configuration class and its superclass hierarchy.
		SourceClass sourceClass = asSourceClass(configClass, filter);
		//真正的进行配置类的解析
		do {
			//解析我们的配置类
			sourceClass = doProcessConfigurationClass(configClass, sourceClass, filter);
		}
		while (sourceClass != null);

		this.configurationClasses.put(configClass, configClass);
	}itionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
			return;
		}

		ConfigurationClass existingClass = this.configurationClasses.get(configClass);
		if (existingClass != null) {
			if (configClass.isImported()) {
				if (existingClass.isImported()) {
					existingClass.mergeImportedBy(configClass);
				}
				// Otherwise ignore new imported config class; existing non-imported class overrides it.
				return;
			}
			else {
				// Explicit bean definition found, probably replacing an import.
				// Let's remove the old one and go with the new one.
				this.configurationClasses.remove(configClass);
				this.knownSuperclasses.values().removeIf(configClass::equals);
			}
		}

		// Recursively process the configuration class and its superclass hierarchy.
		SourceClass sourceClass = asSourceClass(configClass, filter);
		do {
			sourceClass = doProcessConfigurationClass(configClass, sourceClass, filter);
		}
		while (sourceClass != null);

		this.configurationClasses.put(configClass, configClass);
	}
```

重点在于doProcessConfigurationClass方法，需要特别注意，最后一行代码，会把configClass放入一个Map，会在上面第7步中用到。

#### org.springframework.context.annotation.ConfigurationClassParser#doProcessConfigurationClass


```
	protected final SourceClass doProcessConfigurationClass(
			ConfigurationClass configClass, SourceClass sourceClass, Predicate<String> filter)
			throws IOException {

		// 处理我们的@Component注解的
		if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
			// Recursively process any member (nested) classes first
			processMemberClasses(configClass, sourceClass, filter);
		}

		// Process any @PropertySource annotations
		// 处理我们的@propertySource注解的
		for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), PropertySources.class,
				org.springframework.context.annotation.PropertySource.class)) {
			if (this.environment instanceof ConfigurableEnvironment) {
				processPropertySource(propertySource);
			}
			else {
				logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
						"]. Reason: Environment must implement ConfigurableEnvironment");
			}
		}

		// Process any @ComponentScan annotations
		// 解析我们的 @ComponentScan 注解 从我们的配置类上解析处ComponentScans的对象集合属性
		Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
		if (!componentScans.isEmpty() &&
				!this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
			//循环解析 我们解析出来的AnnotationAttributes
			for (AnnotationAttributes componentScan : componentScans) {
				// The config class is annotated with @ComponentScan -> perform the scan immediately
				//把我们扫描出来的类变为bean定义的集合 真正的解析
				Set<BeanDefinitionHolder> scannedBeanDefinitions =
						this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
				// Check the set of scanned definitions for any further config classes and parse recursively if needed
				//循环处理我们包扫描出来的bean定义
				for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
					BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
					if (bdCand == null) {
						bdCand = holder.getBeanDefinition();
					}
					//判断当前扫描出来的bean定义是不是一个配置类,若是的话 直接进行递归解析
					if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
						//递归解析 因为@Component算是lite配置类
						parse(bdCand.getBeanClassName(), holder.getBeanName());
					}
				}
			}
		}

		// Process any @Import annotations
		// 处理 @Import annotations
		//@Import注解是spring中很重要的一个注解，Springboot大量应用这个注解
		//@Import三种类，一种是Import普通类，一种是Import ImportSelector，还有一种是Import ImportBeanDefinitionRegistrar
		//getImports(sourceClass)是获得import的内容，返回的是一个set
		processImports(configClass, sourceClass, getImports(sourceClass), filter, true);

		// Process any @ImportResource annotations
		// 处理 @ImportResource annotations
		AnnotationAttributes importResource =
				AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
		if (importResource != null) {
			String[] resources = importResource.getStringArray("locations");
			Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
			for (String resource : resources) {
				String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
				configClass.addImportedResource(resolvedResource, readerClass);
			}
		}

		// Process individual @Bean methods
		// 处理 @Bean methods 获取到我们配置类中所有标注了@Bean的方法
		Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
		for (MethodMetadata methodMetadata : beanMethods) {
			configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
		}

		// Process default methods on interfaces
		// 处理配置类接口 默认方法的@Bean
		processInterfaces(configClass, sourceClass);

		// Process superclass, if any
		// 处理配置类的父类的 ，循环再解析
		if (sourceClass.getMetadata().hasSuperClass()) {
			String superclass = sourceClass.getMetadata().getSuperClassName();
			if (superclass != null && !superclass.startsWith("java") &&
					!this.knownSuperclasses.containsKey(superclass)) {
				this.knownSuperclasses.put(superclass, configClass);
				// Superclass found, return its annotation metadata and recurse
				return sourceClass.getSuperClass();
			}
		}

		// No superclass -> processing is complete
		// 没有父类解析完成
		return null;
	}
```

1. 处理我们的@Component注解的

   <img src="https://note.youdao.com/yws/api/personal/file/66CA57DD4440401DA957B8750A90F804?method=download&shareKey=e51ab5f187b0242ec6e4371eb0ef9b1e" width = "800" />

   

2. 处理@PropertySource注解，@PropertySource注解用来加载properties文件。

3. 获得ComponentScan注解具体的内容，ComponentScan注解除了最常用的basePackage之外，还有includeFilters，excludeFilters等。

   <img src="https://note.youdao.com/yws/api/personal/file/C94AEBC4DEE442919DD66FF48799F2D2?method=download&shareKey=74bfb421c0ba9dd2a57f3119df6931e4" width = "800" />

   

4. 判断有没有被@ComponentScans标记，或者被@Condition条件带过，如果满足条件的话，进入if，进行如下操作：

4.1 执行扫描操作，把扫描出来的放入set，这个方法稍后再详细说明。

4.2 循环set，判断是否是配置类，是的话，递归调用parse方法，因为被扫描出来的类，还是一个配置类，有@ComponentScans注解，或者其中有被@Bean标记的方法 等等，所以需要再次被解析。

5. 处理@Import注解，@Import是Spring中很重要的一个注解，正是由于它的存在，让Spring非常灵活，不管是Spring内部，还是与Spring整合的第三方技术，都大量的运用了@Import注解，@Import有三种情况，一种是Import普通类，一种是Import ImportSelector，还有一种是Import ImportBeanDefinitionRegistrar，getImports(sourceClass)是获得import的内容，返回的是一个set，这个方法稍后再详细说明。
6. 处理@ImportResource注解。
7. 处理@Bean的方法，可以看到获得了带有@Bean的方法后，不是马上转换成BeanDefinition，而是先用一个set接收。

#### org.springframework.context.annotation.ComponentScanAnnotationParser#parse（我们先来看4.1中的parse方法）

<img src="https://note.youdao.com/yws/api/personal/file/28C7BB2EC263480DABF4C78BCAA6F09C?method=download&shareKey=fa771cf5be6f1bffec1694b8fb83cdd9" width = "800" />



```
public Set<BeanDefinitionHolder> parse(AnnotationAttributes componentScan, final String declaringClass) {
		//扫描器，还记不记在new AnnotationConfigApplicationContext的时候
		//会调用AnnotationConfigApplicationContext的构造方法
		//构造方法里面有一句 this.scanner = new ClassPathBeanDefinitionScanner(this);
		//当时说这个对象不重要，这里就是证明了。常规用法中，实际上执行扫描的只会是这里的scanner对象
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.registry,
				componentScan.getBoolean("useDefaultFilters"), this.environment, this.resourceLoader);

		//判断是否重写了默认的命名规则
		Class<? extends BeanNameGenerator> generatorClass = componentScan.getClass("nameGenerator");
		boolean useInheritedGenerator = (BeanNameGenerator.class == generatorClass);
		scanner.setBeanNameGenerator(useInheritedGenerator ? this.beanNameGenerator :
				BeanUtils.instantiateClass(generatorClass));

		ScopedProxyMode scopedProxyMode = componentScan.getEnum("scopedProxy");
		if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
			scanner.setScopedProxyMode(scopedProxyMode);
		}
		else {
			Class<? extends ScopeMetadataResolver> resolverClass = componentScan.getClass("scopeResolver");
			scanner.setScopeMetadataResolver(BeanUtils.instantiateClass(resolverClass));
		}

		scanner.setResourcePattern(componentScan.getString("resourcePattern"));

		//addIncludeFilter addExcludeFilter,最终是往List<TypeFilter>里面填充数据
		//TypeFilter是一个函数式接口，函数式接口在java8的时候大放异彩，只定义了一个虚方法的接口被称为函数式接口
		//当调用scanner.addIncludeFilter  scanner.addExcludeFilter 仅仅把 定义的规则塞进去，并么有真正去执行匹配过程

		//处理includeFilters
		for (AnnotationAttributes filter : componentScan.getAnnotationArray("includeFilters")) {
			for (TypeFilter typeFilter : typeFiltersFor(filter)) {
				scanner.addIncludeFilter(typeFilter);
			}
		}
		//处理excludeFilters
		for (AnnotationAttributes filter : componentScan.getAnnotationArray("excludeFilters")) {
			for (TypeFilter typeFilter : typeFiltersFor(filter)) {
				scanner.addExcludeFilter(typeFilter);
			}
		}

		boolean lazyInit = componentScan.getBoolean("lazyInit");
		if (lazyInit) {
			scanner.getBeanDefinitionDefaults().setLazyInit(true);
		}

		Set<String> basePackages = new LinkedHashSet<>();
		String[] basePackagesArray = componentScan.getStringArray("basePackages");
		for (String pkg : basePackagesArray) {
			String[] tokenized = StringUtils.tokenizeToStringArray(this.environment.resolvePlaceholders(pkg),
					ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
			Collections.addAll(basePackages, tokenized);
		}
		// 从下面的代码可以看出ComponentScans指定扫描目标，除了最常用的basePackages，还有两种方式
		// 1.指定basePackageClasses，就是指定多个类，只要是与这几个类同级的，或者在这几个类下级的都可以被扫描到，这种方式其实是spring比较推荐的
		// 因为指定basePackages没有IDE的检查，容易出错，但是指定一个类，就有IDE的检查了，不容易出错，经常会用一个空的类来作为basePackageClasses
		// 2.直接不指定，默认会把与配置类同级，或者在配置类下级的作为扫描目标
		for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
			basePackages.add(ClassUtils.getPackageName(clazz));
		}
		if (basePackages.isEmpty()) {
			basePackages.add(ClassUtils.getPackageName(declaringClass));
		}
		//把规则填充到排除规则：List<TypeFilter>，这里就把 注册类自身当作排除规则，真正执行匹配的时候，会把自身给排除
		scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {
			@Override
			protected boolean matchClassName(String className) {
				return declaringClass.equals(className);
			}
		});
		//basePackages是一个LinkedHashSet<String>，这里就是把basePackages转为字符串数组的形式
		return scanner.doScan(StringUtils.toStringArray(basePackages));
	}
```

1. 定义了一个扫描器scanner，还记不记在new AnnotationConfigApplicationContext的时候，会调用AnnotationConfigApplicationContext的构造方法，构造方法里面有一句 this.scanner = new ClassPathBeanDefinitionScanner(this);当时说这个对象不重要，这里就是证明了。常规用法中，实际上执行扫描的只会是这里的scanner对象。

2. 处理includeFilters，就是把规则添加到scanner。

3. 处理excludeFilters，就是把规则添加到scanner。

4. 解析basePackages，获得需要扫描哪些包，如果为空的话，会默认按照

   <img src="https://note.youdao.com/yws/api/personal/file/14CE6224713843C998E47074A6325A3D?method=download&shareKey=4d20a2df0cb4f3b05224dd6d50e5f7ca" width = "800" />

   默认的解析路径

   <img src="https://note.youdao.com/yws/api/personal/file/B779625649BD41E9B9DA178EBDD7BA7F?method=download&shareKey=987dc01b038872d4a8247678052a2543" width = "800" />

   

5. 添加一个默认的排除规则：排除自身。

6. 执行扫描，稍后详细说明。

> 这里需要做一个补充说明，添加规则的时候，只是把具体的规则放入规则类的集合中去，规则类是一个函数式接口，只定义了一个虚方法的接口被称为函数式接口，函数式接口在java8的时候大放异彩，这里只是把规则方塞进去，并没有真正执行匹配规则。

org.springframework.util.ClassUtils#getPackageName(java.lang.String)

```
public static String getPackageName(String fqClassName) {
   Assert.notNull(fqClassName, "Class name must not be null");
   int lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR);
   return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "");
}
```

#### org.springframework.context.annotation.ClassPathBeanDefinitionScanner#doScan（我们来看看到底是怎么执行扫描的doScan）：

<img src="https://note.youdao.com/yws/api/personal/file/846727516A3E4193A45DA6C6DC13A1C5?method=download&shareKey=341a2cd2f7b4cc70f27408af1fb53c48" width = "800" />




```
protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		//创建bean定义的holder对象用于保存扫描后生成的bean定义对象
		Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
		//循环我们的包路径集合
		for (String basePackage : basePackages) {
			//找到候选的Components
			Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
			for (BeanDefinition candidate : candidates) {
				ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
				candidate.setScope(scopeMetadata.getScopeName());
				//设置我们的beanName
				String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
				//这是默认配置 autowire-candidate
				if (candidate instanceof AbstractBeanDefinition) {
					postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
				}
				//获取@Lazy @DependsOn等注解的数据设置到BeanDefinition中
				if (candidate instanceof AnnotatedBeanDefinition) {
					AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
				}
				//把我们解析出来的组件bean定义注册到我们的IOC容器中（容器中没有才注册）
				if (checkCandidate(beanName, candidate)) {
					BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
					definitionHolder =
							AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
					beanDefinitions.add(definitionHolder);
					registerBeanDefinition(definitionHolder, this.registry);
				}
			}
		}
		return beanDefinitions;
	}
```
因为basePackages可能有多个，所以需要循环处理，最终会进行Bean的注册。

#### findCandidateComponents方法：

```
public Set<BeanDefinition> findCandidateComponents(String basePackage) {
		//spring支持component索引技术，需要引入一个组件，因为大部分情况不会引入这个组件
		//所以不会进入到这个if
		if (this.componentsIndex != null && indexSupportsIncludeFilters()) {
			return addCandidateComponentsFromIndex(this.componentsIndex, basePackage);
		}
		else {
			return scanCandidateComponents(basePackage);
		}
	}
```

Spring支持component索引技术，需要引入一个组件，大部分项目没有引入这个组件

#### scanCandidateComponents方法：


```
private Set<BeanDefinition> scanCandidateComponents(String basePackage) {
		Set<BeanDefinition> candidates = new LinkedHashSet<>();
		try {
			//把我们的包路径转为资源路径
			//把 传进来的类似 命名空间形式的字符串转换成类似类文件地址的形式，然后在前面加上classpath*:
			//即：com.xx=>classpath*:com/xx/**/*.class
			String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
					resolveBasePackage(basePackage) + '/' + this.resourcePattern;
			//扫描指定包路径下面的所有.class文件
			Resource[] resources = getResourcePatternResolver().getResources(packageSearchPath);
			boolean traceEnabled = logger.isTraceEnabled();
			boolean debugEnabled = logger.isDebugEnabled();
			//需要我们的resources集合
			for (Resource resource : resources) {
				if (traceEnabled) {
					logger.trace("Scanning " + resource);
				}
				//判断当的是不是可读的
				if (resource.isReadable()) {
					try {
						MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(resource);
						//是不是候选的组件
						if (isCandidateComponent(metadataReader)) {
							//包装成为一个ScannedGenericBeanDefinition
							ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
							//并且设置class资源
							sbd.setSource(resource);
							if (isCandidateComponent(sbd)) {
								if (debugEnabled) {
									logger.debug("Identified candidate component class: " + resource);
								}
								//加入到集合中
								candidates.add(sbd);
							}
							else {
								if (debugEnabled) {
									logger.debug("Ignored because not a concrete top-level class: " + resource);
								}
							}
						}
						else {
							if (traceEnabled) {
								logger.trace("Ignored because not matching any filter: " + resource);
							}
						}
					}
					catch (Throwable ex) {
						throw new BeanDefinitionStoreException(
								"Failed to read candidate component class: " + resource, ex);
					}
				}
				else {
					if (traceEnabled) {
						logger.trace("Ignored because not readable: " + resource);
					}
				}
			}
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
		}
		return candidates;
	}
```

1. 把传进来的类似命名空间形式的字符串转换成类似类文件地址的形式，然后在前面加上classpath，即：com.xx=>classpath:com/xx/**/*.class。

   <img src="https://note.youdao.com/yws/api/personal/file/65F669BE8F2341E58CF47EED7409B985?method=download&shareKey=9fed1724dc172a03cfebf3844305c50b" width = "800" />

   

2. 根据packageSearchPath，获得符合要求的文件。

   <img src="https://note.youdao.com/yws/api/personal/file/60ACF1F71D854DDCA94C477F76A11F79?method=download&shareKey=bf40af6b49b98b7ad8d61dbaeeff070f" width = "800" />

   

3. 循环符合要求的文件，进一步进行判断。
  最终会把符合要求的文件，转换为BeanDefinition，并且返回。

4. 注册BeanDefinition

<img src="https://note.youdao.com/yws/api/personal/file/EF5D0314B37D4F61B3087DEBDA4F7AA8?method=download&shareKey=c1bef4e6bae714bb2cf713f869accae8" width = "800" />



```
protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
   for (TypeFilter tf : this.excludeFilters) {
      if (tf.match(metadataReader, getMetadataReaderFactory())) {
         return false;
      }
   }
   for (TypeFilter tf : this.includeFilters) {
      if (tf.match(metadataReader, getMetadataReaderFactory())) {
         return isConditionMatch(metadataReader);
      }
   }
   return false;
}
```

```
protected boolean matchSelf(MetadataReader metadataReader) {
   AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
   return metadata.hasAnnotation(this.annotationType.getName()) ||
         (this.considerMetaAnnotations && metadata.hasMetaAnnotation(this.annotationType.getName()));
}
```

#### org.springframework.core.annotation.MergedAnnotationsCollection#find（注解配置规则主要代码）

```
private <A extends Annotation> MergedAnnotation<A> find(Object requiredType,
      @Nullable Predicate<? super MergedAnnotation<A>> predicate,
      @Nullable MergedAnnotationSelector<A> selector) {

   if (selector == null) {
      selector = MergedAnnotationSelectors.nearest();
   }

   MergedAnnotation<A> result = null;
   for (int i = 0; i < this.annotations.length; i++) {
      MergedAnnotation<?> root = this.annotations[i];
      AnnotationTypeMappings mappings = this.mappings[i];
      for (int mappingIndex = 0; mappingIndex < mappings.size(); mappingIndex++) {
         AnnotationTypeMapping mapping = mappings.get(mappingIndex);
         if (!isMappingForType(mapping, requiredType)) {
            continue;
         }
         MergedAnnotation<A> candidate = (mappingIndex == 0 ? (MergedAnnotation<A>) root :
               TypeMappedAnnotation.createIfPossible(mapping, root, IntrospectionFailureLogger.INFO));
         if (candidate != null && (predicate == null || predicate.test(candidate))) {
            if (selector.isBestCandidate(candidate)) {
               return candidate;
            }
            result = (result != null ? selector.select(result, candidate) : candidate);
         }
      }
   }
   return result;
}
```

这里我有一个实现了@Service的类进行匹配，会拿到AnotationTypeMappings,  里面有三个元素 

<img src="https://note.youdao.com/yws/api/personal/file/6F3A2E11B59F42C184427AA0BBBE8113?method=download&shareKey=9184a77a32d0dd9bef9d3825185bd4d7" width = "800" />

最终会获取到每个元素的 AnnotationType去和@Component比较，相对则返回true

第一个：

<img src="https://note.youdao.com/yws/api/personal/file/98322910E66E44DA968EB7EE86367087?method=download&shareKey=f165ab4e0a73ebc8a2a85e2101fe283a" width = "800" />

第二个：

<img src="https://note.youdao.com/yws/api/personal/file/75D031A04E974FE192100CB53CD24651?method=download&shareKey=029a1dcbe6298d424df1d210cae5648b" width = "800" />

这里跟的比较深了，我也是看了2遍，可以理解为，除了JDK自带的处理（Spring写的）比如@Service @Component等



#### org.springframework.core.annotation.MergedAnnotationsCollection#isMappingForType

```
private static boolean isMappingForType(AnnotationTypeMapping mapping, @Nullable Object requiredType) {
   if (requiredType == null) {
      return true;
   }
   Class<? extends Annotation> actualType = mapping.getAnnotationType();
   return (actualType == requiredType || actualType.getName().equals(requiredType));
}

static MergedAnnotations of(Collection<MergedAnnotation<?>> annotations) {
   Assert.notNull(annotations, "Annotations must not be null");
   if (annotations.isEmpty()) {
      return TypeMappedAnnotations.NONE;
   }
   return new MergedAnnotationsCollection(annotations);
}
```

最后有2个类满足条件

<img src="https://note.youdao.com/yws/api/personal/file/BA63722F627D406EA4A9BB92D33514BE?method=download&shareKey=b7e2803a243285186570bc9e30b0f636" width = "800" />



注册到BeanDefinitionMap中，可以看到多2个Bean

<img src="https://note.youdao.com/yws/api/personal/file/7A1DB8063F7F4751BF341B0F378B5B54?method=download&shareKey=f050f66af84876dc502ce4eefc7bd6ec" width = "800" />



#### 

####  @Import解析

```
//这个方法内部相当相当复杂，importCandidates是Import的内容，调用这个方法的时候，已经说过可能有三种情况
	//这里再说下，1.Import普通类，2.Import ImportSelector，3.Import ImportBeanDefinitionRegistrar
	//循环importCandidates，判断属于哪种情况
	//如果是普通类，会进到else，调用processConfigurationClass方法
	//这个方法是不是很熟悉，没错，processImports这个方法就是在processConfigurationClass方法中被调用的
	//processImports又主动调用processConfigurationClass方法，是一个递归调用，因为Import的普通类，也有可能被加了Import注解，@ComponentScan注解 或者其他注解，所以普通类需要再次被解析
	//如果Import ImportSelector就跑到了第一个if中去，首先执行Aware接口方法，所以我们在实现ImportSelector的同时，还可以实现Aware接口
	//然后判断是不是DeferredImportSelector，DeferredImportSelector扩展了ImportSelector
	//如果不是的话，调用selectImports方法，获得全限定类名数组，在转换成类的数组，然后再调用processImports，又特么的是一个递归调用...
	//可能又有三种情况，一种情况是selectImports的类是一个普通类，第二种情况是selectImports的类是一个ImportBeanDefinitionRegistrar类，第三种情况是还是一个ImportSelector类...
	//所以又需要递归调用
	//如果Import ImportBeanDefinitionRegistrar就跑到了第二个if，还是会执行Aware接口方法，这里终于没有递归了，会把数据放到ConfigurationClass中的Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> importBeanDefinitionRegistrars中去
	private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
			Collection<SourceClass> importCandidates, Predicate<String> exclusionFilter,
			boolean checkForCircularImports) {

		if (importCandidates.isEmpty()) {
			return;
		}

		if (checkForCircularImports && isChainedImportOnStack(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
		}
		else {
			this.importStack.push(configClass);
			try {
				//获取我们Import导入进来的所有组件
				for (SourceClass candidate : importCandidates) {
					//判断该组件是不是实现了ImportSelector的
					if (candidate.isAssignable(ImportSelector.class)) {
						// Candidate class is an ImportSelector -> delegate to it to determine imports
						Class<?> candidateClass = candidate.loadClass();
						//实例化我们的SelectImport组件
						ImportSelector selector = ParserStrategyUtils.instantiateClass(candidateClass, ImportSelector.class,
								this.environment, this.resourceLoader, this.registry);
						//调用相关的aware方法
						Predicate<String> selectorFilter = selector.getExclusionFilter();
						if (selectorFilter != null) {
							exclusionFilter = exclusionFilter.or(selectorFilter);
						}
						//判断是不是延时的DeferredImportSelectors，是这个类型 不进行处理
						if (selector instanceof DeferredImportSelector) {
							this.deferredImportSelectorHandler.handle(configClass, (DeferredImportSelector) selector);
						}
						else {

							// 不是延时的调用selector的selectImports
							String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
							// 所以递归解析-- 直到成普通组件
							Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames, exclusionFilter);
							processImports(configClass, currentSourceClass, importSourceClasses, exclusionFilter, false);
						}
					}
					//判断我们导入的组件是不是ImportBeanDefinitionRegistrar，这里不直接调用，只是解析
					else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
						// Candidate class is an ImportBeanDefinitionRegistrar ->
						// delegate to it to register additional bean definitions
						Class<?> candidateClass = candidate.loadClass();
						//实例话我们的ImportBeanDefinitionRegistrar对象
						ImportBeanDefinitionRegistrar registrar =
								ParserStrategyUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class,
										this.environment, this.resourceLoader, this.registry);
						//保存我们的ImportBeanDefinitionRegistrar对象 currentSourceClass=所在配置类
						configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
					}
					else {
						// 当做配置类再解析，注意这里会标记：importedBy，  表示这是Import的配置的类
						// 再执行之前的processConfigurationClass()方法 ，
						// Candidate class not an ImportSelector or ImportBeanDefinitionRegistrar ->
						// process it as an @Configuration class
						this.importStack.registerImport(
								currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
						processConfigurationClass(candidate.asConfigClass(configClass), exclusionFilter);
					}
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to process import candidates for configuration class [" +
						configClass.getMetadata().getClassName() + "]", ex);
			}
			finally {
				this.importStack.pop();
			}
		}
	}
```

我们来做一个总结，ConfigurationClassPostProcessor中的processConfigBeanDefinitions方法十分重要，主要是完成扫描，最终注册我们定义的Bean。

## 到这里已经完成了第一步，将类解析成BeanDefinition，存入BeanDefinitionMap中

### 6. registerBeanPostProcessors(beanFactory);

实例化和注册beanFactory中扩展了BeanPostProcessor的bean。
例如：
- AutowiredAnnotationBeanPostProcessor(处理被@Autowired注解修饰的bean并注入)
- RequiredAnnotationBeanPostProcessor(处理被@Required注解修饰的方法)
- CommonAnnotationBeanPostProcessor(处理@PreDestroy、@PostConstruct、@Resource等多个注解的作用)


### 7. initMessageSource()

初始化国际化资源处理器.不是主线代码忽略，没什么学习价值。
initMessageSource();

### 8. initApplicationEventMulticaster()
创建事件多播器,后续写

### 9. onRefresh()
模板方法，在容器刷新的时候可以自定义逻辑，不同的Spring容器做不同的事情。

### 10. registerListeners()

注册监听器，广播early application events,后续写

### 11. finishBeanFactoryInitialization(beanFactory)
实例化所有剩余的（非懒加载）单例
比如invokeBeanFactoryPostProcessors方法中根据各种注解解析出来的类，在这个时候都会被初始化。
实例化的过程各种BeanPostProcessor开始起作用。

后续写

### 12. finishRefresh()
最后容器刷新 发布刷新事件(Spring cloud也是从这里启动的)