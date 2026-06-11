package com.interviewai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interviewai.common.BizException;
import com.interviewai.common.ResultCode;
import com.interviewai.entity.Question;
import com.interviewai.entity.WrongQuestion;
import com.interviewai.mapper.QuestionMapper;
import com.interviewai.mapper.WrongQuestionMapper;
import com.interviewai.service.QuestionService;
import com.interviewai.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final WrongQuestionMapper wrongMapper;
    private final VectorSearchService vectorSearchService;

    @Override
    public Page<Question> listQuestions(String keyword, String tag, Integer difficulty, int page, int pageSize) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Question::getTitle, keyword)
                              .or().like(Question::getContent, keyword));
        }
        if (tag != null && !tag.isBlank()) wrapper.like(Question::getTags, tag);
        if (difficulty != null) wrapper.eq(Question::getDifficulty, difficulty);
        wrapper.orderByDesc(Question::getId);
        return questionMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Question getById(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) throw new BizException(ResultCode.QUESTION_NOT_FOUND);
        return q;
    }

    @Override
    public Question create(Question q) {
        questionMapper.insert(q);
        // 同步到 Qdrant
        try {
            vectorSearchService.storeQuestion(q.getId(), q.getTitle(), q.getContent());
            log.info("题目 {} 已同步到向量库", q.getId());
        } catch (Exception e) {
            log.error("题目 {} 向量化失败: {}", q.getId(), e.getMessage());
        }
        return q;
    }

    @Override
    public Question update(Question q) {
        questionMapper.updateById(q);
        // 题目内容变更时同步更新向量库
        try {
            vectorSearchService.storeQuestion(q.getId(), q.getTitle(), q.getContent());
            log.info("题目 {} 向量已更新", q.getId());
        } catch (Exception e) {
            log.error("题目 {} 向量更新失败: {}", q.getId(), e.getMessage());
        }
        return q;
    }

    @Override
    public void delete(Long id) {
        questionMapper.deleteById(id);
        // 注意：Qdrant 中的旧向量不会自动删除，但搜索时 MySQL 中已无对应记录，
        // 返回的 ID 查不到题目会被过滤掉。如需彻底清理，可通过 Qdrant WebUI 操作。
    }

    @Override
    public List<Map<String, Object>> getWrongList(Long userId) {
        LambdaQueryWrapper<WrongQuestion> w = new LambdaQueryWrapper<>();
        w.eq(WrongQuestion::getUserId, userId).orderByDesc(WrongQuestion::getCreatedAt);
        List<WrongQuestion> list = wrongMapper.selectList(w);

        // 按知识点聚合
        Map<String, Map<String, Object>> agg = new LinkedHashMap<>();
        for (WrongQuestion wq : list) {
            String kp = wq.getKnowledgePoint() != null ? wq.getKnowledgePoint() : "未分类";
            if (kp.length() > 50) kp = kp.substring(0, 50);
            if (agg.containsKey(kp)) {
                Map<String, Object> e = agg.get(kp);
                e.put("frequency", (int) e.get("frequency") + 1);
                e.put("latestId", wq.getId());
            } else {
                Map<String, Object> e = new HashMap<>();
                e.put("knowledgePoint", kp);
                e.put("frequency", 1);
                e.put("latestId", wq.getId());
                e.put("lastWrong", wq.getCreatedAt() != null ? wq.getCreatedAt().toString() : "");
                agg.put(kp, e);
            }
        }
        return new ArrayList<>(agg.values());
    }

    @Override
    public Map<String, Object> getWrongDetail(Long id, Long userId) {
        WrongQuestion wq = wrongMapper.selectById(id);
        if (wq == null || !wq.getUserId().equals(userId))
            throw new BizException(ResultCode.NOT_FOUND, "错题记录不存在");

        // 查同知识点的其他错题
        LambdaQueryWrapper<WrongQuestion> similar = new LambdaQueryWrapper<>();
        similar.eq(WrongQuestion::getUserId, userId)
               .eq(WrongQuestion::getKnowledgePoint, wq.getKnowledgePoint())
               .ne(WrongQuestion::getId, id)
               .orderByDesc(WrongQuestion::getCreatedAt);
        List<WrongQuestion> similarList = wrongMapper.selectList(similar);

        // 推荐相关题库题目
        List<Question> related = List.of();
        if (wq.getKnowledgePoint() != null) {
            related = questionMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .like(Question::getTags, wq.getKnowledgePoint())
                            .last("LIMIT 5"));
        }

        return Map.of("wrong", wq, "similarWrongs", similarList, "relatedQuestions", related);
    }

    @Override
    public void deleteWrong(Long id, Long userId) {
        WrongQuestion wq = wrongMapper.selectById(id);
        if (wq == null || !wq.getUserId().equals(userId))
            throw new BizException(ResultCode.NOT_FOUND, "错题记录不存在");
        wrongMapper.deleteById(id);
        log.info("用户 {} 标记错题 {} 为已掌握", userId, id);
    }
}
