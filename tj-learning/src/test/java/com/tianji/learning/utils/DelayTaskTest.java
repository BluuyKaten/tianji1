package com.tianji.learning.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.DelayQueue;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class DelayTaskTest {
    @Test
    void testDelayQueue() throws InterruptedException {
        //1、初始化延迟队列
        DelayQueue<DelayTask<String>> queue = new DelayQueue<>();
        //2、添加延迟任务
        log.info("开始初始化延迟任务。。。。");
        queue.add(new DelayTask<>("task1",Duration.ofSeconds(3)));
        queue.add(new DelayTask<>("task2",Duration.ofSeconds(1)));
        queue.add(new DelayTask<>("task3",Duration.ofSeconds(2)));
        //3.尝试执行延迟任务
        while (true){
            DelayTask<String> task = queue.take();
            log.info("开始执行延迟任务:{}",task.getData());
        }
    }
}