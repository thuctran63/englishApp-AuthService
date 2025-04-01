package englishapp.api.authservice.database.repositories;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import englishapp.api.authservice.database.models.User;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findByUserName(String userName);
    Mono<User> findByEmail(String email);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
}
