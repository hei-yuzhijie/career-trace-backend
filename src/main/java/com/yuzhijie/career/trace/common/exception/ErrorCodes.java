package com.yuzhijie.career.trace.common.exception;

/**
 * 业务错误码（与接口文档“错误码汇总”保持一致）。
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int INTERNAL_ERROR = 500;

    /** 记忆原文超长 */
    public static final int MEMORY_CONTENT_TOO_LONG = 40001;
    /** 熟练度越界 */
    public static final int PROFICIENCY_OUT_OF_RANGE = 40002;
    /** 重复提交 */
    public static final int DUPLICATE_SUBMIT = 40003;
    /** 记忆不存在 */
    public static final int MEMORY_NOT_FOUND = 40401;
    /** 岗位不存在 */
    public static final int JOB_NOT_FOUND = 40402;
    /** memory_tags 记录不存在 */
    public static final int MEMORY_TAG_NOT_FOUND = 40403;
    /** 打标任务进行中 */
    public static final int TAGGING_IN_PROGRESS = 40901;
    /** 大模型调用失败 */
    public static final int LLM_CALL_FAILED = 50001;
    /** 知识库检索失败 */
    public static final int KNOWLEDGE_RETRIEVAL_FAILED = 50002;
}
