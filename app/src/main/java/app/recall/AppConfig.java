package app.recall;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** Everything asks this bean for the time, so tests can swap in a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
