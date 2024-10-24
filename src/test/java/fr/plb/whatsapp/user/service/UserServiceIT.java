package fr.plb.whatsapp.user.service;

import fr.plb.whatsapp.IntegrationTest;
import fr.plb.whatsapp.user.dto.UserDTO;
import fr.plb.whatsapp.user.entity.User;
import fr.plb.whatsapp.user.exceptions.EmailAlreadyUsedException;
import fr.plb.whatsapp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class UserServiceIT extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    private final static String DEFAULT_EMAIL = "johnathan.doe@example.com";
    private final static String DEFAULT_PASSWORD = "password";

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll().block();
    }

    @Test
    public void registerUser_shouldWork() {
        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName("John");
        userDTO.setLastName("Doe");
        userDTO.setEmail(DEFAULT_EMAIL);

        Mono<User> userRegistration = userService.registerUser(userDTO, DEFAULT_PASSWORD);

        StepVerifier.create(userRegistration)
                .expectNextMatches(userToVerify -> userToVerify.getEmail().equals(DEFAULT_EMAIL))
                .verifyComplete();

        StepVerifier.create(userRegistration)
                .expectError(EmailAlreadyUsedException.class)
                .verify();
    }


    @Test
    @WithMockUser(username = DEFAULT_EMAIL)
    public void getConnectedUser_shouldWork() {
        User user = new User();
        user.setFirstName("Bob");
        user.setLastName("Marley");
        user.setEmail(DEFAULT_EMAIL);
        user.setPassword(passwordEncoder.encode("password"));

        userService.save(user).block();

        StepVerifier.create(userService.getConnectedUser())
                .expectNextMatches(connectedUser -> connectedUser.getEmail().equals(DEFAULT_EMAIL))
                .verifyComplete();
    }

    @Test
    void getall_shouldWork() {
        User user1 = new User();
        user1.setFirstName("John");
        user1.setLastName("Smith");
        user1.setEmail(DEFAULT_EMAIL);
        user1.setPassword(passwordEncoder.encode("password"));

        String emailDefaultUser2 = "alice.johnson@example.com";

        User user2 = new User();
        user2.setFirstName("Alice");
        user2.setLastName("Johnson");
        user2.setEmail(emailDefaultUser2);
        user2.setPassword(passwordEncoder.encode("password"));

        userService.save(user1)
                .then(userService.save(user2))
                .block();

        StepVerifier.create(userService.getAll())
                .assertNext(userToVerify ->
                        assertThat(userToVerify.getEmail()).isEqualTo(DEFAULT_EMAIL))
                .assertNext(userToVerify ->
                        assertThat(userToVerify.getEmail()).isEqualTo(emailDefaultUser2))
                .verifyComplete();
    }

}
