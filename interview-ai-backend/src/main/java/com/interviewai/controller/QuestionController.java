package com.interviewai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interviewai.common.BizException;
import com.interviewai.common.Result;
import com.interviewai.common.ResultCode;
import com.interviewai.entity.Question;
import com.interviewai.entity.User;
import com.interviewai.mapper.UserMapper;
import com.interviewai.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final UserMapper userMapper;

    private void requireAdmin(Authentication auth) {
        if (auth == null) throw new BizException(ResultCode.UNAUTHORIZED);
        User user = userMapper.selectById((Long) auth.getPrincipal());
        if (user == null || !"ADMIN".equals(user.getRole()))
            throw new BizException(ResultCode.FORBIDDEN);
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Question> result = questionService.listQuestions(keyword, tag, difficulty, page, pageSize);
        return Result.ok(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "pageSize", result.getSize()));
    }

    @GetMapping("/wrong")
    public Result<Map<String, Object>> wrongList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        List<Map<String, Object>> records = questionService.getWrongList(userId);
        int total = records.size();
        // 简单分页
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> paged = from < total ? records.subList(from, to) : List.of();
        return Result.ok(Map.of(
                "records", paged,
                "total", total, "page", page, "pageSize", pageSize));
    }

    @GetMapping("/{id}")
    public Result<Question> getById(@PathVariable Long id) {
        return Result.ok(questionService.getById(id));
    }

    @PostMapping
    public Result<Question> create(@RequestBody Question question, Authentication auth) {
        requireAdmin(auth);
        return Result.ok(questionService.create(question));
    }

    @PutMapping("/{id}")
    public Result<Question> update(@PathVariable Long id, @RequestBody Question question, Authentication auth) {
        requireAdmin(auth);
        question.setId(id);
        return Result.ok(questionService.update(question));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        requireAdmin(auth);
        questionService.delete(id);
        return Result.ok();
    }

    @GetMapping("/wrong/{id}")
    public Result<Map<String, Object>> wrongDetail(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(questionService.getWrongDetail(id, userId));
    }

    @DeleteMapping("/wrong/{id}")
    public Result<Void> deleteWrong(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        questionService.deleteWrong(id, userId);
        return Result.ok();
    }
}
