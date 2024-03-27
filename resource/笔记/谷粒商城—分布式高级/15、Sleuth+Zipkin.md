# 一、基本术语
````
Span（跨度）：基本工作单元，发送一个远程调度任务 就会产生一个 Span，Span 是一个 64 位 ID 唯一标识的，Trace 是用另一个 64 位 ID 唯一标识的，Span 还有其他数据信息，比如摘要、时间戳事件、Span 的 ID、以及进度 ID。
Trace（跟踪）：一系列 Span 组成的一个树状结构。请求一个微服务系统的 API 接口，这个 API 接口，需要调用多个微服务，调用每个微服务都会产生一个新的 Span，所有由这个请求产生的 Span 组成了这个 Trace。
Annotation（标注）：用来及时记录一个事件的，一些核心注解用来定义一个请求的开始和结束 。这些注解包括以下：

    cs - Client Sent -客户端发送一个请求，这个注解描述了这个 Span 的开始
    sr - Server Received -服务端获得请求并准备开始处理它，用 sr 减去 cs 时间戳便可得到网络传输的时间。
    ss - Server Sent （服务端发送响应）–该注解表明请求处理的完成(当请求返回客户端)，如果 ss 的时间戳减去 sr 时间戳，就可以得到服务器请求的时间。
    cr - Client Received （客户端接收响应）-此时 Span 的结束，如果 cr 的时间戳减去 cs 时间戳便可以得到整个请求所消耗的时间
````
# 二、整合Zipkin
````
1、docker 安装 zipkin
docker run -d -p 9411:9411 openzipkin/zipkin

2、pom依赖
<!--zipkin 依赖也同时包含了 sleuth，可以省略 sleuth 的引用-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zipkin</artifactId>
</dependency>

3、application.yml
spring:
  zipkin:
    base-url: http://192.168.48.129:9411/	# zipkin 服务器的地址
    discovery-client-enabled: false # 关闭服务发现，否则 Spring Cloud 会把 zipkin 的 url 当做服务名称
    sender:
      type: web # 设置使用 http 的方式传输数据
  sleuth:
    sampler:
      probability: 1  # 设置抽样采集率为 100% ，默认为 0.1 ，即 10%
````
