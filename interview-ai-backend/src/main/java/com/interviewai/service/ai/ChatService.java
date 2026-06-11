package com.interviewai.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI对话服务，封装所有调用DeepSeek的场景。
 * 每个方法对应一个具体的AI任务，Prompt内嵌在方法中便于调试和迭代。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * 从简历文本中提取结构化信息。
     */
    public Map<String, Object> extractResumeInfo(String rawText) {
        String prompt = """
                你是一位专业的简历分析师。请从以下简历文本中提取结构化信息，仅返回JSON格式，不要加任何其他文字。

                简历文本：
                %s

                返回格式（必须是严格的JSON）：
                {
                    "name": "姓名",
                    "school": "学校",
                    "major": "专业",
                    "skills": ["技能1", "技能2"],
                    "experiences": [{"company": "公司", "role": "岗位", "description": "工作描述"}],
                    "projects": [{"name": "项目名", "techStack": ["技术"], "description": "项目描述"}]
                }
                """.formatted(rawText);

        String response = chatClient.prompt().user(prompt).call().content();
        return parseJson(response);
    }

    /**
     * 生成简历分析报告：评分 + 优势 + 不足 + 建议 + 优化后的简历。
     */
    public Map<String, Object> analyzeResume(String rawText, String structuredInfo) {
        String prompt = """
                你是一位资深技术面试官和简历顾问。请分析以下简历并生成详细报告。

                简历原文：
                %s

                已提取的结构化信息：
                %s

                请从以下四个维度评分（0-100分）：
                1. 技术栈完整度：技能列表是否全面、是否有层次
                2. 描述质量：项目/实习经历是否有量化结果、是否突出个人贡献
                3. 关键词覆盖度：是否包含行业热门技术关键词
                4. 格式排版：结构是否清晰、重点是否突出

                另外请输出：
                - strengths：2-3条优势，每条包含point和detail
                - weaknesses：2-3条不足，每条包含point和detail
                - suggestions：3-5条修改建议，每条包含type（high/medium/low）和content
                - optimizedResume：优化后的简历Markdown文本

                仅返回JSON，不要加任何其他文字：
                {
                    "skillScore": 数字,
                    "descriptionQuality": 数字,
                    "keywordCoverage": 数字,
                    "formatScore": 数字,
                    "overallScore": 数字,
                    "strengths": [{"point": "...", "detail": "..."}],
                    "weaknesses": [{"point": "...", "detail": "..."}],
                    "suggestions": [{"type": "high|medium|low", "content": "..."}],
                    "optimizedResume": "Markdown文本"
                }
                """.formatted(rawText, structuredInfo);

        String response = chatClient.prompt().user(prompt).call().content();
        return parseJson(response);
    }

    /**
     * JD与简历匹配分析：技能差距 + 修改建议。
     */
    public Map<String, Object> matchJD(String resumeText, String jdText) {
        String prompt = """
                你是一位HR和技术面试官。请对比以下简历和岗位描述（JD），分析匹配度并给出改进建议。

                简历内容：
                %s

                岗位描述（JD）：
                %s

                请分析：
                1. 匹配度评分（0-100）
                2. 技能差距列表（每项包含skill、required要求、current当前水平）
                3. 简历修改建议列表

                仅返回JSON，不要加任何其他文字：
                {
                    "matchScore": 数字,
                    "skillGap": [{"skill": "技能名", "required": "岗位要求水平", "current": "你的当前水平"}],
                    "suggestions": ["建议1", "建议2"]
                }
                """.formatted(resumeText, jdText);

        String response = chatClient.prompt().user(prompt).call().content();
        return parseJson(response);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String response) {
        try {
            // 处理可能的markdown代码块包裹
            String json = response.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            } else if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            return objectMapper.readValue(json.trim(), Map.class);
        } catch (JsonProcessingException e) {
            log.error("AI返回JSON解析失败, response: {}", response, e);
            throw new RuntimeException("AI分析结果解析失败，请稍后重试", e);
        }
    }
}
