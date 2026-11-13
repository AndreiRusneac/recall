package app.recall.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * This module has no main class, so the persistence tests use this one.
 * Spring finds it by searching up from the test's package, and scans entities and repositories from here.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class PersistenceTestApplication {
}
