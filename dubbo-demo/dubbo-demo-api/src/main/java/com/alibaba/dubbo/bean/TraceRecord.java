package com.alibaba.dubbo.bean;

/**
 * 记录链路追踪信息
 */
public class TraceRecord {

    private String tracingId;

    public String getTracingId() {
        return tracingId;
    }
    public void setTracingId(String tracingId) {
        this.tracingId = tracingId;
    }
}
