package fr.plb.whatsapp.conversation.service;

import fr.plb.whatsapp.conversation.dto.message.MessageDTO;
import fr.plb.whatsapp.user.entity.User;
import fr.plb.whatsapp.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class MessageSenderService {

    private static final Logger log = LoggerFactory.getLogger(MessageSenderService.class);


    private UserService userService;

    public final Map<String, Sinks.Many<ServerSentEvent<MessageDTO>>> emitters = new HashMap<>();

    public MessageSenderService(UserService userService) {
        this.userService = userService;
    }

    private Flux<ServerSentEvent<MessageDTO>> keepAlive(Duration duration) {
        return Flux.interval(duration)
                .map(event -> ServerSentEvent.<MessageDTO>builder()
                        .event("keep alive")
                        .build());
    }

    public Flux<ServerSentEvent<MessageDTO>> subscribe() {
        Sinks.Many<ServerSentEvent<MessageDTO>> newSink = Sinks.many().unicast().onBackpressureBuffer();

        Flux<ServerSentEvent<MessageDTO>> newMessageStream = userService.getConnectedUser()
                .doOnNext(connectedUser -> this.emitters.put(connectedUser.getId(), newSink))
                .flatMapMany(connectedUser -> newSink.asFlux()
                        .doOnCancel(() -> this.emitters.remove(connectedUser.getId())))
                .doFinally(event -> newSink.tryEmitComplete());

        return Flux.merge(newMessageStream, keepAlive(Duration.ofSeconds(2)));
    }

    public Mono<MessageDTO> send(Set<User> members, MessageDTO messageDTO) {
        return Flux.fromIterable(members)
                .filter(user -> !messageDTO.getSenderId().equals(user.getId()))
                .filter(user -> emitters.containsKey(user.getId()))
                .flatMap(user -> {
                    Sinks.Many<ServerSentEvent<MessageDTO>> sink = emitters.get(user.getId());
                    ServerSentEvent<MessageDTO> newMessageEvent = ServerSentEvent.builder(messageDTO)
                            .event("new message")
                            .build();
                    return Mono.fromRunnable(() -> {
                        Sinks.EmitResult emitResult = sink.tryEmitNext(newMessageEvent);
                        if (emitResult.isSuccess()) {
                            log.info("Message sent to user id {}", user.getId());
                        } else {
                            log.warn("Fail to send message to {}", user.getId());
                        }
                    });
                }).then(Mono.just(messageDTO))
                .onErrorResume(error -> {
                    log.error("failed to send message due to error", error);
                    return Mono.empty();
                });
    }
}
