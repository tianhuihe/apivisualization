package com.example.apivisualization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.apivisualization.entity.ProcessNode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程节点Mapper接口
 */
@Mapper
public interface ProcessNodeMapper extends BaseMapper<ProcessNode> {
}