package com.interviewai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interviewai.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
