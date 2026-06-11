package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String answer;
    private String tags;
    private Integer difficulty;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
