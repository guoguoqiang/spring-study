1、Kotlin: warnings found and -Weeror specified

https://www.cxyzjd.com/article/qq_40333952/116425459

双击这个2个jar

![image-20210505143804502](https://img-blog.csdnimg.cn/img_convert/a7c93e2929696ff2c99163868ec5b81f.png)

2、Error:(350, 51) java: 找不到符号
  符号:   变量 CoroutinesUtils
  位置: 类 org.springframework.core.ReactiveAdapterRegistry.CoroutinesRegistrar



解决方法：
点击File -> Project Structure -> Libraries -> + -> Java，然后选择spring-framework/spring-core/kotlin-coroutines/build/libs/kotlin-coroutines-5.2.4.BUILD-SNAPSHOT.jar，在弹出的对话框中选择spring.spring-core.main，在重新build项目即可
————————————————
版权声明：本文为CSDN博主「蚂蚁要上天」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/gooaaee/article/details/104437902





3、/Users/guoqiangguo/Documents/GitHub/spring-study/spring-context/src/main/java/org/springframework/context/weaving/DefaultContextLoadTimeWeaver.java

Error:(26, 38) java: 找不到符号
  符号:   类 InstrumentationSavingAgent
  位置: 程序包 org.springframework.instrument



spring-context 把optional修改为compile

```
compile(project(":spring-instrument"))
```

