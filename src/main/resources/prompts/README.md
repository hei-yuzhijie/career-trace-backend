# prompts 目录约定

本目录存放各模块调用大模型时使用的**约束文档（system prompt）**，随 jar 一起打包，避免把提示词硬编码在 Java 里。

## 命名

- 使用 `<模块>-<任务>.md`，全小写、短横线分隔。
- 当前已有：
  - `memory-tagging.md`：职业记忆 → 标签抽取
- 后续可扩展：
  - `job-tagging.md`：岗位 JD → 岗位标签抽取
  - `memory-summary.md`：职业记忆摘要
  - `resume-parse.md`：简历解析

## 编写要求

1. 文件使用 UTF-8，纯 Markdown；内容本身就是 system prompt，不要写成本项目的 README。
2. 必须写清楚：角色、输入、输出 JSON 结构、字段枚举、约束、反例/示例。
3. 输出格式必须强调“只输出 JSON”，并说明不允许 Markdown 代码块。
4. 标签类任务必须包含归一化约束（主名、语气词剥离、同义词、否定项）。
5. 不要把 API Key、用户隐私、内部地址写进 prompt。
6. 修改 prompt 属于线上行为变更，需要同步修改 `docs/req/岗位匹配功能模块说明文档.md` 并补充测试用例。

## 加载方式

在 `application.properties` 中通过 classpath 指定：

```properties
career-trace.llm.tag-prompt=classpath:prompts/memory-tagging.md
```

`DeepSeekClient` 启动时读取并作为 system message 发送；文件不存在会启动失败，避免静默使用旧提示词。
