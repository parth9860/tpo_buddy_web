package com.tpobuddy.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Chat Message model for AI Placement Assistant conversations.
 */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String sender; // "user" or "assistant"
    private String text;
    private long timestamp;
    private List<String> suggestions = new ArrayList<>();

    public ChatMessage() {
    }

    public ChatMessage(String id, String sender, String text, long timestamp) {
        this.id = id;
        this.sender = sender;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
