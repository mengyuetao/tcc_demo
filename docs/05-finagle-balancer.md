## Finagle 源码分析之 Balancer


- Author: Yuetao Meng
- Mail: mfty1980@sina.com
- Date: 2018-2-8


模块代码位置

```
 com.twitter.finagle.loadbalancer.LoadBalancerFactory
```

负载均衡为两级结构，流程如下：

* 根据 ```Var[Addr]``` 创建 Endpoint  
* 按照weight把Endpoint分组，不同的组创建 Balancer
* 按照weight按比例把请求分配到不同的Balancer，Blancer在分配到不同的Endpointer



```
Var[Addr] Addr 表示服务地址列表，用Var[T] 表示，说明地址可以实时变化
Var[Addr] -> Var[Activity.State[Set[Address]]
->Activity(Var[Activity.State[Set[Address]])
->Event[Activity.State[Set[Address]]]


com.twitter.finagle.loadbalancerTrafficDistributor

监听 Event[Activity.State[Set[Address]]]
并且调用 newEndpoint ， newLoadBalancer
同时对进行了Cache，
避免 newEndpoint ， newLoadBalancer 不必要的调用，这两个调用资源消耗较大

```

Balancer 会过滤掉无效的连接

Balancer 有多个实现，p2c等，每个Blancer都包含Distributor
pick（）方法过滤掉无效的连接（ServiceFactory），如果没有连接可用
那吗将执行 rebuild Distributor，同步 ServiceFactory 最新状态并重试
