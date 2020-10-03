package org.springframework.study;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.study.config.ContextConfig;

/**
 * @description:
 * @author:ggq
 * @date: 2020/10/3 14:50
 */
public class MainApp {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context=new AnnotationConfigApplicationContext(ContextConfig.class);
		IHelloWorld iHelloWorld = context.getBean(IHelloWorld.class);
		iHelloWorld.say();

	}
}
