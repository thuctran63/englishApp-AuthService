package englishapp.api.authservice.repositories;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import englishapp.api.authservice.models.RefreshToken;
import reactor.core.publisher.Mono;

@Repository
public interface RefreshTokenRepository extends ReactiveMongoRepository<RefreshToken, String> {

    Mono<RefreshToken> findByToken(String token);

    Mono<Void> deleteByUserId(String userId);
}