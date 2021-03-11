# 1、问题

​	因为spring 源码5系列是在家里笔记本上面下载编译的，之后就上传到了github，但是用电脑下载导入IDEA之后，却一直编译错误。

![image-20210311143722117](https://cdn.jsdelivr.net/gh/guoguoqiang/image/spring/image-20210311143722117.png)



# 2、解决

​	重新build，clean 无果后，我又新建了一个模块 `study-test`，把前面的部分代码复制过来，运行后，竟然可以。

![image-20210311144003993](https://cdn.jsdelivr.net/gh/guoguoqiang/image/spring/image-20210311144003993.png)



于是查看了，新建模块修改的 `settings.gradle`

![image-20210311144149144](https://cdn.jsdelivr.net/gh/guoguoqiang/image/spring/image-20210311144149144.png)



和之前新建的模块区别是 include 在最下面



接着,我把 `include 'spring-study'`也放在了下面

![image-20210311144330551](https://cdn.jsdelivr.net/gh/guoguoqiang/image/spring/image-20210311144330551.png)



运行成功

![image-20210311144501005](https://cdn.jsdelivr.net/gh/guoguoqiang/image/spring/image-20210311144501005.png)



IDEA版本号：IntelliJ IDEA 2018.3.6

JDK：jdk-8u112-windows-x64

