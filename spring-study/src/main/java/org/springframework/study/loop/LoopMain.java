//package org.springframework.study.loop;
//
//import org.springframework.beans.factory.ObjectFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.config.BeanDefinition;
//import org.springframework.beans.factory.support.RootBeanDefinition;
//
//import java.lang.reflect.Field;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * 模拟循环依赖
// * 代码不能运行
// *
// * @Author ggq
// * @Date 2021/3/17 11:24
// */
//public class LoopMain {
//
//	private static final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>(256);
//
//
//	/**
//	 * 一级缓存：用于存放完全初始化好的 bean
//	 **/
//	private static final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(256);
//
//	/**
//	 * 二级缓存：存放原始的 bean 对象（尚未填充属性），用于解决循环依赖
//	 * 为了将 实例化好的成熟Bean和未实例化的早期Bean 分离开
//	 */
//	private static final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>(16);
//
//	/**
//	 * 三级级缓存：存放 bean 工厂对象，用于解决循环依赖
//	 */
//	private static final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>(16);
//
//	/**
//	 * 当前正在创建Bean的标识
//	 */
//	private static Set<String> singletonCurrennlyInCreation = new HashSet<>();
//
//	public static void load() {
//		RootBeanDefinition a = new RootBeanDefinition(A.class);
//		RootBeanDefinition b = new RootBeanDefinition(B.class);
//		beanDefinitionMap.put("a", a);
//		beanDefinitionMap.put("b", b);
//	}
//
//	public static void main(String[] args) throws Exception {
//		load();
//		for (String key : beanDefinitionMap.keySet()) {
//			final Object bean = getBean(key);
//			System.out.println(bean);
//
//		}
//	}
//
//	// 获取bean
//	public static Object getBean(String beanName) throws Exception {
//
//		//  优化
//		Object object = getSingleton(beanName);
//		if (object != null) {
//			return object;
//		}
//
//		// 标记正在创建
//		if (!singletonCurrennlyInCreation.contains(beanName)) {
//			singletonCurrennlyInCreation.add(beanName);
//		}
//
//
//		RootBeanDefinition beanDefinition = (RootBeanDefinition) beanDefinitionMap.get(beanName);
//		// 实例化
//		final Class<?> beanClass = beanDefinition.getBeanClass();
//
//		Object instance = beanClass.newInstance();
//
//		// 三级缓存
////		singletonFactories.put(beanName,
////				() -> new JdkProxyBeanPostProcessor().getEarlyBeanReference(earlySingletonObjects.get(beanName), beanName));
//
//
//		// 属性赋值
//		// 获取所有的属性
//		final Field[] fields = beanClass.getDeclaredFields();
//		for (Field field : fields) {
//			final Autowired annotation = field.getAnnotation(Autowired.class);
//			// 说明属性上面有注解，就getBean
//			if (annotation != null) {
//				field.setAccessible(true);
//
//				// 获取属性的名字
//				final String name = field.getName();
//
//				final Object bean = getBean(name);
//
//				//  反射属性赋值
//				field.set(instance, bean);
//			}
//		}
//		// 在二级缓存中获取下 有可能的动态代理
//		if (earlySingletonObjects.containsKey(beanName)) {
//			instance = earlySingletonObjects.get(beanName);
//		}
//
//
//		// 添加到一级缓存
//		singletonObjects.put(beanName, instance);
//		// 初始化
//		return instance;
//	}
//
//	private static Object getSingleton(String beanName) {
//		Object bean = singletonObjects.get(beanName);
//
//		// 如果一级缓存中没有，并不标记正在创建
//		if (bean == null && singletonCurrennlyInCreation.contains(beanName)) {
//			bean = earlySingletonObjects.get(beanName);
//
//			// 如果二级缓存中没有，就从三级缓存中拿
//			if (bean == null) {
//
//				final ObjectFactory<?> objectFactory = singletonFactories.get(beanName);
//				if (objectFactory != null) {
//					final Object object = objectFactory.getObject();
//
//					earlySingletonObjects.put(beanName, object);
//				}
//			}
//
//		}
//		return bean;
//	}
//}
