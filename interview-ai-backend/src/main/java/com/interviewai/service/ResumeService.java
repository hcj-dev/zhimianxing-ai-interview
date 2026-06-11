package com.interviewai.service;

import com.interviewai.entity.JobDescription;
import com.interviewai.entity.Resume;
import com.interviewai.entity.ResumeAnalysis;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ResumeService {
    List<Map<String, Object>> listByUser(Long userId);
    Resume getById(Long id);
    void delete(Long id, Long userId);
    Map<String, Object> upload(Long userId, MultipartFile file);
    ResumeAnalysis getAnalysis(Long resumeId);
    String exportMarkdown(Long resumeId);
    Map<String, Object> submitJdMatch(Long userId, Long resumeId, String jdText);
    List<JobDescription> getJdMatchResults(Long resumeId);
}
