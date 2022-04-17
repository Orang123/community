package com.nowcoder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling//开启spring线程池的定时任务
@EnableAsync//开启异步任务注解
public class ThreadPoolcinfig {

}
