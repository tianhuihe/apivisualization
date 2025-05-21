package com.example.apivisualization.service;

import com.example.apivisualization.entity.ProcessDefinition;
import com.example.apivisualization.entity.ProcessNode;
import java.util.List;

/**
 * 流程管理服务接口
 */
public interface ProcessService {
    /**
     * 创建流程定义
     * @param processDefinition 流程定义实体
     * @return 创建后的流程定义
     */
    ProcessDefinition createProcessDefinition(ProcessDefinition processDefinition);

    /**
     * 更新流程定义
     * @param processDefinition 流程定义实体
     * @return 更新后的流程定义
     */
    ProcessDefinition updateProcessDefinition(ProcessDefinition processDefinition);

    /**
     * 删除流程定义
     * @param id 流程定义ID
     */
    void deleteProcessDefinition(Long id);

    /**
     * 获取流程定义详情
     * @param id 流程定义ID
     * @return 流程定义实体
     */
    ProcessDefinition getProcessDefinition(Long id);

    /**
     * 添加流程节点
     * @param processNode 流程节点实体
     * @return 添加后的流程节点
     */
    ProcessNode addProcessNode(ProcessNode processNode);

    /**
     * 更新流程节点
     * @param processNode 流程节点实体
     * @return 更新后的流程节点
     */
    ProcessNode updateProcessNode(ProcessNode processNode);

    /**
     * 删除流程节点
     * @param id 流程节点ID
     */
    void deleteProcessNode(Long id);

    /**
     * 获取流程节点列表
     * @param processDefinitionId 流程定义ID
     * @return 流程节点列表
     */
    List<ProcessNode> listProcessNodes(Long processDefinitionId);

    /**
     * 执行流程编排
     * @param processDefinitionId 流程定义ID
     * @param parameters 执行参数
     * @return 执行结果
     */
    Object executeProcess(Long processDefinitionId, Object parameters);
}