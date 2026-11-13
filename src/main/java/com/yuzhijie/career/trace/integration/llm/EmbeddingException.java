package com.yuzhijie.career.trace.integration.llm;

/**
 * 向量化调用失败。
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
