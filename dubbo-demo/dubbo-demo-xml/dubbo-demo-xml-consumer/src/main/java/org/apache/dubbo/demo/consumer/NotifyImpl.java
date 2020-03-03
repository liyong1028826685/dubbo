package org.apache.dubbo.demo.consumer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="http://youngitman.tech">青年IT男</a>
 * @version v1.0.0
 * @className NotifyImpl
 * @description
 * @date 2020-03-02 16:59
 * @JunitTest: {@link  }
 **/
public class NotifyImpl implements Notify {
    public Map<Integer, String> ret = new HashMap<>();
    public Map<Integer, Throwable> errors = new HashMap<>();

    @Override
    public void onreturn(String msg, Integer id) {
        System.out.println("onreturn:" + msg);
        ret.put(id, msg);
    }

    @Override
    public void onthrow(Throwable ex, Integer id) {
        errors.put(id, ex);
    }
}