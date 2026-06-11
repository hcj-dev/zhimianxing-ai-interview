package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interview_session")
public class InterviewSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private String mode;
    private String techTags;
    private String difficulty;
    private String status;
    private Integer totalQuestions;
    private Integer answeredCount;
    private Double totalScore;
    private String strengths;
    private String weaknesses;
    private String radarData;
    private Integer duration;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
