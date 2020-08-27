/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.demo.ValidationParameter;
import org.apache.dubbo.demo.ValidationService;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class Application {

    static ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/dubbo-consumer.xml");

    /**
     * In order to make sure multicast registry works, need to specify '-Djava.net.preferIPv4Stack=true' before
     * launch the application
     */
    public static void main(String[] args) throws Exception {
        context.start();

//        for(int i=0;i<10;i++){
//            cache();
//            TimeUnit.SECONDS.sleep(1);
//        }

//        for(int i=0;i<10;i++){
//            checkParameters();
//        }


//        mock();

//        asyncCall();

//        asyncRPCContext();

//        async();

        sync();

        //System.in.read();
    }

    private static void sync(){
        DemoService demoService = context.getBean("demoService", DemoService.class);
        String result = demoService.sayHello("hello");
        System.out.println(result);
    }


    private static void mock(){
        DemoService demoService = context.getBean("demoService2", DemoService.class);
        String result = demoService.mock("mock");
        System.out.println(result);
    }

    private static void asyncCall(){
        DemoService demoService = context.getBean("demoService2", DemoService.class);
        CompletableFuture future = RpcContext.getContext().asyncCall(() -> demoService.sayHello("Hello"));
        future.whenComplete((d,t)-> System.out.println("async result is "+d));
    }

    private static void  asyncRPCContext(){
        DemoService demoService = context.getBean("demoService2", DemoService.class);
        demoService.sayHello("Hello");
        CompletableFuture<String> fut = RpcContext.getContext().getCompletableFuture();

        fut.whenComplete((d,t)-> System.out.println("async result is"+d));

        //String s = fut.join();
        //System.out.println("async result is "+s);
    }


    private static void  async(){
        DemoService demoService = context.getBean("demoService2", DemoService.class);
        CompletableFuture<String> fut = demoService.sayHello2("Hello");

        fut.whenComplete((d,t)-> System.out.println("async result is"+d));

        //String s = fut.join();
        //System.out.println("async result is "+s);
    }

    private static void defalut(){
        DemoService demoService = context.getBean("demoService", DemoService.class);

        String result = demoService.sayHello("hello");

        System.out.println("result="+result);
    }

    /**
     * 客户端实现缓存
     */
    private static void cache(){
        DemoService demoService = context.getBean("demoService2", DemoService.class);

        String result = demoService.sayHello("hello");

        System.out.println("result="+result);
    }

    private static void checkParameters(){
        ValidationService validationService = context.getBean("validationService", ValidationService.class);

        ValidationParameter parameter = new ValidationParameter();
        parameter.setAge(30);
        parameter.setName("ouwen");
        parameter.setEmail("1028826686@qq.com");
        Date start = new Date(System.currentTimeMillis() - 10 * 1_0000);
        parameter.setLoginDate(start);
        Date end = new Date(System.currentTimeMillis() + 10 * 1_0000);
        parameter.setExpiryDate(end);

        validationService.save(parameter);

    }

}
