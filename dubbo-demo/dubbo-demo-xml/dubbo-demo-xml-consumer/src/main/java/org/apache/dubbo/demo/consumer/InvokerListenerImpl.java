package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.InvokerListener;
import org.apache.dubbo.rpc.RpcException;

/**
 * @author <a href="http://youngitman.tech">青年IT男</a>
 * @version v1.0.0
 * @className InvokerListenerImpl
 * @description Invoker引用和销毁监听
 * @date 2020-03-03 15:57
 * @JunitTest: {@link  }
 **/
public class InvokerListenerImpl implements InvokerListener {
    @Override
    public void referred(Invoker<?> invoker) throws RpcException {
        System.out.println("The Invoker is referred. reuslt:"+invoker);
    }

    @Override
    public void destroyed(Invoker<?> invoker) {
        System.out.println("The Invoker is destroyed. reuslt:"+invoker);
    }
}
