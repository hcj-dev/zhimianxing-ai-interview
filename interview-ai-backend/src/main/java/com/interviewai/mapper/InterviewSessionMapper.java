package com.interviewai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewai.entity.InterviewSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {
}
