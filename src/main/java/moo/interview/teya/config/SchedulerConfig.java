package moo.interview.teya.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class to enable Spring scheduling for scheduled tasks
 * such as the message queue processor.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}

