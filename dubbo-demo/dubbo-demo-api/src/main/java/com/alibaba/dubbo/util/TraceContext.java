package com.alibaba.dubbo.util;

import com.alibaba.dubbo.bean.TraceRecord;

public class TraceContext {

    private static final ThreadLocal<TraceRecord> REQUEST_LOCAL = new ThreadLocal<TraceRecord>();

    public final static void initialLocal(TraceRecord request) {
        if (null == request) {
            return;
        }
        REQUEST_LOCAL.set(request);
    }

    public static TraceRecord getCurrentRequest() {
        return REQUEST_LOCAL.get();
    }

}
