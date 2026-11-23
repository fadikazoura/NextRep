package com.fadikazoura.nextrep.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} jobs (the demo-account cleanup). */
@Configuration
@EnableScheduling
class SchedulingConfig {
}
