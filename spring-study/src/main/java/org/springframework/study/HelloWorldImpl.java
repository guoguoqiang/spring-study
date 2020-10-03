package org.springframework.study;

import org.springframework.stereotype.Service;

/**
 * @description:
 * @author:ggq
 * @date: 2020/10/3 16:03
 */
@Service
public class HelloWorldImpl implements IHelloWorld{
	@Override
	public void say() {
		System.out.println("hello world!");
	}
}
