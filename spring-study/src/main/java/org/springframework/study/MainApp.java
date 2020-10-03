package org.springframework.study;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author:ggq
 * @date: 2020/10/3 19:28
 */
@Configuration
@ComponentScan
public class MainApp {
	public static void main(String[] args) {
		ApplicationContext context=new AnnotationConfigApplicationContext(MainApp.class);
		UserServiceImpl bean = context.getBean(UserServiceImpl.class);
		bean.sayHi();

	}
}
