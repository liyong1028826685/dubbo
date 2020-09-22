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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * LeastActiveLoadBalance
 * <p>
 * Filter the number of invokers with the least number of active calls and count the weights and quantities of these invokers.
 * If there is only one invoker, use the invoker directly;
 * if there are multiple invokers and the weights are not the same, then random according to the total weight;
 * if there are multiple invokers and the same weight, then randomly called.
 */
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "leastactive";

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size();
        //初始化最小活跃值，最小活跃数，最小活跃的invoker索引（index）,权重数组
        int leastActive = -1;
        int leastCount = 0;
        int[] leastIndexes = new int[length];
        int[] weights = new int[length];
        int totalWeight = 0;
        int firstWeight = 0;
        boolean sameWeight = true;

        // 过滤出所有最少活跃服务,并统计最少活跃服务权重值
        for (int i = 0; i < length; i++) {
            Invoker<T> invoker = invokers.get(i);
            // 获取这个invoker活跃数
            int active = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName()).getActive();
            // 获取invoker的权重 默认100.
            int afterWarmup = getWeight(invoker, invocation);
            // 权重保存到集合中
            weights[i] = afterWarmup;
            //找到最小活跃数invoker
            if (leastActive == -1 || active < leastActive) {
                //重置最小活跃数值
                leastActive = active;
                //重置最小活跃计数
                leastCount = 1;
                // 记录最小获取数对应索引
                leastIndexes[0] = i;
                // 重置权重合计
                totalWeight = afterWarmup;
                // 记录第一个最小活跃数对应权重值
                firstWeight = afterWarmup;
                // 重置是否具有相同权重标志
                sameWeight = true;
                //当前invoker活跃数等于最小活跃数
            } else if (active == leastActive) {
                // 最小活跃数leastCount增加并且记录当前invoker对应索引
                leastIndexes[leastCount++] = i;
                // 累计invoker权重值
                totalWeight += afterWarmup;
                // 判断是否权重值相同
                if (sameWeight && i > 0
                        && afterWarmup != firstWeight) {
                    sameWeight = false;
                }
            }
        }
        //最小活跃数invoker只有一个
        if (leastCount == 1) {
            // 直接返回
            return invokers.get(leastIndexes[0]);
        }
        //具有多个相同活跃数、具有不同权重且权重和大于0
        if (!sameWeight && totalWeight > 0) {
            // 根据权重值进行一个(0,totalWeight]整数随机值
            int offsetWeight = ThreadLocalRandom.current().nextInt(totalWeight);
            // 基于随机值选择所在区间和随机负载均衡算法一致
            for (int i = 0; i < leastCount; i++) {
                int leastIndex = leastIndexes[i];
                offsetWeight -= weights[leastIndex];
                if (offsetWeight < 0) {
                    return invokers.get(leastIndex);
                }
            }
        }
        // 如果所有权重值相同或者totalWeight=0返回服务列表中随机一个服务
        return invokers.get(leastIndexes[ThreadLocalRandom.current().nextInt(leastCount)]);
    }
}
