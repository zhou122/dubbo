package com.alibaba.dubbo.demo.filter;

import com.alibaba.dubbo.bean.TraceRecord;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.*;

import java.util.UUID;

@Activate(group = Constants.PROVIDER)
public class ProviderFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        String traceId = invocation.getAttachment("traceId");
        if(StringUtils.isBlank(traceId)) {
            traceId = UUID.randomUUID().toString();
        }
        RpcContext.getContext().setAttachment("traceId",traceId);
        System.out.println("=====providerTraceId:"+traceId);

        return invoker.invoke(invocation);
    }
}
