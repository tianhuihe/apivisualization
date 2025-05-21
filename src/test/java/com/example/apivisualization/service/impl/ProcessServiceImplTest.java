package com.example.apivisualization.service.impl;

import com.example.apivisualization.entity.ProcessNode;
import com.example.apivisualization.exception.ProcessException;
import com.example.apivisualization.service.impl.ProcessExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProcessServiceImpl单元测试类
 */
class ProcessServiceImplTest {
    @InjectMocks
    private ProcessServiceImpl processService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void executeNode_shouldThrowExceptionWhenNodeIsNull() {
        ProcessExecutionContext context = new ProcessExecutionContext(null);
        assertThrows(ProcessException.class, 
            () -> processService.executeNode(null, context));
    }
    
    @Test
    void executeNode_shouldThrowExceptionWhenContextIsNull() {
        ProcessNode node = new ProcessNode();
        assertThrows(ProcessException.class, 
            () -> processService.executeNode(node, null));
    }
    
    @Test
    void executeNode_shouldThrowExceptionWhenNodeTypeIsEmpty() {
        ProcessNode node = new ProcessNode();
        node.setType("");
        ProcessExecutionContext context = new ProcessExecutionContext(null);
        assertThrows(ProcessException.class, 
            () -> processService.executeNode(node, context));
    }
    
    @Test
    void executeNode_shouldThrowExceptionWhenNodeConfigIsNull() {
        ProcessNode node = new ProcessNode();
        node.setType("API_CALL");
        ProcessExecutionContext context = new ProcessExecutionContext(null);
        assertThrows(ProcessException.class, 
            () -> processService.executeNode(node, context));
    }
    
    @Test
    void retryExecuteNode_shouldRetryWhenRetryableExceptionOccurs() {
        ProcessNode node = new ProcessNode();
        node.setType("API_CALL");
        node.setConfig("{}");
        
        ProcessExecutionContext context = new ProcessExecutionContext(null);
        context.setMaxRetryTimes(3);
        
        // 模拟第一次执行失败
        when(processService.executeNode(node, context))
            .thenThrow(new RetryableException("模拟失败", 0))
            .thenReturn("success");
            
        Object result = processService.retryExecuteNode(node, context);
        
        assertEquals("success", result);
        verify(processService, times(2)).executeNode(node, context);
    }
    
    @Test
    void retryExecuteNode_shouldThrowExceptionWhenMaxRetryTimesExceeded() {
        ProcessNode node = new ProcessNode();
        node.setType("API_CALL");
        node.setConfig("{}");
        
        ProcessExecutionContext context = new ProcessExecutionContext(null);
        context.setMaxRetryTimes(2);
        
        // 模拟连续失败
        when(processService.executeNode(node, context))
            .thenThrow(new RetryableException("模拟失败", 0))
            .thenThrow(new RetryableException("模拟失败", 1))
            .thenThrow(new RetryableException("模拟失败", 2));
            
        assertThrows(RetryableException.class, 
            () -> processService.retryExecuteNode(node, context));
        
        verify(processService, times(3)).executeNode(node, context);
    }
    
    @Test
    void executeNode_shouldLogCorrectFormatWhenSuccess() {
        ProcessNode node = new ProcessNode();
        node.setType("API_CALL");
        node.setConfig("{}");
        
        ProcessExecutionContext context = new ProcessExecutionContext(null);
        
        // 模拟成功执行
        when(processService.executeNode(node, context)).thenReturn("success");
        
        // 捕获日志输出
        try (MockedStatic<Log> mockedLog = Mockito.mockStatic(Log.class)) {
            processService.executeNode(node, context);
            
            // 验证日志格式
            mockedLog.verify(() -> 
                Log.info(contains("流程节点执行完成"), 
                    eq(node.getId()), 
                    eq(node.getType()), 
                    anyLong(), 
                    anyString(), 
                    anyString()));
        }
    }
    
    @Test
    void executeNode_shouldLogCorrectFormatWhenFailed() {
        ProcessNode node = new ProcessNode();
        node.setType("API_CALL");
        node.setConfig("{}");
        
        ProcessExecutionContext context = new ProcessExecutionContext(null);
        
        // 模拟失败执行
        when(processService.executeNode(node, context))
            .thenThrow(new ProcessException("模拟失败"));
        
        // 捕获日志输出
        try (MockedStatic<Log> mockedLog = Mockito.mockStatic(Log.class)) {
            assertThrows(ProcessException.class, 
                () -> processService.executeNode(node, context));
            
            // 验证日志格式
            mockedLog.verify(() -> 
                Log.error(contains("流程节点执行失败"), 
                    eq(node.getId()), 
                    eq(node.getType()), 
                    anyLong(), 
                    anyString()));
        }
    }
}