package common;

import java.io.Serializable;

public class Message implements Serializable {
    private final MessageType type;
    private final Object data;
    private final String description;

    public Message(MessageType type, Object data) {
        this(type, data, "");
    }

    public Message(MessageType type, Object data, String description) {
        this.type = type;
        this.data = data;
        this.description = description;
    }

    public MessageType getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Message{" + "type=" + type + ", description='" + description + "'}";
    }
}
