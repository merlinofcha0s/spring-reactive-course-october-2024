package fr.plb.whatsapp.conversation.resource;

import fr.plb.whatsapp.IntegrationTest;
import fr.plb.whatsapp.conversation.dto.message.*;
import fr.plb.whatsapp.conversation.entity.Conversation;
import fr.plb.whatsapp.conversation.entity.ConversationBuilder;
import fr.plb.whatsapp.conversation.entity.MessageType;
import fr.plb.whatsapp.conversation.repository.ConversationRepository;
import fr.plb.whatsapp.conversation.repository.MessageRepository;
import fr.plb.whatsapp.conversation.service.MessageSenderService;
import fr.plb.whatsapp.user.entity.User;
import fr.plb.whatsapp.user.entity.UserBuilder;
import fr.plb.whatsapp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.HashSet;
import java.util.Set;


public class MessageResourceIT extends IntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageSenderService messageSenderService;

    private Conversation newConversation;

    private final static String DEFAULT_EMAIL = "johnathan.doe@example.com";
    private final static String DEFAULT_CONVERSATION_ID = "671a03d18e072f22e1e69566";
    private final static String DEFAULT_USER_ID = "6718f38a63c97e5b2160120a";
    private final static String DEFAULT_USER_ID_2 = "6718f45e63c97e5b2160120b";


    @BeforeEach
    public void setUp() {
        conversationRepository.deleteAll().block();
        userRepository.deleteAll().block();
        messageRepository.deleteAll().block();

        User user = UserBuilder.user()
                .id(DEFAULT_USER_ID)
                .email(DEFAULT_EMAIL)
                .build();

        userRepository.save(user).block();

        newConversation = ConversationBuilder.conversation()
                .id(DEFAULT_CONVERSATION_ID)
                .name("My conversation")
                .users(Set.of(user))
                .build();

        conversationRepository.save(newConversation).block();
    }

    @Test
    @WithMockUser(username = DEFAULT_EMAIL)
    public void createAndSendMessage_ShouldWork() throws JsonProcessingException {
        MessageContentDTO content = MessageContentDTOBuilder.messageContentDTO()
                .text("Hello, this is a text message!")
                .media(null)
                .type(MessageType.TEXT)
                .build();

        MessageSendNewDTO sendNewMessageDTO = MessageSendNewDTOBuilder.messageSendNewDTO()
                .content(content)
                .conversationPublicId(newConversation.getId())
                .build();

        webTestClient.post()
                .uri("/api/messages/send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(sendNewMessageDTO))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.textContent").isEqualTo("Hello, this is a text message!")
                .jsonPath("$.type").isEqualTo("TEXT")
                .jsonPath("$.conversationId").isEqualTo(newConversation.getId());
    }

    @Test
    @WithMockUser(username = DEFAULT_EMAIL)
    void sendMessageToNonSenderUsers() {
       User  user1 = new User();
        user1.setId("user1");

        User user2 = new User();
        user2.setId("user2");

        MessageDTO messageDTO = new MessageDTOBuilder()
                .senderId("user1")
                .build();

        Set<User> members = new HashSet<>();
        members.add(user1);
        members.add(user2);

        Sinks.Many<ServerSentEvent<MessageDTO>> user2Sink = Sinks.many().unicast().onBackpressureBuffer();
        messageSenderService.emitters.put(user2.getId(), user2Sink);

        Mono<MessageDTO> resultMono = messageSenderService.send(members, messageDTO);

        StepVerifier.create(user2Sink.asFlux())
                .expectNextMatches(sse -> sse.data().equals(messageDTO))
                .thenCancel()
                .verify();

        StepVerifier.create(resultMono)
                .expectNext(messageDTO)
                .verifyComplete();
    }
}
