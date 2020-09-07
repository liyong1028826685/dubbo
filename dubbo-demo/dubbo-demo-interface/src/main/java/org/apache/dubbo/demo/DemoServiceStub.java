package org.apache.dubbo.demo;

import javax.security.auth.Destroyable;
import java.util.concurrent.CompletableFuture;

/**
 * @author <a href="http://youngitman.tech">青年IT男</a>
 * @version v1.0.0
 * @className DemoServiceStub
 * @description
 * @date 2020-02-27 13:53
 * @JunitTest: {@link  }
 **/
public class DemoServiceStub implements DemoService {
    private DemoService demoService;
    public DemoServiceStub(DemoService demoService){
        this.demoService = demoService;
    }

    @Override
    public String mock(String name) {
        return demoService.mock(name);
    }

    @Override
    public String sayHello(String name) {

        System.out.println("The local stub method is sayHello that being exec for before");
        String result = this.demoService.sayHello(name);
        System.out.println("The local stub method is sayHello that being exec for after");

        return result;
    }

    @Override
    public CompletableFuture<String> sayHello2(String str) {
        System.out.println("The local stub method is sayHello2 that being exec for before");
        CompletableFuture<String> result = this.demoService.sayHello2(str);
        System.out.println("The local stub method is sayHello2 that being exec for before");
        return result;
    }
}
