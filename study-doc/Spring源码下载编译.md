# 序言
`Spring`作为一个非常优秀的框架，值得每个java开发者学习一下，学习其优秀的设计思想，下面是我自己关于`Spring`源码的下载和编译

# 参考学习链接
- https://blog.csdn.net/weixin_43360548/article/details/108882029
- https://blog.csdn.net/baomw/article/details/83956300
- https://www.jianshu.com/p/74348b1a4421
- https://blog.csdn.net/a704397849/article/details/102754505

# 学习视频链接
- https://www.bilibili.com/video/BV1XJ41117tT?from=search&seid=13695484075934721207

# 下载

## Spring官网链接
https://spring.io/projects/spring-framework

## 环境
- maven环境
- gradle 使用源码自带的gradleWraper中的gradle版本（速度太慢），建议换成一样版本号gradle本地的
- JDK环境 我本地使用 jdk 1.8.0_131
- idea集成开发工具

## 下载编译

### 下载
1. 点击网页上的 github 小猫标志，进入spring-framework github源码地址
2. 点击 tags 版本标签
![image](https://note.youdao.com/yws/api/personal/file/26DAC874E44E4086A52E985CA0E5DC02?method=download&shareKey=5b277df1e8d13d2ce982571fea6378bd)
3. 推荐下载最新的RELEASE版本
 现在最新的版本为 v5.2.9.RELEASE
4. 解压
5. 用源码里面里面自带的gradle远程下载依赖
> 这是源码里面自带的D:\gitcode\spring-framework-5.2.9.RELEASE\gradle\wrapper\gradle-wrapper.properties里面的配置

```
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-5.6.4-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists

```

6. 修改build.gradle
> 打开build.gradle文件，全文找到repositories节点，会发现，里面有2个配置

原始配置
```
repositories {
			mavenCentral()
			maven { url "https://repo.spring.io/libs-spring-framework-build" }
		}
```

修改为：
```
		repositories {
			maven{ url 'https://maven.aliyun.com/nexus/content/groups/public/'}
			maven{ url 'https://maven.aliyun.com/nexus/content/repositories/jcenter'}
			mavenCentral()
			maven { url "https://repo.spring.io/libs-spring-framework-build" }
		}
```
### 编译
安装官网的方式编译：
> 使用gradlew（gradle-wrapper命令）  先编译oxm:compileTest Java：  Precompile spring-oxm with ./gradlew :spring-oxm:compileTestJava

1. 编译compileTestJava模块
> 打开spring源码文件夹的目录，输入 


```
gradlew :spring-oxm:compileTestJava
```
![image](https://note.youdao.com/yws/api/personal/file/01DEF632FF27480FA7BF9A6589C40093?method=download&shareKey=657c3f0e1381aaada7d86b4a63e993b6)

2. 下载速度太慢解决

修改gradle-wrapper.properties,修改同学分享的连接，或者修改为本地连接

```
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
#distributionUrl=https\://services.gradle.org/distributions/gradle-5.6.4-bin.zip
distributionUrl=http://scooper.top/wp-content/uploads/gradle-5.6.4-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists

```
或者
```
distributionUrl=file:///E:/soft/gradle-xx-bin.zip
```

分享百度云gradle-5.6.4-bin.zip文件


```
链接：https://pan.baidu.com/s/1baAgzSnsddXUNYiWO9ceEw 
提取码：7lph 
```

编译结果如下
![image](https://note.youdao.com/yws/api/personal/file/EA4D13D8E63642C8BB6474DEBECA1E43?method=download&shareKey=9469fa7c5162a0091408a68a1902fd98)

下面的报错不影响整体


# 将编译好的代码导入idea中


![image](https://note.youdao.com/yws/api/personal/file/1C06E48F98C64DF29FBD90182359ED64?method=download&shareKey=800620f79e80679a7ec29529327e38b9)


## 添加测试模块
![image](https://note.youdao.com/yws/api/personal/file/B88E6FC5A09B435995B137B554EE759B?method=download&shareKey=713107e250c64d0d73d195b22e14e19a)

![image](https://note.youdao.com/yws/api/personal/file/A3D3997B2FD144BEB2C8A70575CC2807?method=download&shareKey=1b7923d1d07fda119a70fad10c25ac91)

1. 添加依赖

```
dependencies {
    testCompile group: 'junit', name: 'junit', version: '4.12' 
    compile(project(":spring-context"))     
}
```
compile(project(":spring-context"))  代表本项目的
2. 随意添加任意bean:


```
@Service
public class UserServiceImpl {

	public void sayHi(){
		System.out.println("Hello Spring！");
	}
}
```

```
@Configuration
@ComponentScan
public class MainApp {
	public static void main(String[] args) {
		ApplicationContext context=new AnnotationConfigApplicationContext(MainApp.class);
		UserServiceImpl bean = context.getBean(UserServiceImpl.class);
		bean.sayHi();

	}
}

```

测试运行结果：
![image](https://note.youdao.com/yws/api/personal/file/4E5B8D2BF9A74C53B457CD8877E37838?method=download&shareKey=c372fdc748a30a4f7676f16b79dbc290)




