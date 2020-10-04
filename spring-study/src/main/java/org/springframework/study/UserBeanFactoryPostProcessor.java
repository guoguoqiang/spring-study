package org.springframework.study;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author:ggq
 * @date: 2020/10/4 14:22
 */
@Component
public class UserBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// 获取Bean定义
		BeanDefinition beanDefinition = beanFactory.getBeanDefinition("car");
		// 修改
		beanDefinition.setBeanClassName("org.springframework.study.Person");

	}
}
