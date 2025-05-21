package com.example.apivisualization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.apivisualization.entity.ProcessDefinition;
import com.example.apivisualization.entity.ProcessNode;
import com.example.apivisualization.mapper.ProcessDefinitionMapper;
import com.example.apivisualization.mapper.ProcessNodeMapper;
import com.example.apivisualization.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程管理服务实现类
 */
@Service
public class ProcessServiceImpl implements ProcessService {
    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;
    
    @Autowired
    private ProcessNodeMapper processNodeMapper;

    @Override
    @Transactional
    public ProcessDefinition createProcessDefinition(ProcessDefinition processDefinition) {
        processDefinitionMapper.insert(processDefinition);
        return processDefinition;
    }

    @Override
    @Transactional
    public ProcessDefinition updateProcessDefinition(ProcessDefinition processDefinition) {
        processDefinitionMapper.updateById(processDefinition);
        return processDefinition;
    }

    @Override
    @Transactional
    public void deleteProcessDefinition(Long id) {
        // 删除流程定义
        processDefinitionMapper.deleteById(id);
        // 删除关联的流程节点
        LambdaQueryWrapper<ProcessNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessNode::getProcessDefinitionId, id);
        processNodeMapper.delete(wrapper);
    }

    @Override
    public ProcessDefinition getProcessDefinition(Long id) {
        return processDefinitionMapper.selectById(id);
    }

    @Override
    @Transactional
    public ProcessNode addProcessNode(ProcessNode processNode) {
        processNodeMapper.insert(processNode);
        return processNode;
    }

    @Override
    @Transactional
    public ProcessNode updateProcessNode(ProcessNode processNode) {
        processNodeMapper.updateById(processNode);
        return processNode;
    }

    @Override
    @Transactional
    public void deleteProcessNode(Long id) {
        processNodeMapper.deleteById(id);
    }

    @Override
    public List<ProcessNode> listProcessNodes(Long processDefinitionId) {
        LambdaQueryWrapper<ProcessNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessNode::getProcessDefinitionId, processDefinitionId)
               .orderByAsc(ProcessNode::getSort);
        return processNodeMapper.selectList(wrapper);
    }

    @Override
    public Object executeProcess(Long processDefinitionId, Object parameters) {
        // 1. 获取流程节点列表
        List<ProcessNode> nodes = listProcessNodes(processDefinitionId);
        
        // 2. 创建执行上下文
        ProcessExecutionContext context = new ProcessExecutionContext(parameters);
        
        // 3. 按顺序执行节点
        for (ProcessNode node : nodes) {
            try {
                // 执行当前节点
                Object result = executeNode(node, context);
                
                // 将结果存入上下文
                context.putResult(node.getId(), result);
                
                // 传递参数给下一个节点
                context.setCurrentParameters(result);
                
                // 记录节点执行成功日志
                log.info("流程节点执行成功 - 节点ID: {}, 节点类型: {}, 流程定义ID: {}", 
                    node.getId(), node.getType(), processDefinitionId);
            } catch (Exception e) {
                // 记录节点执行失败日志
                log.error("流程节点执行失败 - 节点ID: {}, 节点类型: {}, 流程定义ID: {}, 错误信息: {}", 
                    node.getId(), node.getType(), processDefinitionId, e.getMessage(), e);
                
                // 处理节点执行异常
                context.setFailedNode(node.getId());
                context.setException(e);
                break;
            }
        }
        
        // 4. 记录流程执行完成日志
        if (context.getFailedNode() != null) {
            log.warn("流程执行中断 - 流程定义ID: {}, 失败节点ID: {}", 
                processDefinitionId, context.getFailedNode());
        } else {
            log.info("流程执行完成 - 流程定义ID: {}", processDefinitionId);
        }
        
        // 5. 返回最终执行结果
        return context.getFinalResult();
    }
    
    /**
     * 执行单个流程节点
     * @param node 流程节点
     * @param context 执行上下文
     * @return 节点执行结果
     */
    private Object executeNode(ProcessNode node, ProcessExecutionContext context) {
        // 1. 参数校验
        if (node == null) {
            throw new ProcessException(ErrorCode.NODE_NOT_FOUND, "流程节点不能为空");
        }
        if (context == null) {
            throw new ProcessException(ErrorCode.CONTEXT_INVALID, "执行上下文不能为空");
        }
        // 校验节点类型
        if (StringUtils.isEmpty(node.getType())) {
            throw new ProcessException(ErrorCode.NODE_TYPE_INVALID, "节点类型不能为空");
        }
        // 校验节点配置
        if (node.getConfig() == null) {
            throw new ProcessException(ErrorCode.NODE_CONFIG_INVALID, "节点配置不能为空");
        }
        
        // 2. 检查执行超时
        if (context.isTimeout()) {
            throw new ProcessException(ErrorCode.PROCESS_TIMEOUT, "流程执行超时");
        }
        
        // 更新执行状态
        context.setCurrentNode(node.getId());
        context.setNodeStartTime(System.currentTimeMillis());
        
        try {
            // 获取当前节点参数
            Object parameters = context.getCurrentParameters();
            
            // 根据节点类型执行不同逻辑
            Object result;
            switch (node.getType()) {
                case "API_CALL":
                    // 执行API调用
                    result = executeApiCall(node, parameters);
                    break;
                    
                case "DATA_TRANSFORM":
                    // 执行数据转换
                    result = executeDataTransform(node, parameters);
                    break;
                    
                case "CONDITIONAL":
                    // 执行条件判断
                    result = executeConditional(node, parameters);
                    break;
                    
                default:
                    throw new IllegalArgumentException("不支持的节点类型: " + node.getType());
            }
            
            // 更新执行状态
            context.setNodeEndTime(System.currentTimeMillis());
            context.setNodeStatus(node.getId(), "SUCCESS");
            
            // 记录执行耗时和详细上下文
            long costTime = context.getNodeEndTime() - context.getNodeStartTime();
            log.info("流程节点执行完成 - 节点ID: {}, 节点类型: {}, 耗时: {}ms, 参数: {}, 结果: {}", 
                node.getId(), node.getType(), costTime, 
                JsonUtils.toJson(context.getCurrentParameters()),
                JsonUtils.toJson(result));
            
            return result;
        } catch (Exception e) {
            // 更新执行状态
            context.setNodeEndTime(System.currentTimeMillis());
            context.setNodeStatus(node.getId(), "FAILED");
            
            // 记录执行耗时和异常详情
            long costTime = context.getNodeEndTime() - context.getNodeStartTime();
            log.error("流程节点执行失败 - 节点ID: {}, 节点类型: {}, 耗时: {}ms, 错误信息: {}", 
                node.getId(), node.getType(), costTime, e.getMessage(), e);
            
            // 对于可重试异常添加重试逻辑
            if (e instanceof RetryableException) {
                RetryableException re = (RetryableException)e;
                if (re.getRetryTimes() < context.getMaxRetryTimes()) {
                    log.warn("流程节点重试执行 - 节点ID: {}, 当前重试次数: {}, 最大重试次数: {}",
                        node.getId(), re.getRetryTimes(), context.getMaxRetryTimes());
                    return retryExecuteNode(node, context);
                }
            }
            
            throw new ProcessException(ErrorCode.NODE_EXECUTION_FAILED, "节点执行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行API调用节点
     */
    private Object executeApiCall(ProcessNode node, Object parameters) {
        // 1. 解析节点配置
        String apiUrl = node.getConfig().getString("apiUrl");
        String method = node.getConfig().getString("method", "GET");
        Integer timeout = node.getConfig().getInt("timeout", 5000);
        
        // 2. 准备请求参数
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 添加自定义请求头
        if (node.getConfig().has("headers")) {
            JSONObject headerConfig = node.getConfig().getJSONObject("headers");
            for (String key : headerConfig.keySet()) {
                headers.add(key, headerConfig.getString(key));
            }
        }
        
        HttpEntity<Object> requestEntity = new HttpEntity<>(parameters, headers);
        
        // 3. 配置RestTemplate
        RestTemplate restTemplate = new RestTemplate();
        
        // 设置超时
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        restTemplate.setRequestFactory(requestFactory);
        
        // 4. 执行HTTP请求
        ResponseEntity<String> response;
        try {
            switch (method.toUpperCase()) {
                case "GET":
                    response = restTemplate.exchange(
                        apiUrl, HttpMethod.GET, requestEntity, String.class);
                    break;
                case "POST":
                    response = restTemplate.exchange(
                        apiUrl, HttpMethod.POST, requestEntity, String.class);
                    break;
                case "PUT":
                    response = restTemplate.exchange(
                        apiUrl, HttpMethod.PUT, requestEntity, String.class);
                    break;
                case "DELETE":
                    response = restTemplate.exchange(
                        apiUrl, HttpMethod.DELETE, requestEntity, String.class);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的HTTP方法: " + method);
            }
            
            // 5. 处理响应
            if (response.getStatusCode().is2xxSuccessful()) {
                // 支持JSON和XML格式的响应
                String contentType = response.getHeaders().getContentType() != null ? 
                    response.getHeaders().getContentType().toString() : "";
                
                if (contentType.contains("application/json")) {
                    return new JSONObject(response.getBody());
                } else if (contentType.contains("application/xml")) {
                    return XML.toJSONObject(response.getBody());
                }
                return response.getBody();
            } else {
                throw new RuntimeException("API调用失败: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            throw new RuntimeException("API调用异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行数据转换节点
     */
    private Object executeDataTransform(ProcessNode node, Object parameters) {
        // 1. 解析转换配置
        JSONObject transformConfig = node.getConfig();
        String transformType = transformConfig.getString("type", "MAPPING");
        
        // 2. 根据转换类型执行不同逻辑
        switch (transformType) {
            case "MAPPING":
                // 字段映射转换
                return executeFieldMapping(transformConfig, parameters);
                
            case "FILTER":
                // 字段过滤转换
                return executeFieldFilter(transformConfig, parameters);
                
            case "CALCULATION":
                // 计算字段转换
                return executeFieldCalculation(transformConfig, parameters);
                
            default:
                throw new IllegalArgumentException("不支持的数据转换类型: " + transformType);
        }
    }
    
    /**
     * 执行字段映射转换
     */
    private Object executeFieldMapping(JSONObject config, Object parameters) {
        // 1. 获取映射规则
        JSONObject mappingRules = config.getJSONObject("rules");
        
        // 2. 根据参数类型执行不同转换
        if (parameters instanceof Map) {
            // Map类型参数转换
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> inputMap = (Map<String, Object>) parameters;
            
            // 3. 应用映射规则
            for (String sourceKey : mappingRules.keySet()) {
                String targetKey = mappingRules.getString(sourceKey);
                if (inputMap.containsKey(sourceKey)) {
                    result.put(targetKey, inputMap.get(sourceKey));
                }
            }
            
            return result;
        } else if (parameters instanceof List) {
            // List类型参数转换
            List<Object> result = new ArrayList<>();
            List<Object> inputList = (List<Object>) parameters;
            
            // 4. 应用映射规则
            for (Object item : inputList) {
                if (item instanceof Map) {
                    Map<String, Object> transformedItem = new HashMap<>();
                    Map<String, Object> itemMap = (Map<String, Object>) item;
                    
                    for (String sourceKey : mappingRules.keySet()) {
                        String targetKey = mappingRules.getString(sourceKey);
                        if (itemMap.containsKey(sourceKey)) {
                            transformedItem.put(targetKey, itemMap.get(sourceKey));
                        }
                    }
                    
                    result.add(transformedItem);
                } else {
                    result.add(item);
                }
            }
            
            return result;
        } else {
            // 简单类型参数转换
            return parameters;
        }
    }
    
    /**
     * 执行字段过滤转换
     */
    private Object executeFieldFilter(JSONObject config, Object parameters) {
        // 1. 获取过滤规则
        JSONArray includeFields = config.getJSONArray("includeFields");
        JSONArray excludeFields = config.getJSONArray("excludeFields");
        
        // 2. 根据参数类型执行不同转换
        if (parameters instanceof Map) {
            // Map类型参数过滤
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> inputMap = (Map<String, Object>) parameters;
            
            // 3. 应用包含规则
            if (includeFields != null) {
                for (int i = 0; i < includeFields.length(); i++) {
                    String field = includeFields.getString(i);
                    if (inputMap.containsKey(field)) {
                        result.put(field, inputMap.get(field));
                    }
                }
            } else {
                // 如果没有包含规则，默认包含所有字段
                result.putAll(inputMap);
            }
            
            // 4. 应用排除规则
            if (excludeFields != null) {
                for (int i = 0; i < excludeFields.length(); i++) {
                    String field = excludeFields.getString(i);
                    result.remove(field);
                }
            }
            
            return result;
        } else if (parameters instanceof List) {
            // List类型参数过滤
            List<Object> result = new ArrayList<>();
            List<Object> inputList = (List<Object>) parameters;
            
            // 5. 应用过滤规则
            for (Object item : inputList) {
                if (item instanceof Map) {
                    Map<String, Object> filteredItem = new HashMap<>();
                    Map<String, Object> itemMap = (Map<String, Object>) item;
                    
                    // 应用包含规则
                    if (includeFields != null) {
                        for (int i = 0; i < includeFields.length(); i++) {
                            String field = includeFields.getString(i);
                            if (itemMap.containsKey(field)) {
                                filteredItem.put(field, itemMap.get(field));
                            }
                        }
                    } else {
                        // 如果没有包含规则，默认包含所有字段
                        filteredItem.putAll(itemMap);
                    }
                    
                    // 应用排除规则
                    if (excludeFields != null) {
                        for (int i = 0; i < excludeFields.length(); i++) {
                            String field = excludeFields.getString(i);
                            filteredItem.remove(field);
                        }
                    }
                    
                    result.add(filteredItem);
                } else {
                    result.add(item);
                }
            }
            
            return result;
        } else {
            // 简单类型参数转换
            return parameters;
        }
    }
    
    /**
     * 执行计算字段转换
     */
    private Object executeFieldCalculation(JSONObject config, Object parameters) {
        // 1. 获取计算规则
        JSONObject calculationRules = config.getJSONObject("rules");
        
        // 2. 根据参数类型执行不同转换
        if (parameters instanceof Map) {
            // Map类型参数计算
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> inputMap = (Map<String, Object>) parameters;
            
            // 3. 应用计算规则
            for (String targetField : calculationRules.keySet()) {
                JSONObject rule = calculationRules.getJSONObject(targetField);
                String expression = rule.getString("expression");
                
                // 简单表达式计算
                Object calculatedValue = evaluateExpression(expression, inputMap);
                result.put(targetField, calculatedValue);
            }
            
            return result;
        } else if (parameters instanceof List) {
            // List类型参数计算
            List<Object> result = new ArrayList<>();
            List<Object> inputList = (List<Object>) parameters;
            
            // 4. 应用计算规则
            for (Object item : inputList) {
                if (item instanceof Map) {
                    Map<String, Object> calculatedItem = new HashMap<>();
                    Map<String, Object> itemMap = (Map<String, Object>) item;
                    
                    for (String targetField : calculationRules.keySet()) {
                        JSONObject rule = calculationRules.getJSONObject(targetField);
                        String expression = rule.getString("expression");
                        
                        // 简单表达式计算
                        Object calculatedValue = evaluateExpression(expression, itemMap);
                        calculatedItem.put(targetField, calculatedValue);
                    }
                    
                    result.add(calculatedItem);
                } else {
                    result.add(item);
                }
            }
            
            return result;
        } else {
            // 简单类型参数转换
            return parameters;
        }
    }
    
    /**
     * 评估简单表达式
     */
    private Object evaluateExpression(String expression, Map<String, Object> context) {
        // 简单实现：替换变量并计算
        for (String var : context.keySet()) {
            expression = expression.replace("$" + var, context.get(var).toString());
        }
        
        try {
            // 使用ScriptEngine计算表达式
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("js");
            return engine.eval(expression);
        } catch (ScriptException e) {
            throw new RuntimeException("表达式计算失败: " + expression, e);
        }
    }
    
    /**
     * 执行条件判断节点
     */
    private Object executeConditional(ProcessNode node, Object parameters) {
        // 1. 解析条件配置
        JSONObject conditionConfig = node.getConfig();
        String conditionType = conditionConfig.getString("type", "SIMPLE");
        
        // 2. 根据条件类型执行不同评估逻辑
        boolean result;
        switch (conditionType) {
            case "COMPLEX":
                // 复杂逻辑表达式评估
                result = evaluateComplexCondition(conditionConfig, parameters);
                break;
                
            case "RANGE":
                // 范围条件评估
                result = evaluateRangeCondition(conditionConfig, parameters);
                break;
                
            case "REGEX":
                // 正则表达式匹配
                result = evaluateRegexCondition(conditionConfig, parameters);
                break;
                
            case "SCRIPT":
                // 脚本表达式评估
                result = evaluateScriptCondition(conditionConfig, parameters);
                break;
                
            case "COMPOSITE":
                // 复合条件评估
                result = evaluateCompositeCondition(conditionConfig, parameters);
                break;
                
            default:
                // 简单条件评估
                if (parameters instanceof Map) {
                    result = evaluateSimpleCondition(conditionConfig, (Map<String, Object>) parameters);
                } else {
                    result = evaluateSimpleValueCondition(conditionConfig, parameters);
                }
        }
        
        // 3. 返回对应结果
        return result ? conditionConfig.get("trueValue") : conditionConfig.get("falseValue");
    }
    
    /**
     * 评估脚本条件
     */
    private boolean evaluateScriptCondition(JSONObject conditionConfig, Object parameters) {
        String script = conditionConfig.getString("script");
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        
        try {
            // 设置参数
            if (parameters instanceof Map) {
                ((Map<String, Object>) parameters).forEach((k, v) -> engine.put(k, v));
            } else {
                engine.put("value", parameters);
            }
            
            // 执行脚本
            return (boolean) engine.eval(script);
        } catch (ScriptException e) {
            throw new RuntimeException("脚本条件评估失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 评估复合条件
     */
    private boolean evaluateCompositeCondition(JSONObject conditionConfig, Object parameters) {
        JSONArray conditions = conditionConfig.getJSONArray("conditions");
        String operator = conditionConfig.getString("operator", "AND");
        
        boolean result = "OR".equalsIgnoreCase(operator) ? false : true;
        
        for (int i = 0; i < conditions.size(); i++) {
            JSONObject cond = conditions.getJSONObject(i);
            boolean partialResult = evaluateCondition(cond, parameters);
            
            if ("OR".equalsIgnoreCase(operator)) {
                result = result || partialResult;
                if (result) break;
            } else {
                result = result && partialResult;
                if (!result) break;
            }
        }
        
        return result;
    }
    
    /**
     * 通用条件评估方法
     */
    private boolean evaluateCondition(JSONObject condition, Object parameters) {
        String type = condition.getString("type", "SIMPLE");
        
        switch (type) {
            case "COMPLEX":
                return evaluateComplexCondition(condition, parameters);
            case "RANGE":
                return evaluateRangeCondition(condition, parameters);
            case "REGEX":
                return evaluateRegexCondition(condition, parameters);
            case "SCRIPT":
                return evaluateScriptCondition(condition, parameters);
            default:
                if (parameters instanceof Map) {
                    return evaluateSimpleCondition(condition, (Map<String, Object>) parameters);
                } else {
                    return evaluateSimpleValueCondition(condition, parameters);
                }
        }
    }
    
    /**
     * 评估简单条件（Map类型参数）
     */
    private boolean evaluateSimpleCondition(JSONObject config, Map<String, Object> parameters) {
        String field = config.getString("field");
        String operator = config.getString("operator", "EXISTS");
        Object expectedValue = config.get("value");
        
        if (!parameters.containsKey(field)) {
            return false;
        }
        
        Object actualValue = parameters.get(field);
        if (actualValue == null) {
            return false;
        }
        
        switch (operator) {
            case "EQUALS":
                return actualValue.equals(expectedValue);
            case "NOT_EQUALS":
                return !actualValue.equals(expectedValue);
            case "CONTAINS":
                return actualValue.toString().contains(expectedValue.toString());
            case "STARTS_WITH":
                return actualValue.toString().startsWith(expectedValue.toString());
            case "ENDS_WITH":
                return actualValue.toString().endsWith(expectedValue.toString());
            default:
                // EXISTS
                return true;
        }
    }
    
    /**
     * 评估简单条件（值类型参数）
     */
    private boolean evaluateSimpleValueCondition(JSONObject config, Object parameter) {
        String operator = config.getString("operator", "EQUALS");
        Object expectedValue = config.get("value");
        
        if (parameter == null) {
            return false;
        }
        
        switch (operator) {
            case "EQUALS":
                return parameter.equals(expectedValue);
            case "NOT_EQUALS":
                return !parameter.equals(expectedValue);
            case "CONTAINS":
                return parameter.toString().contains(expectedValue.toString());
            case "STARTS_WITH":
                return parameter.toString().startsWith(expectedValue.toString());
            case "ENDS_WITH":
                return parameter.toString().endsWith(expectedValue.toString());
            default:
                return false;
        }
    }
    
    /**
     * 评估复杂逻辑表达式
     */
    private boolean evaluateComplexCondition(JSONObject config, Object parameters) {
        JSONArray conditions = config.getJSONArray("conditions");
        String logic = config.getString("logic", "AND");
        
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }
        
        boolean finalResult = "OR".equalsIgnoreCase(logic) ? false : true;
        
        for (int i = 0; i < conditions.length(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            boolean currentResult;
            
            if (parameters instanceof Map) {
                currentResult = evaluateSimpleCondition(condition, (Map<String, Object>) parameters);
            } else {
                currentResult = evaluateSimpleValueCondition(condition, parameters);
            }
            
            if ("OR".equalsIgnoreCase(logic)) {
                finalResult = finalResult || currentResult;
                if (finalResult) break;
            } else {
                finalResult = finalResult && currentResult;
                if (!finalResult) break;
            }
        }
        
        return finalResult;
    }
    
    /**
     * 评估范围条件
     */
    private boolean evaluateRangeCondition(JSONObject config, Object parameters) {
        String field = config.getString("field");
        Object minValue = config.get("min");
        Object maxValue = config.get("max");
        boolean includeMin = config.getBoolean("includeMin", true);
        boolean includeMax = config.getBoolean("includeMax", true);
        
        if (!(parameters instanceof Map)) {
            return false;
        }
        
        Map<String, Object> paramMap = (Map<String, Object>) parameters;
        if (!paramMap.containsKey(field) || paramMap.get(field) == null) {
            return false;
        }
        
        Object value = paramMap.get(field);
        
        try {
            double numValue = Double.parseDouble(value.toString());
            
            if (minValue != null) {
                double min = Double.parseDouble(minValue.toString());
                if (includeMin ? numValue < min : numValue <= min) {
                    return false;
                }
            }
            
            if (maxValue != null) {
                double max = Double.parseDouble(maxValue.toString());
                if (includeMax ? numValue > max : numValue >= max) {
                    return false;
                }
            }
            
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 评估正则表达式条件
     */
    private boolean evaluateRegexCondition(JSONObject config, Object parameters) {
        String field = config.getString("field");
        String pattern = config.getString("pattern");
        
        if (!(parameters instanceof Map)) {
            return false;
        }
        
        Map<String, Object> paramMap = (Map<String, Object>) parameters;
        if (!paramMap.containsKey(field) || paramMap.get(field) == null) {
            return false;
        }
        
        String value = paramMap.get(field).toString();
        return value.matches(pattern);
    }
}