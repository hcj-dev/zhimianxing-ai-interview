package com.interviewai.controller;

import com.interviewai.common.Result;
import com.interviewai.entity.JobDescription;
import com.interviewai.entity.Resume;
import com.interviewai.entity.ResumeAnalysis;
import com.interviewai.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(resumeService.listByUser(userId));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Resume resume = resumeService.getById(id);
        ResumeAnalysis analysis = null;
        try {
            analysis = resumeService.getAnalysis(id);
        } catch (Exception ignored) {
            // 分析报告可能还在生成中
        }
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("resume", resume);
        data.put("analysis", analysis);
        return Result.ok(data);
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(resumeService.upload(userId, file));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        resumeService.delete(id, userId);
        return Result.ok();
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportMarkdown(@PathVariable Long id) {
        String markdown = resumeService.exportMarkdown(id);
        byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume.md")
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .body(bytes);
    }

    @PostMapping("/{id}/match-jd")
    public Result<Map<String, Object>> matchJD(@PathVariable Long id,
                                                @RequestBody Map<String, String> body,
                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String jdText = body.get("jdText");
        return Result.ok(resumeService.submitJdMatch(userId, id, jdText));
    }

    @GetMapping("/{id}/match-jd")
    public Result<List<JobDescription>> getJdMatchResults(@PathVariable Long id) {
        return Result.ok(resumeService.getJdMatchResults(id));
    }
}
