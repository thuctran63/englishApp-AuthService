package englishapp.api.authservice.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import englishapp.api.authservice.database.models.User;
import englishapp.api.authservice.database.repositories.UserRepository;
import englishapp.api.authservice.dto.register.BodyParamRegister;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Mono<User> findByUsername(String username) {
        return userRepository.findByUserName(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found with username: " + username)));
    }

    public Mono<User> registerUser(BodyParamRegister request){
        return userRepository.findByEmail(request.getEmail()).flatMap(existingUser -> Mono.error(new RuntimeException("Email already exists")))
                .then(userRepository.findByUserName(request.getUserName()).flatMap(existingUser -> Mono.error(new RuntimeException("Username already exists"))))
                .then(Mono.defer(() -> {
                    User user = new User();
                    user.setTypeUser(request.getTypeUser());
                    user.setUserName(request.getUserName());
                    user.setEmail(request.getEmail());
                    user.setPassword(request.getPassword());
                    user.setCreatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                }));
    }
}
