package com.yuzhijie.career.trace.module.memory.support;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 打标任务状态（进程内）。
 *
 * <p>接口文档要求 taggingStatus 由 memory_tags 是否存在 + 异步任务状态推导，不新增字段；
 * 因此任务状态只保存在本进程内。应用重启后任务状态丢失，退化为：
 * 有标签 → DONE，无标签 → PENDING。</p>
 */
@Component
public class TaggingStatusStore {

    public static final String PENDING = "PENDING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";

    /** PENDING 超过该时长视为任务丢失 */
    private static final long PENDING_TIMEOUT_MS = 10 * 60 * 1000L;

    private final Map<Long, State> states = new ConcurrentHashMap<>();

    /**
     * 尝试占用任务：已有进行中的任务时返回 false（用于 40901）。
     */
    public boolean tryStart(Long memoryId) {
        long now = System.currentTimeMillis();
        State previous = states.get(memoryId);
        if (previous != null && PENDING.equals(previous.status()) && now - previous.startedAt() < PENDING_TIMEOUT_MS) {
            return false;
        }
        states.put(memoryId, new State(PENDING, now, 0L, null));
        return true;
    }

    public void markDone(Long memoryId) {
        long now = System.currentTimeMillis();
        states.put(memoryId, new State(DONE, now, now, null));
    }

    public void markFailed(Long memoryId, String error) {
        long now = System.currentTimeMillis();
        states.put(memoryId, new State(FAILED, now, now, error));
    }

    public void remove(Long memoryId) {
        states.remove(memoryId);
    }

    /**
     * 推导最终状态：内存状态优先；没有任务记录时按是否已有标签推断。
     */
    public String resolve(Long memoryId, boolean hasTags) {
        State state = states.get(memoryId);
        if (state != null) {
            if (PENDING.equals(state.status())
                    && System.currentTimeMillis() - state.startedAt() > PENDING_TIMEOUT_MS) {
                return FAILED;
            }
            return state.status();
        }
        return hasTags ? DONE : PENDING;
    }

    public String errorOf(Long memoryId) {
        State state = states.get(memoryId);
        return state == null ? null : state.error();
    }

    public record State(String status, long startedAt, long finishedAt, String error) {
    }
}
