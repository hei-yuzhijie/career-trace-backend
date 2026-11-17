# Java 后端参考题库

## 核心考点

### Java 基础
- `==` 与 `equals`、`hashCode` 契约；包装类缓存。
- 重载与重写、静态分派与动态分派。
- 接口与抽象类、default 方法。
- 异常体系、受检/非受检异常、try-with-resources。

### 集合
- `ArrayList` 扩容、`LinkedList` 适用场景、`RandomAccess`。
- `HashMap` 数组+链表+红黑树、扩容、负载因子、线程不安全。
- `ConcurrentHashMap` JDK 7/8 实现差异、CAS、size 统计。
- `fail-fast` 与 `fail-safe`。

### 并发
- JMM、`volatile`、`synchronized` 锁升级、`ReentrantLock`。
- CAS/ABA、`Atomic`、AQS、`ThreadLocal` 内存泄漏。
- 线程池核心参数、拒绝策略、动态调整、父子任务隔离。
- `CompletableFuture` 编排、异常处理、自定义线程池。
- 死锁条件、排查（jstack/arthas）、预防。

### JVM
- 运行时数据区、对象创建与内存布局。
- GC 判定、分代收集、G1/ZGC 适用场景。
- 类加载与双亲委派、打破方式。
- OOM/GC 频繁排查：dump、jstat、GC 日志、MAT。

## 出题模板
- 这个机制底层怎么实现？性能代价是什么？
- 多线程下会有什么问题？如何保证线程安全？
- 线上出现 OOM/CPU 飙高/接口变慢，怎么定位？
- 你的方案在流量翻 3 倍时先扩展哪里？依据是什么？
