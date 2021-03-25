//package org.springframework.study.loop;
//
//import org.springframework.beans.BeansException;
//import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
//import org.springframework.stereotype.Component;
//
///**
// * @Author ggq
// * @Date 2021/3/17 14:41
// */
////@Component
//public class JdkProxyBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor {
//
//	@Override
//	public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
//		if(bean instanceof A){
//			JdkDynimcProxy jdkDynimcProxy = new JdkDynimcProxy(bean);
//			return jdkDynimcProxy.getProxy();
//		}
//		return bean;
//	}
//}
