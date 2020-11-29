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
package org.apache.dubbo.rpc.cluster.router.tag;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;
import org.apache.dubbo.rpc.cluster.router.tag.model.TagRouterRule;
import org.apache.dubbo.rpc.cluster.router.tag.model.TagRuleParser;

import java.net.UnknownHostException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.apache.dubbo.rpc.Constants.FORCE_USE_TAG;

/**
 * TagRouter, "application.tag-router"
 */
public class TagRouter extends AbstractRouter implements ConfigurationListener {
    public static final String NAME = "TAG_ROUTER";
    private static final int TAG_ROUTER_DEFAULT_PRIORITY = 100;
    private static final Logger logger = LoggerFactory.getLogger(TagRouter.class);
    private static final String RULE_SUFFIX = ".tag-router";

    private TagRouterRule tagRouterRule;
    private String application;

    public TagRouter(URL url) {
        super(url);
        this.priority = TAG_ROUTER_DEFAULT_PRIORITY;
    }

    @Override
    public synchronized void process(ConfigChangedEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Notification of tag rule, change type is: " + event.getChangeType() + ", raw rule is:\n " +
                    event.getContent());
        }

        try {
            if (event.getChangeType().equals(ConfigChangeType.DELETED)) {
                this.tagRouterRule = null;
            } else {
                this.tagRouterRule = TagRuleParser.parse(event.getContent());
            }
        } catch (Exception e) {
            logger.error("Failed to parse the raw tag router rule and it will not take effect, please check if the " +
                    "rule matches with the template, the raw rule is:\n ", e);
        }
    }

    @Override
    public URL getUrl() {
        return url;
    }

    /**
     *
     * 标签路由
     *
     * @author liyong
     * @date 4:48 PM 2020/11/29
     * @param invokers
     * @param url
     * @param invocation
     * @exception
     * @return java.util.List<org.apache.dubbo.rpc.Invoker<T>>
     **/
    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (CollectionUtils.isEmpty(invokers)) {
            return invokers;
        }

        // 这里因为配置中心可能更新配置，所有使用另外一个常量引用（类似复制）
        final TagRouterRule tagRouterRuleCopy = tagRouterRule;
        //如果动态规则不存在或无效或没有激活，使用静态标签
        if (tagRouterRuleCopy == null || !tagRouterRuleCopy.isValid() || !tagRouterRuleCopy.isEnabled()) {
            //处理静态标签
            return filterUsingStaticTag(invokers, url, invocation);
        }

        List<Invoker<T>> result = invokers;
        //获取上下文中Attachment的标签参数，这个参数由客户端调用时候写入
        String tag = StringUtils.isEmpty(invocation.getAttachment(TAG_KEY)) ? url.getParameter(TAG_KEY) :
                invocation.getAttachment(TAG_KEY);

        // 如果存在传递标签
        if (StringUtils.isNotEmpty(tag)) {
            //通过传递的标签找到动态配置对应的服务地址
            List<String> addresses = tagRouterRuleCopy.getTagnameToAddresses().get(tag);
            // 通过标签分组进行过滤
            if (CollectionUtils.isNotEmpty(addresses)) {
                //获取匹配地址的服务
                result = filterInvoker(invokers, invoker -> addressMatches(invoker.getUrl(), addresses));
                //如果返回结果不为null 或者 返回结果为空但是配置force=true也直接返回
                if (CollectionUtils.isNotEmpty(result) || tagRouterRuleCopy.isForce()) {
                    return result;
                }
            } else {
                //检测静态标签
                result = filterInvoker(invokers, invoker -> tag.equals(invoker.getUrl().getParameter(TAG_KEY)));
            }
            //如果提供者没有配置标签 默认force.tag = false 表示可以访问任意的提供者 ，除非我们显示的禁止
            if (CollectionUtils.isNotEmpty(result) || isForceUseTag(invocation)) {
                return result;
            }
            else {
                //返回所有的提供者，不需要任意标签
                List<Invoker<T>> tmp = filterInvoker(invokers, invoker -> addressNotMatches(invoker.getUrl(),
                        tagRouterRuleCopy.getAddresses()));
                //查找提供者标签为空
                return filterInvoker(tmp, invoker -> StringUtils.isEmpty(invoker.getUrl().getParameter(TAG_KEY)));
            }
        } else {
            //返回所有的 addresses
            List<String> addresses = tagRouterRuleCopy.getAddresses();
            if (CollectionUtils.isNotEmpty(addresses)) {
                result = filterInvoker(invokers, invoker -> addressNotMatches(invoker.getUrl(), addresses));
                // 1. all addresses are in dynamic tag group, return empty list.
                if (CollectionUtils.isEmpty(result)) {
                    return result;
                }
            }
            //继续使用静态标签过滤
            return filterInvoker(result, invoker -> {
                String localTag = invoker.getUrl().getParameter(TAG_KEY);
                return StringUtils.isEmpty(localTag) || !tagRouterRuleCopy.getTagNames().contains(localTag);
            });
        }
    }

    /**
     * If there's no dynamic tag rule being set, use static tag in URL.
     * <p>
     * A typical scenario is a Consumer using version 2.7.x calls Providers using version 2.6.x or lower,
     * the Consumer should always respect the tag in provider URL regardless of whether a dynamic tag rule has been set to it or not.
     * <p>
     * TODO, to guarantee consistent behavior of interoperability between 2.6- and 2.7+, this method should has the same logic with the TagRouter in 2.6.x.
     *
     * @param invokers
     * @param url
     * @param invocation
     * @param <T>
     * @return
     */
    private <T> List<Invoker<T>> filterUsingStaticTag(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        List<Invoker<T>> result = invokers;
        // 动态标签参数
        String tag = StringUtils.isEmpty(invocation.getAttachment(TAG_KEY)) ? url.getParameter(TAG_KEY) :
                invocation.getAttachment(TAG_KEY);
        // 消费端指定的标签
        if (!StringUtils.isEmpty(tag)) {
            //匹配消费端指定的标签与每一个服务提供者在 XML 中或注解中配置的静态标签是否一致
            result = filterInvoker(invokers, invoker -> tag.equals(invoker.getUrl().getParameter(TAG_KEY)));
            //如果没有匹配到结果并且force=false 查找未配置静态标签的提供者
            if (CollectionUtils.isEmpty(result) && !isForceUseTag(invocation)) {
                result = filterInvoker(invokers, invoker -> StringUtils.isEmpty(invoker.getUrl().getParameter(TAG_KEY)));
            }
        } else {
            //查找未配置静态标签的提供者
            result = filterInvoker(invokers, invoker -> StringUtils.isEmpty(invoker.getUrl().getParameter(TAG_KEY)));
        }
        return result;
    }

    @Override
    public boolean isRuntime() {
        return tagRouterRule != null && tagRouterRule.isRuntime();
    }

    @Override
    public boolean isForce() {
        // FIXME
        return tagRouterRule != null && tagRouterRule.isForce();
    }

    private boolean isForceUseTag(Invocation invocation) {
        return Boolean.valueOf(invocation.getAttachment(FORCE_USE_TAG, url.getParameter(FORCE_USE_TAG, "false")));
    }

    private <T> List<Invoker<T>> filterInvoker(List<Invoker<T>> invokers, Predicate<Invoker<T>> predicate) {
        return invokers.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private boolean addressMatches(URL url, List<String> addresses) {
        return addresses != null && checkAddressMatch(addresses, url.getHost(), url.getPort());
    }

    private boolean addressNotMatches(URL url, List<String> addresses) {
        return addresses == null || !checkAddressMatch(addresses, url.getHost(), url.getPort());
    }

    /**
     *
     * 检测地址是否匹配
     *
     * @author liyong
     * @date 4:46 PM 2020/11/29
     * @param addresses
     * @param host
     * @param port
     * @exception
     * @return boolean
     **/
    private boolean checkAddressMatch(List<String> addresses, String host, int port) {
        for (String address : addresses) {
            try {
                //匹配ip
                if (NetUtils.matchIpExpression(address, host, port)) {
                    return true;
                }
                if ((ANYHOST_VALUE + ":" + port).equals(address)) {
                    return true;
                }
            } catch (UnknownHostException e) {
                logger.error("The format of ip address is invalid in tag route. Address :" + address, e);
            } catch (Exception e) {
                logger.error("The format of ip address is invalid in tag route. Address :" + address, e);
            }
        }
        return false;
    }

    public void setApplication(String app) {
        this.application = app;
    }

    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        if (CollectionUtils.isEmpty(invokers)) {
            return;
        }

        Invoker<T> invoker = invokers.get(0);
        URL url = invoker.getUrl();
        String providerApplication = url.getParameter(CommonConstants.REMOTE_APPLICATION_KEY);

        if (StringUtils.isEmpty(providerApplication)) {
            logger.error("TagRouter must getConfig from or subscribe to a specific application, but the application " +
                    "in this TagRouter is not specified.");
            return;
        }

        synchronized (this) {
            if (!providerApplication.equals(application)) {
                if (!StringUtils.isEmpty(application)) {
                    ruleRepository.removeListener(application + RULE_SUFFIX, this);
                }
                String key = providerApplication + RULE_SUFFIX;
                ruleRepository.addListener(key, this);
                application = providerApplication;
                String rawRule = ruleRepository.getRule(key, DynamicConfiguration.DEFAULT_GROUP);
                if (StringUtils.isNotEmpty(rawRule)) {
                    this.process(new ConfigChangedEvent(key, DynamicConfiguration.DEFAULT_GROUP, rawRule));
                }
            }
        }
    }

}
