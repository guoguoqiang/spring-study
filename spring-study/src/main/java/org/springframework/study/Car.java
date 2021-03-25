package org.springframework.study;

import org.springframework.stereotype.Component;


/**
 * @description:
 * @author:ggq
 * @date: 2020/10/4 14:25
 */
//@ManagedBean
//@Named
//@Component
public class Car {

	private String name;

//	private Tank tank;

	public Car(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

//	public Tank getTank() {
//		return tank;
//	}
//
//	public void setTank(Tank tank) {
//		this.tank = tank;
//	}

	public Car() {
		System.out.println("car  加载");
	}
}
