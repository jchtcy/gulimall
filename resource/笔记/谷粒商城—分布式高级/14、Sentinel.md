# 一、SpringBoot整合Sentinel
````
1、common模块 pom文件引入
<!--sentinel-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>

2、下载对应的sentinel版本 本项目为1.7.1
java -jar sentinel-dashboard-1.7.1.jar --server.port=8070

3、秒杀服务修改配置文件 application.properties
# sentinel
# sentinel的地址
spring.cloud.sentinel.transport.dashboard=127.0.0.1:8070
# 该微服务与sentinel连接的端口
spring.cloud.sentinel.transport.port=8719
# 暴露应用信息
management.endpoints.web.exposure.include='*'

4、秒杀服务新建 SentinelUrlBlockHandler 自定义限流返回信息
````
# 二、所有服务引入sentinel
````
修改配置文件 application.properties
# sentinel
# sentinel的地址
spring.cloud.sentinel.transport.dashboard=127.0.0.1:8070
# 暴露应用信息
management.endpoints.web.exposure.include='*'
# 开启sentinel熔断保护
feign.sentinel.enabled=true
````
# 三、熔断降级
````
1、调用方(product)修改配置
feign.sentinel.enabled=true

2、product 新建SeckillFeignServiceFallBack降级方法

3、修改商品服务中的被调用方(SeckillFeignService) 指定降级方法

4、上面是调用方做降级处理, 调用方做降级是局部的, 提供方做降级是全局降级
````
# 四、自定义受保护资源
````
1、基于 try catch

修改SeckillService的getCurrentSeckillSkus方法

try(Entry entry = SphU.entry("seckillSkus")) {
    // 受保护的逻辑
} catch (BlockException e) {
    log.error("资源被限流" + e.getMessage());
}

2、基于注解(方便)

public List<SeckillSkuRedisTo> blockHandler(BlockException e) {

    log.error("getCurrentSeckillSkusResource资源被限流了.");
    return null;
}
    
@SentinelResource(value = "getCurrentSeckillSkusResource", blockHandler = "blockHandler")
@Override
public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {

3、blockHandler 函数会在原方法被限流/降级/系统保护时调用, 而fallback函数会针对所有类型的异常
````
# 五、sentinel整合网关
````
1、网关导入依赖
<!-- sentinel整合网关-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>
    <version>2.2.1.RELEASE</version>
</dependency>

2、自定义网关回调 SentinelGatewayConfig
````
