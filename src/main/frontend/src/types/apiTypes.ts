/**
 * API请求和响应类型定义
 */

type ProcessDefinition = {
  id: number;
  name: string;
  description?: string;
  version: string;
  status: 'DRAFT' | 'PUBLISHED' | 'DEPRECATED';
  createTime: string;
  updateTime: string;
};

type ProcessNode = {
  id: number;
  processDefinitionId: number;
  name: string;
  type: string;
  config: Record<string, any>;
  sort: number;
};

// 流程定义相关API类型
type CreateProcessDefinitionRequest = Omit<ProcessDefinition, 'id' | 'createTime' | 'updateTime'>;
type UpdateProcessDefinitionRequest = ProcessDefinition;
type ProcessDefinitionResponse = ProcessDefinition;
type ProcessDefinitionListResponse = ProcessDefinition[];

// 流程节点相关API类型
type CreateProcessNodeRequest = Omit<ProcessNode, 'id'>;
type UpdateProcessNodeRequest = ProcessNode;
type ProcessNodeResponse = ProcessNode;
type ProcessNodeListResponse = ProcessNode[];

// 流程执行相关API类型
type ExecuteProcessRequest = {
  parameters: Record<string, any>;
};
type ExecuteProcessResponse = {
  success: boolean;
  result?: any;
  error?: {
    code: string;
    message: string;
    details?: Record<string, any>;
  };
};

export type {
  ProcessDefinition,
  ProcessNode,
  CreateProcessDefinitionRequest,
  UpdateProcessDefinitionRequest,
  ProcessDefinitionResponse,
  ProcessDefinitionListResponse,
  CreateProcessNodeRequest,
  UpdateProcessNodeRequest,
  ProcessNodeResponse,
  ProcessNodeListResponse,
  ExecuteProcessRequest,
  ExecuteProcessResponse
};