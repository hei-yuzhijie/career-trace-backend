package com.yuzhijie.career.trace.integration.llm;

import lombok.Getter;

/**
 * 大模型调用失败（网络、鉴权、返回格式等）。
 *
 * <p>retryable 用于打标重试：鉴权/参数错误不重试，超时/限流/5xx/解析失败可重试。</p>
 */
@Getter
public class LlmException extends RuntimeException {

    private final boolean retryable;

    public LlmException(String message) {
        this(message, true);
    }

    public LlmException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public LlmException(String message, Throwable cause) {
        this(message, cause, true);
    }

    public LlmException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }
}
