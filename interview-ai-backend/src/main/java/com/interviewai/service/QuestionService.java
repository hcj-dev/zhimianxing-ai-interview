package com.interviewai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interviewai.entity.Question;

import java.util.List;
import java.util.Map;

public interface QuestionService {
    Page<Question> listQuestions(String keyword, String tag, Integer difficulty, int page, int pageSize);
    Question getById(Long id);
    Question create(Question question);
    Question update(Question question);
    void delete(Long id);
    List<Map<String, Object>> getWrongList(Long userId);
    Map<String, Object> getWrongDetail(Long id, Long userId);
    void deleteWrong(Long id, Long userId);
}
