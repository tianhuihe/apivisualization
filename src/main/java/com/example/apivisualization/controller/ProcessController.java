package com.example.apivisualization.controller;

import com.example.apivisualization.entity.ProcessDefinition;
import com.example.apivisualization.entity.ProcessNode;
import com.example.apivisualization.service.ProcessService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程管理控制器
 */
@RestController
@RequestMapping("/api/process")
@Api(tags = "流程管理API")
public class ProcessController {
    @Autowired
    private ProcessService processService;
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * 创建流程定义
     * @param processDefinition 流程定义实体
     * @return 创建后的流程定义
     */
    @PostMapping("/definitions")
    @ApiOperation("创建流程定义")
    public ProcessDefinition createProcessDefinition(@Valid @RequestBody ProcessDefinition processDefinition) {
        return processService.createProcessDefinition(processDefinition);
    }

    /**
     * 更新流程定义
     * @param processDefinition 流程定义实体
     * @return 更新后的流程定义
     */
    @PutMapping("/definitions")
    @ApiOperation("更新流程定义")
    public ProcessDefinition updateProcessDefinition(@Valid @RequestBody ProcessDefinition processDefinition) {
        return processService.updateProcessDefinition(processDefinition);
    }

    /**
     * 删除流程定义
     * @param id 流程定义ID
     */
    @DeleteMapping("/definitions/{id}")
    @ApiOperation("删除流程定义")
    public void deleteProcessDefinition(@PathVariable Long id) {
        processService.deleteProcessDefinition(id);
    }

    /**
     * 获取流程定义详情
     * @param id 流程定义ID
     * @return 流程定义实体
     */
    @GetMapping("/definitions/{id}")
    @ApiOperation("获取流程定义详情")
    public ProcessDefinition getProcessDefinition(@PathVariable Long id) {
        return processService.getProcessDefinition(id);
    }

    /**
     * 添加流程节点
     * @param processNode 流程节点实体
     * @return 添加后的流程节点
     */
    @PostMapping("/nodes")
    @ApiOperation("添加流程节点")
    public ProcessNode addProcessNode(@Valid @RequestBody ProcessNode processNode) {
        return processService.addProcessNode(processNode);
    }

    /**
     * 更新流程节点
     * @param processNode 流程节点实体
     * @return 更新后的流程节点
     */
    @PutMapping("/nodes")
    @ApiOperation("更新流程节点")
    public ProcessNode updateProcessNode(@Valid @RequestBody ProcessNode processNode) {
        return processService.updateProcessNode(processNode);
    }

    /**
     * 删除流程节点
     * @param id 流程节点ID
     */
    @DeleteMapping("/nodes/{id}")
    @ApiOperation("删除流程节点")
    public void deleteProcessNode(@PathVariable Long id) {
        processService.deleteProcessNode(id);
    }

    /**
     * 获取流程节点列表
     * @param processDefinitionId 流程定义ID
     * @return 流程节点列表
     */
    @GetMapping("/nodes")
    @ApiOperation("获取流程节点列表")
    public List<ProcessNode> listProcessNodes(@RequestParam Long processDefinitionId) {
        return processService.listProcessNodes(processDefinitionId);
    }

    /**
     * 执行流程编排
     * @param processDefinitionId 流程定义ID
     * @param parameters 执行参数
     * @return 执行结果
     */
    @PostMapping("/execute/{processDefinitionId}")
    @ApiOperation("执行流程编排")
    public Object executeProcess(@PathVariable Long processDefinitionId, @RequestBody Object parameters) {
        return processService.executeProcess(processDefinitionId, parameters);
    }
}