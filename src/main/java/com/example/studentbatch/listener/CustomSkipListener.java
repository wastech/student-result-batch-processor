package com.example.studentbatch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Component
public class CustomSkipListener implements SkipListener<Object, Object> {

    private static final Logger log = LoggerFactory.getLogger(CustomSkipListener.class);

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("🚨 SKIP IN READ - Exception: {}, Message: {}",
            t.getClass().getSimpleName(), t.getMessage(), t);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.error("🚨 SKIP IN WRITE - Item: {}, Exception: {}, Message: {}",
            item, t.getClass().getSimpleName(), t.getMessage(), t);
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.error("🚨 SKIP IN PROCESS - Item: {}, Exception: {}, Message: {}",
            item, t.getClass().getSimpleName(), t.getMessage(), t);
    }
}