package com.interviewai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewai.entity.JobDescription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobDescriptionMapper extends BaseMapper<JobDescription> {
}
