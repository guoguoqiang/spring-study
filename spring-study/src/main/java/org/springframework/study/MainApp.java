package org.springframework.study;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
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

	@Bean
	public Car car(){
		return new Car("五菱");
	}
	public static void main(String[] args) {
		ApplicationContext context=new AnnotationConfigApplicationContext(MainApp.class);
		Car car = context.getBean("car", Car.class);
		System.out.println(car.getName());
//		UserServiceImpl bean = context.getBean("userServiceImpl",UserServiceImpl.class);
//		bean.sayHi();

	}
}
