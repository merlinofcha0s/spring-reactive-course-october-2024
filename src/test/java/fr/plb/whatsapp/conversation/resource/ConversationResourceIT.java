package fr.plb.whatsapp.conversation.resource;

import fr.plb.whatsapp.IntegrationTest;
import fr.plb.whatsapp.conversation.dto.conversation.ConversationDTO;
import fr.plb.whatsapp.conversation.dto.conversation.ConversationToCreateDTO;
import fr.plb.whatsapp.conversation.dto.conversation.ConversationToCreateDTOBuilder;
import fr.plb.whatsapp.conversation.entity.Conversation;
import fr.plb.whatsapp.conversation.entity.ConversationBuilder;
import fr.plb.whatsapp.conversation.repository.ConversationRepository;
import fr.plb.whatsapp.user.entity.User;
import fr.plb.whatsapp.user.entity.UserBuilder;
import fr.plb.whatsapp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.hasItems;

public class ConversationResourceIT extends IntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserRepository userRepository;

    private ConversationToCreateDTO conversationToCreateDTO;

    private final static String DEFAULT_EMAIL = "johnathan.doe@example.com";
    private final static String DEFAULT_CONVERSATION_ID = "671a03d18e072f22e1e69566";
    private final static String DEFAULT_USER_ID = "6718f38a63c97e5b2160120a";
    private final static String DEFAULT_USER_ID_2 = "6718f45e63c97e5b2160120b";

    private User user;

    @BeforeEach
    public void createConversation() {
        conversationToCreateDTO = ConversationToCreateDTOBuilder.conversationToCreateDTO()
                .name("Default conversation")
                .members(Set.of(DEFAULT_USER_ID, DEFAULT_USER_ID_2))
                .build();
    }

    @BeforeEach
    public void setup() {
        conversationRepository.deleteAll().block();
        userRepository.deleteAll().block();

        user = UserBuilder.user()
                .id(DEFAULT_USER_ID)
                .email(DEFAULT_EMAIL)
                .build();

        userRepository.save(user).block();

        Conversation newConversation = ConversationBuilder.conversation()
                .id(DEFAULT_CONVERSATION_ID)
                .name("My conversation")
                .users(Set.of(user))
                .build();

        conversationRepository.save(newConversation).block();
    }

    @Test
    @WithMockUser
    public void create_ShouldWork() throws JsonProcessingException {
        webTestClient
                .post()
                .uri("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(conversationToCreateDTO))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.name")
                .isEqualTo(conversationToCreateDTO.name())
                .jsonPath("$.members.[*].publicId")
                .value(hasItems(DEFAULT_USER_ID, DEFAULT_USER_ID_2));
    }

    @Test
    @WithMockUser(username = DEFAULT_EMAIL)
    public void getAll_shouldReturnConversations() {
        Conversation newConversation = ConversationBuilder.conversation()
                .name("Test Conversation 2")
                .users(Set.of(user))
                .build();

        conversationRepository.save(newConversation).block();

        webTestClient.get().uri("/api/conversations")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ConversationDTO.class)
                .hasSize(2);
    }

    @Test
    @WithMockUser(username = DEFAULT_EMAIL)
    public void getOneByPublicId_shouldReturnConversation() {
        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/api/conversations/get-one-by-public-id")
                        .queryParam("conversationId", DEFAULT_CONVERSATION_ID).build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConversationDTO.class)
                .value(response -> {
                    assertThat(response.name()).isEqualTo("My conversation");
                });
    }

    @Test
    @WithMockUser(username = DEFAULT_EMAIL)
    public void getOneByPublicId_shouldReturnBadRequest() {
        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/api/conversations/get-one-by-public-id")
                        .queryParam("conversationId", "dummy-id").build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @WithMockUser(username = DEFAULT_EMAIL)
    public void delete_shouldDeleteConversation() {
        webTestClient.delete().uri(uriBuilder -> uriBuilder
                        .path("/api/conversations")
                        .queryParam("publicId", DEFAULT_CONVERSATION_ID).build())
                .exchange()
                .expectStatus().isOk();

        assertThat(conversationRepository.findAll().collectList().block().isEmpty()).isTrue();
    }
}
