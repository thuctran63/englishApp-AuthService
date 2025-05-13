package englishapp.api.authservice.repositories;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import englishapp.api.authservice.models.RefreshToken;
import reactor.core.publisher.Mono;

@Repository
public interface RefreshTokenRepository extends ReactiveMongoRepository<RefreshToken, String> {

    @Query("{ 'rf_token': ?0 }")
    Mono<RefreshToken> findByRfToken(String token);

    @Query(value = "{ 'rf_token': ?0 }", delete = true)
    Mono<Long> deleteByRfToken(String rfToken);
}