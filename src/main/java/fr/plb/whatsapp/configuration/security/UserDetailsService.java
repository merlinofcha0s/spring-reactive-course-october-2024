package fr.plb.whatsapp.configuration.security;

import fr.plb.whatsapp.user.entity.Authority;
import fr.plb.whatsapp.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class UserDetailsService implements ReactiveUserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsService.class);

    private final UserRepository userRepository;

    public UserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String email) {
        log.debug("Authenticating {}", email);
        return userRepository
                .findOneByEmailIgnoreCase(email)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User with email was not found in the db " + email)))
                .map(this::createSpringSecurityUser);
    }

    private User createSpringSecurityUser(fr.plb.whatsapp.user.entity.User user) {
        List<SimpleGrantedAuthority> grantedAuthorities = user.getAuthorities()
                .stream()
                .map(Authority::getName)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new User(user.getEmail(), user.getPassword(), grantedAuthorities);
    }

}
