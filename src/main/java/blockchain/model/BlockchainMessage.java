package blockchain.model;

import consensus.crypto.StringUtils;

public class BlockchainMessage {
    private final MessageType messageType;
    private final String jsonData;

    public BlockchainMessage(MessageType messageType, Object jsonData) {
        this.messageType = messageType;
        this.jsonData = StringUtils.toJson(jsonData);
    }

    public BlockchainMessage(MessageType messageType) {
        this.messageType = messageType;
        this.jsonData = null;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getJsonData() {
        return jsonData;
    }
}