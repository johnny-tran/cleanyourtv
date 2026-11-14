package com.gathin.tvhome;

import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class PopupRenderTest {
    @Test public void materialJobsLeaveTheCallerAndShareOneWorker() throws Exception {
        Thread caller = Thread.currentThread();
        Thread first = PopupMaterial.RENDERER.submit(Thread::currentThread).get(2, TimeUnit.SECONDS);
        Thread second = PopupMaterial.RENDERER.submit(Thread::currentThread).get(2, TimeUnit.SECONDS);
        assertNotSame(caller, first);
        assertSame(first, second);
    }
}
