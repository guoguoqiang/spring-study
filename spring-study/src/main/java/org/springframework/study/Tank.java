package org.springframework.study;

/**
 * @Author ggq
 * @Date 2021/3/16 13:28
 */
public class Tank {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Tank() {
		System.out.println("tank  加载。。。");
	}

}
