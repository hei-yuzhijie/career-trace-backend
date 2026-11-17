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

    /** 简历文件非法 */
    public static final int RESUME_FILE_INVALID = 40004;
    /** 面试题量/难度等参数非法 */
    public static final int INTERVIEW_PARAM_INVALID = 40005;
    /** 答案题号非法 */
    public static final int ANSWER_INDEX_INVALID = 40006;
    /** 方向标签非法 */
    public static final int DIRECTION_INVALID = 40007;
    /** 答案超长 */
    public static final int ANSWER_TOO_LONG = 40008;
    /** 没有可评估答案 */
    public static final int NO_ANSWERS = 40009;

    /** 简历不存在 */
    public static final int RESUME_NOT_FOUND = 40404;
    /** 面试会话不存在 */
    public static final int INTERVIEW_NOT_FOUND = 40405;
    /** 面试记忆候选不存在 */
    public static final int INTERVIEW_CANDIDATE_NOT_FOUND = 40406;
    /** 简历记忆候选不存在 */
    public static final int RESUME_CANDIDATE_NOT_FOUND = 40407;

    /** 简历分析中 */
    public static final int RESUME_PROCESSING = 40902;
    /** 面试状态不允许 */
    public static final int INTERVIEW_STATE_INVALID = 40903;
    /** 面试正在处理中 */
    public static final int INTERVIEW_PROCESSING = 40904;
    /** 简历尚未完成诊断（导出） */
    public static final int RESUME_REPORT_NOT_READY = 40905;
    /** 简历尚未完成诊断（导入） */
    public static final int RESUME_IMPORT_NOT_READY = 40906;
    /** 面试尚未完成评估 */
    public static final int INTERVIEW_NOT_READY = 40908;
    /** 记忆候选已处理 */
    public static final int CANDIDATE_ALREADY_PROCESSED = 40909;

    /** 简历解析失败 */
    public static final int RESUME_PARSE_FAILED = 50003;
    /** 简历诊断失败 */
    public static final int RESUME_DIAGNOSIS_FAILED = 50004;
    /** 面试出题失败 */
    public static final int INTERVIEW_GENERATE_FAILED = 50005;
    /** 面试评估失败 */
    public static final int INTERVIEW_EVALUATION_FAILED = 50006;
}
