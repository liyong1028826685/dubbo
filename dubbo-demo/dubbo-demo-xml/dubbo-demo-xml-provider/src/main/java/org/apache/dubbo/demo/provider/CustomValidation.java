package org.apache.dubbo.demo.provider;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.validation.Validation;
import org.apache.dubbo.validation.Validator;

/**
 * @author <a href="http://youngitman.tech">青年IT男</a>
 * @version v1.0.0
 * @className CustomValidation
 * @description
 *
 * 解决服务提供端开启jsr303参数校验 validation 需要配置为"custom"
 *
 * provider端启用(注解方式)
 * @Service(validation = "xxx")
 *
 * consumer端启用(注解方式)
 * @Reference(validation = "xxx")
 *
 * @date 2020-02-26 19:30
 * @JunitTest: {@link  }
 **/
public class CustomValidation implements Validation {

    @Override
    public Validator getValidator(URL url) {
        return new CustomValidator(url);
    }
}