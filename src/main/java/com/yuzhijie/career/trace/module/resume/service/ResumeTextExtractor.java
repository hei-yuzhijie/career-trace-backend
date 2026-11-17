package com.yuzhijie.career.trace.module.resume.service;

import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * 使用 Apache Tika 抽取 PDF/DOCX/TXT/MD 纯文本。
 */
@Service
public class ResumeTextExtractor {

    private final Tika tika = new Tika();

    public ResumeTextExtractor() {
        tika.setMaxStringLength(200_000);
    }

    public String extract(Path path) {
        try {
            String text = tika.parseToString(path.toFile());
            if (text == null || text.isBlank()) {
                throw BizException.internal(ErrorCodes.RESUME_PARSE_FAILED,
                        "简历解析为空，请上传可复制文本的 PDF/DOCX");
            }
            return text.trim();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw BizException.internal(ErrorCodes.RESUME_PARSE_FAILED, "简历解析失败，请检查文件格式");
        }
    }
}
