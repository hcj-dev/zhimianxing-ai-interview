package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interview_message")
public class InterviewMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer sequence;
    private String role;
    private String content;
    private Long questionId;
    private Integer score;
    private String feedback;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
