package com.interviewai.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * PDF文本提取服务。使用Apache PDFBox解析简历文件。
 */
@Slf4j
@Service
public class PdfService {

    /**
     * 从PDF文件中提取纯文本。
     * PDFBox的PDFTextStripper按阅读顺序输出文本，基本保留了段落结构。
     */
    public String extractText(File pdfFile) {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("PDF解析失败: {}", pdfFile.getAbsolutePath(), e);
            throw new RuntimeException("PDF解析失败: " + e.getMessage());
        }
    }
}
