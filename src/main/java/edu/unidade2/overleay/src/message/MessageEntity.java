package edu.unidade2.overleay.src.message;

import java.io.Serializable;

public class MessageEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private MessageType type;
    private Integer originNodeId;
    private Integer destinationNodeId;
    private String payload;

    public MessageEntity(MessageType type, Integer originNodeId, Integer destinationNodeId, String payload) {
        this.type = type;
        this.originNodeId = originNodeId;
        this.destinationNodeId = destinationNodeId;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Integer getOriginNodeId() {
        return originNodeId;
    }

    public void setOriginNodeId(Integer originNodeId) {
        this.originNodeId = originNodeId;
    }

    public Integer getDestinationNodeId() {
        return destinationNodeId;
    }

    public void setDestinationNodeId(Integer destinationNodeId) {
        this.destinationNodeId = destinationNodeId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
