# 一、windows机配置
````
修改 C:\Windows\System32\drivers\etc 下的 hosts文件
添加映射
# gulimall
192.168.48.129(虚拟机ip) gulimall.com
````
# 二、Nginx+网关+openFeign的逻辑
````
要实现的逻辑：本机浏览器请求gulimall.com，通过配置hosts文件之后，那么当你在浏览器中输入gulimall.com的时候，相当于域名解析DNS服务解析得到ip 192.168.56.10，也就是并不是访问java服务，而是先去找nginx。什么意思呢？是说如果某一天项目上线了，gulimall.com应该是nginx的ip，用户访问的都是nginx

请求到了nginx之后，

    如果是静态资源/static/*直接在nginx服务器中找到静态资源直接返回。
    如果不是静态资源/（他配置在/static/*的后面所以才优先级低），nginx把他upstream转交给另外一个ip 192.168.56.1:88这个ip端口是网关gateway。

到达网关之后，通过url信息断言判断应该转发给nacos中的哪个微服务（在给nacos之前也可以重写url），这样就得到了响应

而对于openFeign，因为在服务中注册了nacos的ip，所以他并不经过nginx
````
# 三、Nginx+网关配置
## 1、修改nginx.conf, 配置上游服务器(网关模块)
````
cd /mydata/nginx/conf

vi nginx.conf 
http {
    ...
    upstream gulimall{
      server 本机ip:88;
    }
    include /etc/nginx/conf.d/*.conf;
}
````
## 2、新建gulimall.conf
````
cd /mydata/nginx/conf/conf.d

cp default.conf gulimall.conf

vi gulimall.conf 
server {
    listen       80;
    server_name  gulimall.com; #监听gulimall.com:80

    #charset koi8-r;
    #access_log  /var/log/nginx/log/host.access.log  main;

    location / { #接收到gulimall.com的访问后，如果是/，转交给指定的upstream
       proxy_pass http://gulimall; #请求映射到上游服务器(网关)
       proxy_set_header Host $host; #由于nginx的转发会丢失host头，造成网关不知道原host，所以我们添加头信息
    }

    ....
}
````
## 3、新建路由规则
````
配置gateway为服务器，将域名为**.gulimall.com转发至商品服务。配置的时候注意 网关优先匹配的原则，所以要把这个配置放到后面

- id: gulimall_host_route
  uri: lb://gulimall-product
  predicates:
    - Host=**.gulimall.com
````
