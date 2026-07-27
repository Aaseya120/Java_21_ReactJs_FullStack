package com.demo.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a distributed lock should be acquired before executing the method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * The prefix for the lock key.
     * @return the prefix
     */
    String keyPrefix();

    /**
     * The name of the method parameter to use as the lock key suffix.
     * @return the parameter name
     */
    String keyParam() default "id";

    /**
     * Maximum time to wait for the lock in seconds.
     * @return wait time
     */
    long waitTime() default 5;

    /**
     * Maximum time to hold the lock in seconds.
     * @return lease time
     */
    long leaseTime() default 10;
}
