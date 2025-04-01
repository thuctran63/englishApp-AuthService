package englishapp.api.authservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import englishapp.api.authservice.database.repositories.UserRepository;

@Service
public class UserService implements ReactiveUserDetailsService {

    @Autowired
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUserName(username)
                .map(user -> new CustomUserDetail(user))
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found")));
    }
    
    
}
