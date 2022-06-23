package media.platform.rmqmsgsim.json;

import media.platform.rmqmsgsim.AppInstance;
import media.platform.rmqmsgsim.common.DateFormatUtil;
import media.platform.rmqmsgsim.common.MsgTypeChecker;
import media.platform.rmqmsgsim.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

/**
 * @author dajin kim
 */
public class JsonMapper {
    private static final Logger log = LoggerFactory.getLogger(JsonMapper.class);
    private static JsonMapper jsonMapper = null;

    private final AppInstance instance = AppInstance.getInstance();
    private final JsonParse jsonParse = JsonParse.getInstance();
    private final JsonModifier jsonModifier = JsonModifier.getInstance();

    private JsonMapper() {
        // Do Nothing
    }

    public static JsonMapper getInstance() {
        if (jsonMapper == null)
            jsonMapper = new JsonMapper();
        return jsonMapper;
    }

    public String jsonMsgMapping(String prevMsg, String sendMsg) {

        // header
        sendMsg = jsonHeaderMapping(prevMsg, sendMsg);
        // body
        sendMsg = jsonBodyMapping(prevMsg, sendMsg);

        return sendMsg;
    }

    public String jsonHeaderMapping(String prevMsg, String sendMsg) {
        // header
        for (String headerKey : instance.getHeaderList()) {
            sendMsg = jsonHeaderProc(prevMsg, sendMsg, headerKey);
        }
        return sendMsg;
    }

    private String jsonBodyMapping(String prevMsg, String sendMsg) {
        for (String bodyKey : instance.getBodyList()) {
            sendMsg = jsonBodyProc(prevMsg, sendMsg, bodyKey);
        }
        return sendMsg;
    }

    private String jsonHeaderProc(String prevMsg, String sendMsg, String headerKey) {
        switch (headerKey) {
            case "transactionId":
                String tId;
                if (MsgTypeChecker.checkReqType(sendMsg)) {
                    tId = UUID.randomUUID().toString();
                } else {
                    tId = jsonParse.headerParse(prevMsg, headerKey);
                }
                sendMsg = jsonModifier.headerModifying(sendMsg, headerKey, tId);
                break;
            case "timestamp":
                String date = DateFormatUtil.formatYmdHmsS(System.currentTimeMillis());
                sendMsg = jsonModifier.headerModifying(sendMsg, headerKey, date);
                break;
            case "msgFrom":
                Config config = AppInstance.getInstance().getConfig();
                sendMsg = jsonModifier.headerModifying(sendMsg, headerKey, config.getRmqServerQueue());
                break;
            default:
                log.trace("JsonMapper.jsonHeaderProc - {} case not defined", headerKey);
                break;
        }

        return sendMsg;
    }

    private String jsonBodyProc(String prevMsg, String sendMsg, String bodyKey) {
        // sendMsg body 에 bodyKey 존재하고 value 공란일때
        String sendBodyValue = jsonParse.bodyParse(sendMsg, bodyKey);
        if (sendBodyValue != null && sendBodyValue.isEmpty()) {

            // prevMsg 에서 bodyField 값 파싱
            String prevBodyValue = jsonParse.bodyParse(prevMsg, bodyKey);
            if (prevBodyValue != null) {
                try {
                    // Integer Value
                    int intValue = Integer.parseInt(prevBodyValue);
                    sendMsg = jsonModifier.bodyModifying(sendMsg, bodyKey, intValue);
                } catch (NumberFormatException e) {
                    // String Value
                    log.trace("JsonMapper.jsonMsgMapping.ParseInt [{}]", prevBodyValue);
                    sendMsg = jsonModifier.bodyModifying(sendMsg, bodyKey, prevBodyValue);
                }
            } else {
                // 이전 메시지에 bodyField 가 존재하지 않을때
                // 이전 메시지 존재하지 않는 첫번째 메시지 처리
                sendMsg = createBodyValue(sendMsg, bodyKey);
            }
        }

        return sendMsg;
    }

    private String createBodyValue(String sendMsg, String bodyKey) {
        String id = UUID.randomUUID().toString();
        sendMsg = jsonModifier.bodyModifying(sendMsg, bodyKey, id);

        return sendMsg;
    }

}
