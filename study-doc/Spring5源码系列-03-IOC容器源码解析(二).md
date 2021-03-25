[TOC]



# 简介

Spring 容器类的加载成一个Bean，其中大概分成2大步，

第一步就是类解析成一个BeanDefinition（Bean定义），

第二步就是将BeanDefinition创建成一的Bean，经过实例化、属性赋值、初始化最终的一个Bean。

续写第二步

## 主要方法：`finishBeanFactoryInitialization(beanFactory)`

org.springframework.context.support.AbstractApplicationContext#finishBeanFactoryInitialization
# 知识笔记
## 1、Spring怎么解决循环依赖的

三级缓存

## 2、为什么要二级缓存和三级缓存

1、用一级缓存解决循环依赖的问题是：

无法保证多线程下的一级缓存Bean的完整性。

2、二级缓存只要是为了分离成熟Bean和纯净Bean(未注入属性)的存放， 防止多线程中在Bean还未创建完成时读取到的Bean时不完整的。所以也是为了保证我们getBean是完整最终的Bean，不会出现不完整的情况

3、三级缓存利用函数接口，将createBean的逻辑和getBean的逻辑解耦。

所以三级缓存解决循环依赖和处理多线程读取不完整的Bean都是最完美的解决方案。

## 3、Spring有没有解决构造函数的循环依赖

没有

## 4、Sring有没有解决多例下的循环依赖

多例不会存放在缓存中，直接抛异常

## 5、如果所有的bean实例化完成了，在哪里还可以在修改bean？

实现`SmartInitializingSingleton`接口

参考代码连接：org.springframework.beans.factory.support.DefaultListableBeanFactory#preInstantiateSingletons

## 6、spring能通过构造器和setter实现循环依赖的注入吗？

构造器不能通过循环依赖注入，可以通过setter注入方式构成的循环依赖

# 源码解析

## finishBeanFactoryInitialization源码解析

功能：实例化bean

### 一、beanFactory.freezeConfiguration()

冻结所有的 bean 定义 ， 说明注册的 bean 定义将不被修改或任何进一步的处理

### 二、beanFactory.preInstantiateSingletons();

源码：

```
public void preInstantiateSingletons() throws BeansException {
   if (logger.isTraceEnabled()) {
      logger.trace("Pre-instantiating singletons in " + this);
   }

   //获取我们容器中所有bean定义的名称
   List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

   //循环我们所有的bean定义名称
   for (String beanName : beanNames) {
      //合并我们的bean定义，转换为统一的RootBeanDefinition类型(在)， 方便后续处理
      RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
      /**
       * 根据bean定义判断是不是抽象的&& 不是单例的 &&不是懒加载的
       */
      if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
         //是不是工厂bean
         if (isFactoryBean(beanName)) {
            // 是factoryBean会先生成实际的bean  &beanName 是用来获取实际bean的
            Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
            if (bean instanceof FactoryBean) {
               FactoryBean<?> factory = (FactoryBean<?>) bean;
               boolean isEagerInit;
               if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
                  isEagerInit = AccessController.doPrivileged(
                        (PrivilegedAction<Boolean>) ((SmartFactoryBean<?>) factory)::isEagerInit,
                        getAccessControlContext());
               }
               else {
                  isEagerInit = (factory instanceof SmartFactoryBean &&
                        ((SmartFactoryBean<?>) factory).isEagerInit());
               }
               //调用真正的getBean的流程
               if (isEagerInit) {
                  getBean(beanName);
               }
            }
         }
         else {
            //非工厂Bean 就是普通的bean
            getBean(beanName);
         }
      }
   }

   //或有的bean的名称 ...........到这里所有的单实例的bean已经记载到单实例bean到缓存中
   for (String beanName : beanNames) {
      //从单例缓存池中获取所有的对象
      Object singletonInstance = getSingleton(beanName);
      //判断当前的bean是否实现了SmartInitializingSingleton接口
      if (singletonInstance instanceof SmartInitializingSingleton) {
         SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
         if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
               smartSingleton.afterSingletonsInstantiated();
               return null;
            }, getAccessControlContext());
         }
         else {
            //触发实例化之后的方法afterSingletonsInstantiated
            smartSingleton.afterSingletonsInstantiated();
         }
      }
   }
}
```

实例化剩余的单实例bean

#### 1、先循环所有`bean定义的名称集合`

