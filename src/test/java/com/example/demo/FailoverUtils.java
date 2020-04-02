package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class FailoverUtils {
    public static final Logger LOGGER = LoggerFactory.getLogger(FailoverUtils.class);

    public static <T> T retry(int times, Supplier<T> function, int waitSec){
        Throwable saved=null;
        for (int i=1; i<=times && !Thread.currentThread().isInterrupted(); ++i) {
            try {
                LOGGER.info("Trying {}/{} to perform action {}", i, times, function);
                return function.get();
            } catch (Throwable e) {
                LOGGER.warn("Next attempt to perform action {}", function);
                saved = e;
                try {
                    TimeUnit.SECONDS.sleep(waitSec);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
        }
        throw new RuntimeException("Couldn't successful perform action "+function+" after "+times+" times", saved);
    }

}
