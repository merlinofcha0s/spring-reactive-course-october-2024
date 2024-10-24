package fr.plb.whatsapp.user.service;

import fr.plb.whatsapp.configuration.security.AuthoritiesConstants;
import fr.plb.whatsapp.configuration.security.SecurityUtils;
import fr.plb.whatsapp.user.dto.UserDTO;
import fr.plb.whatsapp.user.entity.Authority;
import fr.plb.whatsapp.user.entity.User;
import fr.plb.whatsapp.user.exceptions.EmailAlreadyUsedException;
import fr.plb.whatsapp.user.repository.AuthorityRepository;
import fr.plb.whatsapp.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final AuthorityRepository authorityRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, AuthorityRepository authorityRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<User> save(User user) {
        return userRepository.save(user);
    }

    public Flux<User> getAll() {
        return userRepository.findAll();
    }

    public Mono<User> getConnectedUser() {
        return SecurityUtils.getCurrentUserEmail()
                .flatMap(userRepository::findOneByEmailIgnoreCase);
    }

    public Mono<User> registerUser(UserDTO userDTO, String password) {
        return userRepository.findOneByEmailIgnoreCase(userDTO.getEmail())
                .flatMap(existingUser -> Mono.error(new EmailAlreadyUsedException()))
                .then(Mono.fromCallable(() -> {
                            User newUser = new User();
                            String encryptedPassword = passwordEncoder.encode(password);
                            newUser.setPassword(encryptedPassword);
                            newUser.setFirstName(userDTO.getFirstName());
                            newUser.setLastName(userDTO.getLastName());
                            newUser.setEmail(userDTO.getEmail());
                            newUser.setImageUrl(userDTO.getImageUrl());
                            return newUser;
                        })
                        .flatMap(newUser -> {
                            Set<Authority> authorities = new HashSet<>();
                            return authorityRepository.findById(AuthoritiesConstants.USER)
                                    .map(authorities::add)
                                    .thenReturn(newUser)
                                    .doOnNext(user -> user.setAuthorities(authorities))
                                    .flatMap(this::save)
                                    .doOnNext(user -> log.debug(""));
                        }));

    }
}
