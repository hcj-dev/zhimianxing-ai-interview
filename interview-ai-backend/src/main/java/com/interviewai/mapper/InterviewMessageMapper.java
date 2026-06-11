package com.interviewai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewai.entity.InterviewMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewMessageMapper extends BaseMapper<InterviewMessage> {
}
