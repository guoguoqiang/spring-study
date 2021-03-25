package org.springframework.study;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

/**
 * @description:
 * @author:ggq
 * @date: 2020/10/3 19:28
 */
@Configuration
@ComponentScan
@EnableAspectJAutoProxy
public class MainApp {

	@Bean
	public Car car() {
		final Car car = new Car("五菱");
//		car.setTank(tank());
		return car;
	}

//	@Bean
//	public Tank tank() {
//		return new Tank();
//	}

	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(MainApp.class);
		Car car = context.getBean("car", Car.class);

		// 测试 去掉@Configuration tank() 不会走cglib动态代理，每次调用都会创建
//		Car car2 = context.getBean("car", Car.class);
		System.out.println(car.getName());
//		UserServiceImpl bean = context.getBean("userServiceImpl", UserServiceImpl.class);
//		bean.sayHi();
	}
}
