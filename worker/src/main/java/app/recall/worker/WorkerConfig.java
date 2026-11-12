package app.recall.worker;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on Spring's scheduler so methods marked @Scheduled run. */
@Configuration
@EnableScheduling
public class WorkerConfig {
}
