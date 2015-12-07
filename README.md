# JavaLB 是基于grizzly的高性能负载均衡服务.
编译安装步骤:<br> 
  1、git clone 源码<br> 
  2、mvn install 编译安装<br> 
  3、copy target目录下lb-1.0.0.jar到JavaLB目录<br> 
  4、打开命令行: java -jar ./web port ./log4j.properties zkser_addr<br> 
  5、打开浏览器访问:http://127.0.0.1:port<br> 
