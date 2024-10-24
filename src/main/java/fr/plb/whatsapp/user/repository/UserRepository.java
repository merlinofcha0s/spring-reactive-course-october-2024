package fr.plb.whatsapp.user.repository;

import fr.plb.whatsapp.user.entity.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface UserRepository extends ReactiveMongoRepository<User, String> {

    Mono<User> findByEmail(String email);

    Flux<User> findByIdIn(Set<String> publicIds);

    Mono<User> findOneByEmailIgnoreCase(String email);
}
