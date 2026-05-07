package ma.sgitu.g8.config;

import ma.sgitu.g8.repository.EventRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackageClasses = EventRepository.class)
public class MongoConfig {
}
