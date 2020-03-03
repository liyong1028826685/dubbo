package org.apache.dubbo.demo.consumer;

/**
 * @author <a href="http://youngitman.tech">青年IT男</a>
 * @version v1.0.0
 * @className Notify
 * @description 事件回掉通知
 * @date 2020-03-02 16:58
 * @JunitTest: {@link  }
 **/
public interface Notify {
    public void onreturn(String msg, Integer id);
    public void onthrow(Throwable ex, Integer id);
}
