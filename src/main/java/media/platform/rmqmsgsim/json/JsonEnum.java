package media.platform.rmqmsgsim.json;

public enum JsonEnum {
    FILE_EXTENSION(".json"), REQ("req"), RES("res"),
    DIRECTION("direction"), MSG("msg"),
    HEADER("header"), BODY("body"),
    TYPE("type"), REASON_CODE("reasonCode"), TRANSACTION_ID("transactionId"), MSG_FROM("msgFrom"), TIMESTAMP("timestamp"),
    TASK_ID("taskId"), CALL_ID("callId"), AMF_ID("amf_id");

    final String value;

    public String getValue() {
        return value;
    }

    JsonEnum(String value) {
        this.value = value;
    }
}
