package app.recall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entry point. Because this class sits in the app.recall package, Spring finds every
 * component, entity and repository in the other modules (app.recall.web, app.recall.persistence, ...).
 */
@SpringBootApplication
public class RecallApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecallApplication.class, args);
    }
}
