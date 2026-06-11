package com.interviewai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resume_analysis")
public class ResumeAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private Integer skillScore;
    private Integer descriptionQuality;
    private Integer keywordCoverage;
    private Integer formatScore;
    private Integer overallScore;
    private String suggestions;
    private String strengths;
    private String weaknesses;
    private String optimizedResume;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
