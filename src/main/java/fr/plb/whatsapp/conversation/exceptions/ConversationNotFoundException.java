package fr.plb.whatsapp.conversation.exceptions;

public class ConversationNotFoundException extends RuntimeException {
  public ConversationNotFoundException(String message) {
    super(message);
  }
}
