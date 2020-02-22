##网络工具

###被动反向代理

内网穿透，把内网的监听端口暴露到服务器上。

比如：
   * 在PC上监听80端口，但没有公网地址
   * 有一台公网的服务器 1.2.3.4
   
可以通过此工具把 80映射到 1.2.3.4:10002（代理端口）

这样，在外网访问 1.2.3.4:10002即可访问PC:80

使用方式：
   * 编译 mvn
   * NETUTIL="java -classpath target/netutil-1.0-SNAPSHOT.jar"
   * 在服务器1.2.3.4上开启控制服务 $NETUTIL com.wangdiao.App
      * LISTEN： 1.2.3.4:9999
   * 在PC上开启被动代理服务： $NETUTIL com.wangdiao.client.ControlClient 1.2.3.4 9999 10001 10002
      * 参数：控制服地址 控制服端口 注册端口 代理端口
      * 说明：代理端口供外网访问，如果HTTP可以用nginx再做反向代理到公网服务器的80
      * LISTEN: 1.2.3.4:10001 注册端
      * LISTEN: 1.2.3.4:10002 代理端
   * 在PC上注册需要代理的80端口：$NETUTIL com.wangdiao.client.RegisterClient 1.2.3.4 10001 localhost 80
      * 参数：注册地址 注册端口 被代理服地址 被代理服端口
      * 说明：被代理服不一定是PC本机，只要在PC上能访问到即可
   * 最后，访问1.2.3.4:10002即可访问到PC的80
   