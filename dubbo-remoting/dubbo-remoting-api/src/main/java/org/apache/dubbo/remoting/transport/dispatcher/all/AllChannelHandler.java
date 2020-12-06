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
package org.apache.dubbo.remoting.transport.dispatcher.all;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.ExecutionException;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelEventRunnable;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelEventRunnable.ChannelState;
import org.apache.dubbo.remoting.transport.dispatcher.WrappedChannelHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/***
 *@className AllChannelHandler
 *       
 *@description 所有处理分发到线程池去处理
 *       
 *@author <a href="http://youngitman.tech">青年IT男</a>
 *       
 *@date 12:50 2020-03-05
 *       
 *@JunitTest: {@link  }     
 *
 *@version v1.0.0
 *       
**/
public class AllChannelHandler extends WrappedChannelHandler {

    public AllChannelHandler(ChannelHandler handler, URL url) {
        super(handler, url);
    }

    /**
     *
     * 远程连接事件回调
     *
     * @author liyong
     * @date 1:34 PM 2020/12/6
     * @param channel
     * @exception
     * @return void
     **/
    @Override
    public void connected(Channel channel) throws RemotingException {
        ExecutorService executor = getExecutorService();
        try {
            //连接到远程事件放入线程池执行
            executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.CONNECTED));
        } catch (Throwable t) {
            throw new ExecutionException("connect event", channel, getClass() + " error when process connected event .", t);
        }
    }

    /**
     *
     * 端口远程连接
     *
     * @author liyong
     * @date 1:34 PM 2020/12/6
     * @param channel
     * @exception
     * @return void
     **/
    @Override
    public void disconnected(Channel channel) throws RemotingException {
        ExecutorService executor = getExecutorService();
        try {
            //断开连接处理事件放入线程池执行
            executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.DISCONNECTED));
        } catch (Throwable t) {
            throw new ExecutionException("disconnect event", channel, getClass() + " error when process disconnected event .", t);
        }
    }

    /**
     *
     * 接收到数据回调
     *
     * @author liyong
     * @date 1:34 PM 2020/12/6
     * @param channel
     * @param message
     * @exception
     * @return void
     **/
    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        ExecutorService executor = getPreferredExecutorService(message);
        try {
            //接收到数据放入线程池处理
            executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.RECEIVED, message));
        } catch (Throwable t) {
        	if(message instanceof Request && t instanceof RejectedExecutionException){
                sendFeedback(channel, (Request) message, t);
                return;
        	}
            throw new ExecutionException(message, channel, getClass() + " error when process received event .", t);
        }
    }

    /**
     *
     * 发生异常回调
     *
     * @author liyong
     * @date 1:35 PM 2020/12/6
     * @param channel
     * @param exception
     * @exception
     * @return void
     **/
    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        ExecutorService executor = getExecutorService();
        try {
            //发生异常放入线程池处理
            executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.CAUGHT, exception));
        } catch (Throwable t) {
            throw new ExecutionException("caught event", channel, getClass() + " error when process caught event .", t);
        }
    }
}