![image-20210325100833622](https://cdn.jsdelivr.net/gh/guoguoqiang/image/spring/image-20210325100833622.png)



设置条件断点，看`car`的实例化过程

![image-20210325101656527](https://cdn.jsdelivr.net/gh/guoguoqiang/image/spring/image-20210325101656527.png)

#### 2、getMergedLocalBeanDefinition(beanName);

合并我们的bean定义，转换为统一的RootBeanDefinition类型(在)， 方便后续处理



#### 3、!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()

对bean实例化的条件：

不是抽象的&& 不是单例的 &&不是懒加载的



#### 4、isFactoryBean(beanName)

是不是工厂bean，我们没有实现 `FactoryBean`接口，所以不是工厂bean

![image-20210325102051002](https://cdn.jsdelivr.net/gh/guoguoqiang/image/spring/image-20210325102051002.png)

#### 5、getBean(beanName);

非工厂Bean 就是普通的bean，获取bean



##### 5.1、transformedBeanName

或许很多人不理解转换对应的`beanName`是什么意思，传入的参数name不就是`beanName`吗？其实不是，这里传入的参数可能是别名，也可能是FactoryBean，所以需要进行一系列的解析，这些解析内容包括如下内容

- 去除FactoryBean的修饰符，也就是如果name=‘’‘&aa’，那么就会首先去除&而使name=‘‘aa’’
- 取指定alias所表示的最终beanName，例如别名A指向名称为B的bean则返回B；若别名A指向别名B，别名B又指向名称为C的bean则返回C。

去除工厂Bean的前缀(&)，我们这里不是工厂bean，就直接返回了

##### 5.2、getSingleton(beanName)

尝试从缓存中加载单例
单例在Spring的同一个容器内只会被创建一次，后续再获取bean，就直接从单例缓存中获取了，当然这里也只是尝试加载，
首先尝试从缓存中加载，如果加载不成功，则再次尝试从singletonFactories中加载。
因为在创建单例bean的时候会存在依赖注入的情况，而在创建依赖的时候为了避免循环依赖，在Spring中创建bean的原则是
不等bean创建完成就会将创建bean的ObjectFactory提早曝光加入缓存中，一旦下一个bean创建时候需要依赖上一个bean则直接
使用ObjectFactory

```
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
   /**
    * 第一步:我们尝试去一级缓存(单例缓存池中去获取对象,一般情况从该map中获取的对象是直接可以使用的)
    * IOC容器初始化加载单实例bean的时候第一次进来的时候 该map中一般返回空
    */
   Object singletonObject = this.singletonObjects.get(beanName);
   /**
    * 若在第一级缓存中没有获取到对象,并且singletonsCurrentlyInCreation这个list包含该beanName
    * IOC容器初始化加载单实例bean的时候第一次进来的时候 该list中一般返回空,但是循环依赖的时候可以满足该条件
    */
   if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
      /**
       * 二级缓存
       */
      singletonObject = this.earlySingletonObjects.get(beanName);
      if (singletonObject == null && allowEarlyReference) {
         synchronized (this.singletonObjects) {

            singletonObject = this.singletonObjects.get(beanName);
            /**
             * 二级缓存中也没有获取到对象,allowEarlyReference为true(参数是有上一个方法传递进来的true)
             */
            if (singletonObject == null) {
               /**
                * 尝试去二级缓存中获取对象(二级缓存中的对象是一个早期对象)
                * 何为早期对象:就是bean刚刚调用了构造方法，还来不及给bean的属性进行赋值的对象(纯净态)
                * 就是早期对象
                */
               singletonObject = this.earlySingletonObjects.get(beanName);
               //从三级缓存中获取到对象不为空
               if (singletonObject == null) {
                  /**
                   * 直接从三级缓存中获取 ObjectFactory对象 这个对接就是用来解决循环依赖的关键所在
                   * 在ioc后期的过程中,当bean调用了构造方法的时候,把早期对象包裹成一个ObjectFactory
                   * 暴露到三级缓存中
                   */
                  ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                  if (singletonFactory != null) {

                     /**
                      * 在这里通过暴露的ObjectFactory 包装对象中,通过调用他的getObject()来获取我们的早期对象
                      * 在这个环节中会调用到 getEarlyBeanReference()来进行后置处理
                      */
                     singletonObject = singletonFactory.getObject();
                     this.earlySingletonObjects.put(beanName, singletonObject);
                     this.singletonFactories.remove(beanName);
                  }
               }
            }
         }
      }
   }
   return singletonObject;
}
```

##### 5.3、isPrototypeCurrentlyInCreation(beanName)

如果没有在一级缓存中获取到bean，就会去创建bean

首先判断`prototypesCurrentlyInCreation`是否是原型bean，如果是，则抛出异常

```
/** 用户保存原型对象正常创建的beanName的一个set 用于保存正在创建原型bean的name */
private final ThreadLocal<Object> prototypesCurrentlyInCreation =
      new NamedThreadLocal<>("Prototype beans currently in creation");
```

##### 5.4、getParentBeanFactory()

判断AbstractBeanFacotry工厂是否有父工厂(一般情况下是没有父工厂因为abstractBeanFactory直接是抽象类,不存在父工厂)

一般情况下,只有Spring 和SpringMvc整合的时才会有父子容器的概念,

比如我们的Controller中注入Service的时候，发现我们依赖的是一个引用对象，那么他就会调用getBean去把service找出来

但是当前所在的容器是web子容器，那么就会在这里的 先去父容器找

##### 5.5、markBeanAsCreated(beanName);

```
/** 用于保存已经创建好的Bean */
private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));
```

```
protected void markBeanAsCreated(String beanName) {
   //没有创建
   if (!this.alreadyCreated.contains(beanName)) {
      //全局加锁
      synchronized (this.mergedBeanDefinitions) {
         //再次检查一次：DCL 双检查模式
         if (!this.alreadyCreated.contains(beanName)) {
            //从 mergedBeanDefinitions 中删除 beanName，并在下次访问时重新创建它
            clearMergedBeanDefinition(beanName);
            // 添加到已创建 bean 集合中
            this.alreadyCreated.add(beanName);
         }
      }
   }
}
```

##### 5.6、checkMergedBeanDefinition(mbd, beanName, args);

检查当前创建的bean定义是不是抽象的bean定义



##### 5.7、getSingleton

```
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
   Assert.notNull(beanName, "Bean name must not be null");
   synchronized (this.singletonObjects) {
      //尝试从单例缓存池中获取对象
      Object singletonObject = this.singletonObjects.get(beanName);
      if (singletonObject == null) {
         if (this.singletonsCurrentlyInDestruction) {
            throw new BeanCreationNotAllowedException(beanName,
                  "Singleton bean creation not allowed while singletons of this factory are in destruction " +
                  "(Do not request a bean from a BeanFactory in a destroy method implementation!)");
         }
         if (logger.isDebugEnabled()) {
            logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
         }
         /**
          * 标记当前的bean马上就要被创建了
          * singletonsCurrentlyInCreation 在这里会把beanName加入进来，若第二次循环依赖（构造器注入会抛出异常）
          */
         beforeSingletonCreation(beanName);
         boolean newSingleton = false;
         boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
         if (recordSuppressedExceptions) {
            this.suppressedExceptions = new LinkedHashSet<>();
         }
         try {
            //<3> 初始化 bean
            // 这个过程其实是调用 createBean() 方法
            singletonObject = singletonFactory.getObject();
            newSingleton = true;
         }
         catch (IllegalStateException ex) {
            //回调我们singletonObjects的get方法,进行正在的创建bean的逻辑
            singletonObject = this.singletonObjects.get(beanName);
            if (singletonObject == null) {
               throw ex;
            }
         }
         catch (BeanCreationException ex) {
            if (recordSuppressedExceptions) {
               for (Exception suppressedException : this.suppressedExceptions) {
                  ex.addRelatedCause(suppressedException);
               }
            }
            throw ex;
         }
         finally {
            if (recordSuppressedExceptions) {
               this.suppressedExceptions = null;
            }
            // <4> 后置处理
            //主要做的事情就是把singletonsCurrentlyInCreation标记正在创建的bean从集合中移除
            afterSingletonCreation(beanName);
         }
         if (newSingleton) {
            // <5> 加入缓存中
            addSingleton(beanName, singletonObject);
         }
      }
      return singletonObject;
   }
}
```

###### 5.7.1、singletonObject = singletonFactory.getObject()

函数式接口，创建bean



##### 5.8、createBean(beanName, mbd, args)

```
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
      throws BeanCreationException {

   if (logger.isTraceEnabled()) {
      logger.trace("Creating instance of bean '" + beanName + "'");
   }
   RootBeanDefinition mbdToUse = mbd;

   // 确保此时的 bean 已经被解析了
   Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
   if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
      mbdToUse = new RootBeanDefinition(mbd);
      mbdToUse.setBeanClass(resolvedClass);
   }

   // Prepare method overrides.
   try {
      /**
       * 验证和准备覆盖方法( 仅在XML方式中）
       * lookup-method 和 replace-method
       * 这两个配置存放在 BeanDefinition 中的 methodOverrides( 仅在XML方式中）
       * 在XML方式中 bean 实例化的过程中如果检测到存在 methodOverrides ，
       * 则会动态地位为当前 bean 生成代理并使用对应的拦截器为 bean 做增强处理。
       * 具体的实现我们后续分析，现在先看 mbdToUse.prepareMethodOverrides() 代码块
       */
      mbdToUse.prepareMethodOverrides();
   }
   catch (BeanDefinitionValidationException ex) {
      throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
            beanName, "Validation of method overrides failed", ex);
   }

   try {
      /**
       * 第1个bean后置处理器
       * 通过bean的后置处理器来进行后置处理生成代理对象,一般情况下在此处不会生成代理对象
       * 为什么不能生成代理对象,不管是我们的jdk代理还是cglib代理都不会在此处进行代理，因为我们的
       * 真实的对象没有生成,所以在这里不会生成代理对象，那么在这一步是我们aop和事务的关键，因为在这里
       * 解析我们的aop切面信息进行缓存
       */
      Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
      if (bean != null) {
         return bean;
      }
   }
   catch (Throwable ex) {
      throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
            "BeanPostProcessor before instantiation of bean failed", ex);
   }

   try {
      /**
       * 该步骤是我们真正的创建我们的bean的实例对象的过程
       */
      Object beanInstance = doCreateBean(beanName, mbdToUse, args);
      if (logger.isTraceEnabled()) {
         logger.trace("Finished creating instance of bean '" + beanName + "'");
      }
      return beanInstance;
   }
   catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
      // A previously detected exception with proper bean creation context already,
      // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
      throw ex;
   }
   catch (Throwable ex) {
      throw new BeanCreationException(
            mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
   }
}
```



###### 5.8.1、resolveBeforeInstantiation(beanName, mbdToUse);

 第1个bean后置处理器
通过bean的后置处理器来进行后置处理生成代理对象,一般情况下在此处不会生成代理对象
 为什么不能生成代理对象,不管是我们的jdk代理还是cglib代理都不会在此处进行代理，因为我们的
 真实的对象没有生成,所以在这里不会生成代理对象，那么在这一步是我们aop和事务的关键，因为在这里
 解析我们的aop切面信息进行缓存

```
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
   Object bean = null;
   if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
      //判断容器中是否有InstantiationAwareBeanPostProcessors
      if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
         //获取当前bean 的class对象
         Class<?> targetType = determineTargetType(beanName, mbd);
         if (targetType != null) {
            /**
             * 后置处理器的【第一次】调用 总共有九处调用 事务在这里不会被调用，aop的才会被调用
             * 为啥aop在这里调用了，因为在此处需要解析出对应的切面报错到缓存中
             */
            bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
            //若InstantiationAwareBeanPostProcessors后置处理器的postProcessBeforeInstantiation返回不为null

            //说明生成了代理对象那么我们就调用
            if (bean != null) {
               /**
                * 后置处理器的第二处调用，该后置处理器若被调用的话，那么第一处的处理器肯定返回的不是null
                * InstantiationAwareBeanPostProcessors后置处理器postProcessAfterInitialization
                */
               bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
            }
         }
      }
      mbd.beforeInstantiationResolved = (bean != null);
   }
   return bean;
}
```



###### 5.8.2、applyBeanPostProcessorsAfterInitialization(bean, beanName);

```
protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
   for (BeanPostProcessor bp : getBeanPostProcessors()) {
      if (bp instanceof InstantiationAwareBeanPostProcessor) {
         InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
         Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
         if (result != null) {
            return result;
         }
      }
   }
   return null;
}
```



##### 5.9、doCreateBean(beanName, mbdToUse, args)

该步骤是我们真正的创建我们的bean的实例对象的过程

```
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
      throws BeanCreationException {

   //BeanWrapper 是对 Bean 的包装，其接口中所定义的功能很简单包括设置获取被包装的对象，获取被包装 bean 的属性描述器
   BeanWrapper instanceWrapper = null;
   if (mbd.isSingleton()) {
      //从没有完成的FactoryBean中移除
      instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
   }
   if (instanceWrapper == null) {
      //创建bean实例化 使用合适的实例化策略来创建新的实例：工厂方法、构造函数自动注入、简单初始化 该方法很复杂也很重要
      instanceWrapper = createBeanInstance(beanName, mbd, args);
   }
   //从beanWrapper中获取我们的早期对象
   Object bean = instanceWrapper.getWrappedInstance();
   Class<?> beanType = instanceWrapper.getWrappedClass();
   if (beanType != NullBean.class) {
      mbd.resolvedTargetType = beanType;
   }

   // Allow post-processors to modify the merged bean definition.
   synchronized (mbd.postProcessingLock) {
      if (!mbd.postProcessed) {
         try {
            //进行后置处理 @AutoWired @Value的注解的预解析
            applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
         }
         catch (Throwable ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                  "Post-processing of merged bean definition failed", ex);
         }
         mbd.postProcessed = true;
      }
   }

   /**
    * 缓存单例到三级缓存中，以防循环依赖
    * 判断是否早期引用的Bean,如果是，则允许提前暴露引用
    * 判断是否能够暴露早期对象的条件:
    * 是否单例
    * 是否允许循环依赖
    * 是否正在创建的Bean
    */
   boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
         isSingletonCurrentlyInCreation(beanName));
   //上述条件满足，允许中期暴露对象
   if (earlySingletonExposure) {
      if (logger.isTraceEnabled()) {
         logger.trace("Eagerly caching bean '" + beanName +
               "' to allow for resolving potential circular references");
      }
      //把我们的早期对象包装成一个singletonFactory对象 该对象提供了一个getObject方法,该方法内部调用getEarlyBeanReference方法
      addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
   }

   // Initialize the bean instance.
   Object exposedObject = bean;
   try {
      //属性赋值 给我们的属性进行赋值(调用set方法进行赋值)
      populateBean(beanName, mbd, instanceWrapper);
      //进行对象初始化操作(在这里可能生成代理对象)
      exposedObject = initializeBean(beanName, exposedObject, mbd);
   }
   catch (Throwable ex) {
      if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
         throw (BeanCreationException) ex;
      }
      else {
         throw new BeanCreationException(
               mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
      }
   }
   // 是早期对象暴露
   if (earlySingletonExposure) {
      /**
       * 去缓存中获取到我们的对象 由于传递的allowEarlyReference 是false 要求只能在一级二级缓存中去获取
       * 正常普通的bean(不存在循环依赖的bean) 创建的过程中，压根不会把三级缓存提升到二级缓存中
       */
      Object earlySingletonReference = getSingleton(beanName, false);
      //能够获取到
      if (earlySingletonReference != null) {
         //经过后置处理的bean和早期的bean引用还相等的话(表示当前的bean没有被代理过)
         if (exposedObject == bean) {
            exposedObject = earlySingletonReference;
         }
         //处理依赖的bean
         else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
            String[] dependentBeans = getDependentBeans(beanName);
            Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
            for (String dependentBean : dependentBeans) {
               if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                  actualDependentBeans.add(dependentBean);
               }
            }
            if (!actualDependentBeans.isEmpty()) {
               throw new BeanCurrentlyInCreationException(beanName,
                     "Bean with name '" + beanName + "' has been injected into other beans [" +
                     StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                     "] in its raw version as part of a circular reference, but has eventually been " +
                     "wrapped. This means that said other beans do not use the final version of the " +
                     "bean. This is often the result of over-eager type matching - consider using " +
                     "'getBeanNamesForType' with the 'allowEagerInit' flag turned off, for example.");
            }
         }
      }
   }

   // Register bean as disposable.
   try {
      //注册销毁的bean的销毁接口
      registerDisposableBeanIfNecessary(beanName, bean, mbd);
   }
   catch (BeanDefinitionValidationException ex) {
      throw new BeanCreationException(
            mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
   }

   return exposedObject;
}
```



###### 5.9.1、createBeanInstance(beanName, mbd, args)

创建bean实例化 使用合适的实例化策略来创建新的实例：工厂方法、构造函数自动注入、简单初始化 该方法很复杂也很重要

org.springframework.beans.factory.support.SimpleInstantiationStrategy#instantiate(org.springframework.beans.factory.support.RootBeanDefinition, java.lang.String, org.springframework.beans.factory.BeanFactory, java.lang.Object, java.lang.reflect.Method, java.lang.Object...)

通过发射获取到bean

![image-20210325111450760](https://cdn.jsdelivr.net/gh/guoguoqiang/image/spring/image-20210325111450760.png)

###### 5.9.2、applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);

进行后置处理 @AutoWired @Value的注解的预解析



###### 5.9.3、addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));

把我们的早期对象包装成一个singletonFactory对象 该对象提供了一个getObject方法,该方法内部调用getEarlyBeanReference方法

```
protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
   Assert.notNull(singletonFactory, "Singleton factory must not be null");
   synchronized (this.singletonObjects) {
      if (!this.singletonObjects.containsKey(beanName)) {
         this.singletonFactories.put(beanName, singletonFactory);
         this.earlySingletonObjects.remove(beanName);
         this.registeredSingletons.add(beanName);
      }
   }
}
```



##### 5.10、populateBean(beanName, mbd, instanceWrapper);

属性赋值 给我们的属性进行赋值(调用set方法进行赋值)

##### 5.11、initializeBean(beanName, exposedObject, mbd);

进行对象初始化操作(在这里可能生成代理对象)

###### 5.11.1、invokeAwareMethods(beanName, bean)

若我们的bean实现了XXXAware接口进行方法的回调

###### 5.11.2、applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName)

调用我们的bean的后置处理器的postProcessorsBeforeInitialization方法  @PostCust注解的方法

###### 5.11.3、invokeInitMethods(beanName, wrappedBean, mbd)

调用初始化方法

###### 5.11.4、applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName)

调用我们bean的后置处理器的PostProcessorsAfterInitialization方法

```
public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException {

		Object result = existingBean;
		//获取我们容器中的所有的bean的后置处理器
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			/**
			 * 在这里是后置处理器的【第九次调用】 aop和事务都会在这里生存代理对象
			 *
			 * 【很重要】
			 * 我们AOP @EnableAspectJAutoProxy 为我们容器中导入了 AnnotationAwareAspectJAutoProxyCreator
			 * 我们事务注解@EnableTransactionManagement 为我们的容器导入了 InfrastructureAdvisorAutoProxyCreator
			 * 都是实现了我们的 BeanPostProcessor接口,InstantiationAwareBeanPostProcessor,
			 * 在这里实现的是BeanPostProcessor接口的postProcessAfterInitialization来生成我们的代理对象
			 */
			Object current = processor.postProcessAfterInitialization(result, beanName);
			//若只有有一个返回null 那么直接返回原始的
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}
```

##### 5.12、getSingleton(beanName, false);

 去缓存中获取到我们的对象 由于传递的allowEarlyReference 是false 要求只能在一级二级缓存中去获取
 正常普通的bean(不存在循环依赖的bean) 创建的过程中，压根不会把三级缓存提升到二级缓存中



##### 5.13、registerDisposableBeanIfNecessary(beanName, bean, mbd);

注册销毁的bean的销毁接口



##### 5.15、afterSingletonCreation(beanName);

主要做的事情就是把singletonsCurrentlyInCreation标记正在创建的bean从集合中移除



##### 5.16、addSingleton(beanName, singletonObject)

加入缓存中

```
protected void addSingleton(String beanName, Object singletonObject) {
   synchronized (this.singletonObjects) {
      //加入到单例缓存池中
      this.singletonObjects.put(beanName, singletonObject);
      //从三级缓存中移除(针对的不是处理循环依赖的)
      this.singletonFactories.remove(beanName);
      //从二级缓存中移除(循环依赖的时候 早期对象存在于二级缓存)
      this.earlySingletonObjects.remove(beanName);
      //用来记录保存已经处理的bean
      this.registeredSingletons.add(beanName);
   }
}
```



#### 6、if (singletonInstance instanceof SmartInitializingSingleton)

到这里所有的单实例的bean已经记载到单实例bean到缓存中

判断当前的bean是否实现了SmartInitializingSingleton接口

如果需要在bean创建完成之后再需要对bean修改，就实现`SmartInitializingSingleton`接口

![image-20210325132839105](https://cdn.jsdelivr.net/gh/guoguoqiang/image/spring/image-20210325132839105.png)



# 参考学习

https://segmentfault.com/a/1190000015221968

http://www.iflym.com/index.php/code/201208280001.html

Spring（第二版）源码深度解析  作者 郝佳



