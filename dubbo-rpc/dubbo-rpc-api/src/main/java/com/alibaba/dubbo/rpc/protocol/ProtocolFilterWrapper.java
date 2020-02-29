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
package com.alibaba.dubbo.rpc.protocol;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.List;

/**
 * ListenerProtocol
 * 实现 Protocol 接口，Protocol 的 Wrapper 拓展实现类，用于给 Invoker 增加过滤链
 */
public class ProtocolFilterWrapper implements Protocol {

    private final Protocol protocol;

    public ProtocolFilterWrapper(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    /**
     * 创建带 Filter 链的 Invoker 对象
     * @param invoker  Invoker 对象
     * @param key  获取 URL 参数名
     * @param group  分组  在暴露服务时，group = provider;在引用服务时，group = consumer
     * @param <T>  泛型
     * @return  Invoker 对象
     */
    private static <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group) {
        Invoker<T> last = invoker;
        // 获得过滤器数组
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(invoker.getUrl(), key, group);
        // 倒序循环 Filter ，创建带 Filter 链的 Invoker 对象。因为是通过嵌套声明匿名类循环调用的方式，所以要倒序
        if (!filters.isEmpty()) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                final Filter filter = filters.get(i);
                final Invoker<T> next = last;
                last = new Invoker<T>() {

                    public Class<T> getInterface() {
                        return invoker.getInterface();
                    }

                    public URL getUrl() {
                        return invoker.getUrl();
                    }

                    public boolean isAvailable() {
                        return invoker.isAvailable();
                    }

                    /**
                     * 调用 Filter#(invoker, invocation) 方法，不断执行过滤逻辑。而在 Filter 中，又不断调用 Invoker#invoker(invocation) 方法，
                     * 最终最后一个 Filter ，会调用 InjvmInvoker#invoke(invocation) 方法，继续执行逻辑
                     * (不同的协议，会调用不同的 Invoker 实现类，例如 Dubbo 协议，调用的是 DubboInvoker)
                     * @param invocation
                     * @return
                     * @throws RpcException
                     */
                    public Result invoke(Invocation invocation) throws RpcException {
                        return filter.invoke(next, invocation);
                    }

                    public void destroy() {
                        invoker.destroy();
                    }

                    @Override
                    public String toString() {
                        return invoker.toString();
                    }
                };
            }
        }
        return last;
    }

    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        // 注册中心  本地暴露服务不会符合这个判断。在远程暴露服务会符合暴露该判断；无需创建 Filter 过滤链
        if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
            //RegistryProtocol-->
            return protocol.export(invoker);
        }
        // 建立带有 Filter 过滤链的 Invoker ，再暴露服务
        return protocol.export(buildInvokerChain(invoker, Constants.SERVICE_FILTER_KEY, Constants.PROVIDER));
    }

    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        // 注册中心  本地引用服务不会符合这个判断
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            return protocol.refer(type, url);
        }
        // 引用服务，返回 Invoker 对象
        // 给改 Invoker 对象，包装成带有 Filter 过滤链的 Invoker 对象
        // 默认情况下，获得的 Filter 数组如下：ConsumerContextFilter,FutureFilter和MonitorFilter
        return buildInvokerChain(protocol.refer(type, url), Constants.REFERENCE_FILTER_KEY, Constants.CONSUMER);
    }

    public void destroy() {
        protocol.destroy();
    }

}