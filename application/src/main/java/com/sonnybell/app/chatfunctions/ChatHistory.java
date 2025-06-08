package com.sonnybell.app.chatfunctions;

import java.util.LinkedList;
import java.util.List;

/**
 * ChatHistory class to manage the history of messages.
 * It stores a limited number of messages and provides methods to add and
 * retrieve them.
 * This class is thread-safe and ensures that the message history is
 * synchronized
 * across multiple threads.
 * Uses a LinkedList to store messages, allowing for efficient addition and
 * removal
 * of messages.
 * The maximum number of messages stored is defined by the MAX_HISTORY constant.
 * When the maximum is reached, the oldest message is removed to make space for
 * the new one.
 * This ensures that the message history does not grow indefinitely and consumes
 * excessive memory.
 */
public final class ChatHistory {

    private static final int MAX_HISTORY = 100;
    private static final LinkedList<String> MESSAGE_HISTORY = new LinkedList<>();

    private ChatHistory() {
        // Prevent instantiation
    }

    /**
     * Adds a message to the history.
     * If the history is full, the oldest message is removed.
     *
     * @param message The message to add to the history.
     */
    public static synchronized void addMessageToHistory(String message) {
        if (MESSAGE_HISTORY.size() >= MAX_HISTORY) {
            MESSAGE_HISTORY.removeFirst();
        }
        MESSAGE_HISTORY.add(message);
    }

    /**
     * Retrieves the message history.
     * Returns a copy of the message history to ensure thread safety.
     *
     * @return A list of messages in the history.
     */
    public static synchronized List<String> getMessageHistory() {
        return new LinkedList<>(MESSAGE_HISTORY);
    }
}
