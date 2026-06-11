package com.interviewai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interviewai.common.BizException;
import com.interviewai.common.ResultCode;
import com.interviewai.config.RabbitMQConfig;
import com.interviewai.entity.JobDescription;
import com.interviewai.entity.Resume;
import com.interviewai.entity.ResumeAnalysis;
import com.interviewai.mapper.JobDescriptionMapper;
import com.interviewai.mapper.ResumeAnalysisMapper;
import com.interviewai.mapper.ResumeMapper;
import com.interviewai.service.FileService;
import com.interviewai.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;
    private final ResumeAnalysisMapper analysisMapper;
    private final JobDescriptionMapper jdMapper;
    private final FileService fileService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public List<Map<String, Object>> listByUser(Long userId) {
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getUserId, userId)
               .orderByDesc(Resume::getCreatedAt);
        List<Resume> resumes = resumeMapper.selectList(wrapper);

        // 批量查询每个简历的分析分数
        List<Map<String, Object>> result = new ArrayList<>();
        for (Resume r : resumes) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("userId", r.getUserId());
            item.put("fileName", r.getFileName());
            item.put("fileUrl", r.getFileUrl());
            item.put("status", r.getStatus());
            item.put("version", r.getVersion());
            item.put("createdAt", r.getCreatedAt());
            item.put("updatedAt", r.getUpdatedAt());

            ResumeAnalysis analysis = analysisMapper.selectOne(
                    new LambdaQueryWrapper<ResumeAnalysis>()
                            .eq(ResumeAnalysis::getResumeId, r.getId()));
            item.put("overallScore", analysis != null ? analysis.getOverallScore() : null);

            result.add(item);
        }
        return result;
    }

    @Override
    public Resume getById(Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null) throw new BizException(ResultCode.RESUME_NOT_FOUND);
        return resume;
    }

    @Override
    public void delete(Long id, Long userId) {
        Resume resume = getById(id);
        if (!resume.getUserId().equals(userId))
            throw new BizException(ResultCode.FORBIDDEN, "只能删除自己的简历");
        analysisMapper.delete(new LambdaQueryWrapper<ResumeAnalysis>()
                .eq(ResumeAnalysis::getResumeId, id));
        resumeMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> upload(Long userId, MultipartFile file) {
        String fileUrl = fileService.save(file, "resumes");
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(file.getOriginalFilename());
        resume.setFileUrl(fileUrl);
        resume.setStatus("PROCESSING");
        resume.setVersion(1);
        resumeMapper.insert(resume);

        String filePath = fileService.getFilePath(fileUrl).toString();
        rabbitTemplate.convertAndSend(RabbitMQConfig.RESUME_ANALYSIS_QUEUE,
                Map.of("resumeId", resume.getId(), "filePath", filePath));

        log.info("简历上传成功, resumeId={}", resume.getId());
        return Map.of("taskId", String.valueOf(resume.getId()), "status", "PROCESSING");
    }

    @Override
    public ResumeAnalysis getAnalysis(Long resumeId) {
        ResumeAnalysis analysis = analysisMapper.selectOne(
                new LambdaQueryWrapper<ResumeAnalysis>()
                        .eq(ResumeAnalysis::getResumeId, resumeId));
        if (analysis == null)
            throw new BizException(ResultCode.NOT_FOUND, "分析报告尚未生成");
        return analysis;
    }

    @Override
    public String exportMarkdown(Long resumeId) {
        ResumeAnalysis analysis = getAnalysis(resumeId);
        if (analysis.getOptimizedResume() == null)
            throw new BizException(ResultCode.NOT_FOUND, "优化简历尚未生成");
        return analysis.getOptimizedResume();
    }

    @Override
    public Map<String, Object> submitJdMatch(Long userId, Long resumeId, String jdText) {
        JobDescription jd = new JobDescription();
        jd.setResumeId(resumeId);
        jd.setUserId(userId);
        jd.setJdText(jdText);
        jd.setStatus("PROCESSING");
        jdMapper.insert(jd);

        rabbitTemplate.convertAndSend(RabbitMQConfig.JD_MATCH_QUEUE,
                Map.of("jdId", jd.getId(), "resumeId", resumeId));
        return Map.of("taskId", String.valueOf(jd.getId()), "status", "PROCESSING");
    }

    @Override
    public List<JobDescription> getJdMatchResults(Long resumeId) {
        return jdMapper.selectList(new LambdaQueryWrapper<JobDescription>()
                .eq(JobDescription::getResumeId, resumeId)
                .orderByDesc(JobDescription::getCreatedAt));
    }
}
