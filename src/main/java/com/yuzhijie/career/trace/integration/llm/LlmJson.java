package com.yuzhijie.career.trace.integration.llm;

/**
 * 从大模型返回内容中提取 JSON 对象，兼容 ```json 代码块和前后解释文字。
 */
public final class LlmJson {

    private LlmJson() {
    }

    public static String extractObject(String content) {
        if (content == null) {
            throw new IllegalArgumentException("大模型返回内容为空");
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("大模型返回内容不是 JSON 对象");
        }
        return text.substring(start, end + 1);
    }
}
