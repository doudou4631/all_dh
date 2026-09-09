package com.geek.server.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeQueryServiceImplTest {

    @Test
    void treatsCancelledTeddyResultAsUnmarked() {
        assertFalse(FreeQueryServiceImpl.isMarked(
                "no-泰迪未来标记已取消，部分手机端可能需要3-5个工作日的同步时间，请耐心等待。"));
    }

    @Test
    void treatsNoPrefixAsUnmarkedCaseInsensitively() {
        assertFalse(FreeQueryServiceImpl.isMarked("NO-cancelled"));
    }

    @Test
    void treatsCancelledMarkMessageWithoutPrefixAsUnmarked() {
        assertFalse(FreeQueryServiceImpl.isMarked("泰迪未来标记已取消"));
    }

    @Test
    void keepsPositiveMarkResultMarked() {
        assertTrue(FreeQueryServiceImpl.isMarked("yes-骚扰电话"));
    }
}
