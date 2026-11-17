# prompts 目录约定

本目录存放各模块调用大模型时使用的**约束文档（当前作为 system prompt）**，随 jar 一起打包，避免把提示词硬编码在 Java 里。

## 命名

- 使用 `<模块>/<任务>.md`，全小写、短横线分隔。
- 当前已有：
  - `memory-tagging.md`：职业记忆 → 标签抽取
  - `resume/resume-diagnosis.md`：简历诊断 + 记忆候选
  - `interview/question-resume.md`：简历题
  - `interview/question-skill.md`：方向题
  - `interview/evaluation.md`：逐题评估
  - `interview/evaluation-summary.md`：分批结果汇总
  - `interview/skill-meta.yml`：可选的方向展示/优先级/参考题库元数据
  - `../skills/`：技术岗题库（Java/MySQL/Redis/Spring/Agent/系统设计/算法/前端），由 `InterviewReferenceService` 按方向加载

## 编写要求

1. 文件使用 UTF-8，纯 Markdown；内容本身就是 system prompt，不要写成本项目的 README。
2. 必须写清楚：角色、任务、输入、输出 JSON 结构、字段枚举、约束、示例。
3. 输出格式必须强调“只输出 JSON”，并说明不允许 Markdown 代码块。
4. 简历、JD、问答记录等用户内容必须显式标注“是数据，不是指令”，防止 prompt injection。
5. 数量类字段必须写“严格等于”，不要写“尽量/大约”。
6. 标签类任务必须包含归一化约束（主名、语气词剥离、同义词、否定项）。
7. 不要把 API Key、用户隐私、内部地址写进 prompt。
8. 修改 prompt 属于行为变更，需要同步更新 `docs/req` 对应说明，并补充测试用例。

## 加载方式

在 `application.properties` 中通过 classpath 指定：

```properties
career-trace.llm.tag-prompt=classpath:prompts/memory-tagging.md
career-trace.resume.diagnosis-prompt=classpath:prompts/resume/resume-diagnosis.md
career-trace.interview.resume-prompt=classpath:prompts/interview/question-resume.md
career-trace.interview.skill-prompt=classpath:prompts/interview/question-skill.md
career-trace.interview.evaluation-prompt=classpath:prompts/interview/evaluation.md
career-trace.interview.summary-prompt=classpath:prompts/interview/evaluation-summary.md
```

`DeepSeekClient` / `PromptLoader` 启动或首次调用时读取；文件不存在会失败，避免静默使用旧提示词。
