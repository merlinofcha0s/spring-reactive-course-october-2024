package fr.plb.whatsapp.conversation.exceptions;

public class ConversationAlreadyExist extends RuntimeException {
    public ConversationAlreadyExist(String message) {
        super(message);
    }
}
