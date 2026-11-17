package com.yuzhijie.career.trace.module.resume.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import com.yuzhijie.career.trace.module.resume.entity.Resume;
import com.yuzhijie.career.trace.module.resume.vo.ResumeAdviceVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeDetailVO;
import com.yuzhijie.career.trace.module.resume.vo.ResumeDimensionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 简历诊断 PDF 报告（OpenPDF + 中文字体）。
 */
@Slf4j
@Service
public class ResumePdfService {

    private final String fontPath;

    public ResumePdfService(@Value("${career-trace.resume.report-font:C:/Windows/Fonts/simhei.ttf}")
                            String fontPath) {
        this.fontPath = fontPath;
    }

    public byte[] render(Resume resume, ResumeDetailVO detail) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            BaseFont baseFont = loadFont();
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font sectionFont = new Font(baseFont, 13, Font.BOLD);
            Font bodyFont = new Font(baseFont, 10, Font.NORMAL);
            Font smallFont = new Font(baseFont, 9, Font.NORMAL);

            Document document = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(document, output);
            document.open();

            Paragraph title = new Paragraph("职迹 CareerTrace · 简历诊断报告", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph("文件名：" + safe(resume.getOriginalFilename()), bodyFont));
            document.add(new Paragraph("目标岗位 ID：" + (resume.getJobId() == null ? "未指定" : resume.getJobId()), bodyFont));
            document.add(new Paragraph("诊断总分：" + (detail.getOverallScore() == null ? "-" : detail.getOverallScore()) + " / 100", sectionFont));
            document.add(new Paragraph(" ", bodyFont));

            document.add(new Paragraph("一、五维得分", sectionFont));
            PdfPTable table = new PdfPTable(new float[]{2.2f, 1f, 1f, 5f});
            table.setWidthPercentage(100);
            addHeader(table, "维度", smallFont);
            addHeader(table, "得分", smallFont);
            addHeader(table, "满分", smallFont);
            addHeader(table, "评述", smallFont);
            if (detail.getDimensions() != null) {
                for (ResumeDimensionVO dimension : detail.getDimensions()) {
                    addCell(table, safe(dimension.getDimension()), smallFont);
                    addCell(table, String.valueOf(dimension.getScore()), smallFont);
                    addCell(table, String.valueOf(dimension.getMaxScore()), smallFont);
                    addCell(table, safe(dimension.getComment()), smallFont);
                }
            }
            document.add(table);
            document.add(new Paragraph(" ", bodyFont));

            document.add(new Paragraph("二、总结", sectionFont));
            document.add(new Paragraph(safe(detail.getSummary()), bodyFont));
            document.add(new Paragraph(" ", bodyFont));

            document.add(new Paragraph("三、优势", sectionFont));
            List<String> strengths = detail.getStrengths();
            if (strengths == null || strengths.isEmpty()) {
                document.add(new Paragraph("暂无", bodyFont));
            } else {
                for (String strength : strengths) {
                    document.add(new Paragraph("· " + safe(strength), bodyFont));
                }
            }
            document.add(new Paragraph(" ", bodyFont));

            document.add(new Paragraph("四、改进建议", sectionFont));
            List<ResumeAdviceVO> advices = detail.getAdvices();
            if (advices == null || advices.isEmpty()) {
                document.add(new Paragraph("暂无", bodyFont));
            } else {
                for (ResumeAdviceVO advice : advices) {
                    document.add(new Paragraph("【" + safe(advice.getPriority()) + "】"
                            + safe(advice.getCategory()) + "：" + safe(advice.getIssue()), bodyFont));
                    document.add(new Paragraph("建议：" + safe(advice.getRecommendation()), bodyFont));
                    if (advice.getEvidence() != null && !advice.getEvidence().isBlank()) {
                        document.add(new Paragraph("证据：" + safe(advice.getEvidence()), smallFont));
                    }
                    document.add(new Paragraph(" ", smallFont));
                }
            }

            document.close();
            return output.toByteArray();
        } catch (Exception e) {
            log.error("生成简历诊断 PDF 失败 resumeId={}", resume.getId(), e);
            throw BizException.internal(ErrorCodes.RESUME_DIAGNOSIS_FAILED, "PDF 报告生成失败");
        }
    }

    private BaseFont loadFont() throws Exception {
        Path path = Paths.get(fontPath);
        if (Files.exists(path)) {
            return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }
        // 兜底：使用 OpenPDF 内置的中文字体名；如果运行环境不支持会抛出明确异常
        return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
    }

    private static void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private static void addCell(PdfPTable table, String text, Font font) {
        table.addCell(new PdfPCell(new Phrase(text, font)));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
