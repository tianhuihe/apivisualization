package com.example.apivisualization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.apivisualization.entity.ProcessDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程定义Mapper接口
 */
@Mapper
public interface ProcessDefinitionMapper extends BaseMapper<ProcessDefinition> {
}