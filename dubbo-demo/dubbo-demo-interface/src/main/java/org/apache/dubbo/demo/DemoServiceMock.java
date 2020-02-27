package org.apache.dubbo.demo;

import org.apache.dubbo.demo.DemoService;

import java.util.concurrent.CompletableFuture;

/**
 * @author <a href="http://youngitman.tech">青年IT男</a>
 * @version v1.0.0
 * @className DemoServiceMock
 * @description
 *
 * 1.默认情况当远程服务调用异常返回Mock默认数据 通常作为一种兜底操作
 * 2.强制使用本地Mock在服务开发阶段服务提供方未提供服务需要本地Mock一份数据返回
 * @date 2020-02-27 14:07
 * @JunitTest: {@link  }
 **/
public class DemoServiceMock implements DemoService {

    @Override
    public String mock(String name) {
        return "Mock value";
    }

    @Override
    public String sayHello(String name) {
        return "The defalut value is None";
    }

    @Override
    public CompletableFuture<String> sayHello2(String str) {
        return CompletableFuture.supplyAsync(()->"None");
    }
}
