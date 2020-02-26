package org.apache.dubbo.demo.provider;

/**
 * @author <a href="http://youngitman.tech">青年IT男</a>
 * @version v1.0.0
 * @className CustomValidator
 * @description
 * @date 2020-02-26 19:31
 * @JunitTest: {@link  }
 **/
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.validation.support.jvalidation.JValidator;
import org.apache.dubbo.validation.Validator;

import javax.validation.*;
import java.util.*;

/**
 * 继承自{@link JValidator}保证了dubbo原生的vidation逻辑。
 * 修改点：1、捕获ConstraintViolationException异常，将验证未通过异常改为ValidationException，解决dubbo hessian反序列化失败问题
 *       2、格式化验证提示，使用json格式响应
 *
 * @Author lixin
 * @Date 2019-04-23 17:35
 */
public class CustomValidator extends JValidator implements Validator {

    private final Class<?> clazz;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public CustomValidator(URL url) {
        super(url);
        this.clazz = ReflectUtils.forName(url.getServiceInterface());
    }

    @Override
    public void validate(String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Exception {

        try{
            super.validate(methodName,parameterTypes,arguments);
        }catch (ConstraintViolationException e){
            throw new ValidationException(constraintMessage(clazz.getName(),methodName,e.getConstraintViolations()));
        }
    }

    private String constraintMessage(String className,String methodName,Set<ConstraintViolation<?>> violations){
        JSONObject json = new JSONObject();
        json.put("service",className);
        json.put("method",methodName);

        JSONArray details = new JSONArray();
        for(ConstraintViolation violation : violations){
            JSONObject detail = new JSONObject();
            detail.put("bean",violation.getRootBean().getClass().getName());
            detail.put("property",violation.getPropertyPath().toString());
            detail.put("message",violation.getMessage());
            details.add(detail);
        }
        json.put("details",details);

        return json.toString();
    }
}
