package com.yuzhijie.career.trace.common.util;

import com.yuzhijie.career.trace.common.exception.BizException;

/**
 * 分页参数校验：page 从 1 开始，size 1-50，非法时返回 400。
 */
public final class Pages {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    private Pages() {
    }

    public static int page(Integer page) {
        if (page == null) {
            return 1;
        }
        if (page < 1) {
            throw BizException.badRequest("分页参数非法");
        }
        return page;
    }

    public static int size(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw BizException.badRequest("分页参数非法");
        }
        return size;
    }
}
