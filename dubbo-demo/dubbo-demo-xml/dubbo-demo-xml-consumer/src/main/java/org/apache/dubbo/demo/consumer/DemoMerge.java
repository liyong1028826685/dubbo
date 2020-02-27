package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.rpc.cluster.Merger;

import java.util.Arrays;

/**
 * @author <a href="http://youngitman.tech">青年IT男</a>
 * @version v1.0.0
 * @className DemoMerge
 * @description
 * @date 2020-02-27 11:14
 * @JunitTest: {@link  }
 **/
public class DemoMerge    implements Merger<String>{

    @Override
    public String merge(String... items) {
        StringBuilder sb = new StringBuilder("merge1 result:");
        Arrays.stream(items).forEach(s->sb.append(s));
        return sb.toString();
    }
}
