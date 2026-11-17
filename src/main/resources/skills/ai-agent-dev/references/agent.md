# AI Agent 开发参考题库

## Agent 基础
- Agent = LLM + Planning + Memory + Tools。
- Agent Loop：感知 → 推理 → 行动 → 观察；终止条件与最大步数。
- ReAct、Plan-and-Execute、Reflection 的适用场景与代价。
- 多 Agent：Orchestrator-Subagent、Peer-to-Peer、A2A。

## LLM 调用
- Token 预算：上下文窗口 = 输入 + 输出 + 历史 + RAG + 工具定义。
- Temperature/Top-p/惩罚项；JSON Mode 与结构化输出。
- Function Calling：Schema、并行调用、失败重试、幂等。
- 流式输出、TTFT、P99 延迟与成本优化。

## RAG
- 离线索引：加载、清洗、分块、Embedding、存储。
- 在线检索：查询改写、向量/关键词/混合检索、重排。
- 分块大小、Embedding 模型选择、召回率评估。
- 幻觉、上下文噪声、Lost in the Middle 的治理。

## 上下文工程与 MCP
- 上下文压缩、摘要、裁剪、渐进式披露。
- MCP：Resources/Tools/Prompts/Sampling；stdio 与 Streamable HTTP。
- 工具权限、超时、重试、审计；Prompt Injection 防御。

## 出题模板
- Agent 如何防止无限循环？终止条件怎么设计？
- RAG 召回率低/幻觉高，怎么定位是检索还是生成问题？
- 工具调用失败/重复调用，如何保证幂等和可恢复？
- 上下文超窗时，你保留什么、丢弃什么？依据是什么？
