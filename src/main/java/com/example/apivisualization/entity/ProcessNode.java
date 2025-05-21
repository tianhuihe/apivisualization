package com.example.apivisualization.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 流程节点实体类
 */
@Data
@TableName("process_node")
public class ProcessNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long processDefinitionId;
    
    private String name;
    
    private String type;
    
    private String config;
    
    private Integer sort;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
    
    @TableLogic
    private Integer deleted;
}