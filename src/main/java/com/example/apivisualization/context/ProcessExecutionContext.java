package com.example.apivisualization.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程执行上下文
 * 用于管理流程执行过程中的状态和参数传递
 */
public class ProcessExecutionContext {
    private Object initialParameters;
    private Object currentParameters;
    private Map<Long, Object> nodeResults = new HashMap<>();
    private Long failedNodeId;
    private Exception exception;

    public ProcessExecutionContext(Object initialParameters) {
        this.initialParameters = initialParameters;
        this.currentParameters = initialParameters;
    }

    /**
     * 获取初始参数
     */
    public Object getInitialParameters() {
        return initialParameters;
    }

    /**
     * 获取当前参数
     */
    public Object getCurrentParameters() {
        return currentParameters;
    }

    /**
     * 设置当前参数
     */
    public void setCurrentParameters(Object parameters) {
        this.currentParameters = parameters;
    }

    /**
     * 存储节点执行结果
     */
    public void putResult(Long nodeId, Object result) {
        nodeResults.put(nodeId, result);
    }

    /**
     * 获取节点执行结果
     */
    public Object getResult(Long nodeId) {
        return nodeResults.get(nodeId);
    }

    /**
     * 设置失败节点ID
     */
    public void setFailedNode(Long nodeId) {
        this.failedNodeId = nodeId;
    }

    /**
     * 获取失败节点ID
     */
    public Long getFailedNode() {
        return failedNodeId;
    }

    /**
     * 设置异常信息
     */
    public void setException(Exception e) {
        this.exception = e;
    }

    /**
     * 获取异常信息
     */
    public Exception getException() {
        return exception;
    }

    /**
     * 获取最终执行结果
     */
    public Object getFinalResult() {
        // 如果有异常则返回异常
        if (exception != null) {
            return exception;
        }
        // 否则返回最后一个节点的执行结果
        return currentParameters;
    }
}