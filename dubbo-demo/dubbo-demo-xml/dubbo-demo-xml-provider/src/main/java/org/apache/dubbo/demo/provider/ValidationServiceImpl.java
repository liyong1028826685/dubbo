package org.apache.dubbo.demo.provider;

import org.apache.dubbo.demo.ValidationParameter;
import org.apache.dubbo.demo.ValidationService;

/**
 * @author <a href="http://youngitman.tech">青年IT男</a>
 * @version v1.0.0
 * @className ValidationServiceImpl
 * @description
 * @date 2020-02-26 14:45
 * @JunitTest: {@link  }
 **/
public class ValidationServiceImpl  implements ValidationService {
    @Override
    public void save(ValidationParameter parameter) {
        System.out.println(parameter);
    }

    @Override
    public void update(ValidationParameter parameter) {
        System.out.println(parameter);
    }
}
