package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wrong_question")
public class WrongQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sessionId;
    private String knowledgePoint;
    private String questionText;
    private String userAnswer;
    private Integer score;
    private String feedback;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
