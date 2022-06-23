package media.platform.rmqmsgsim.scenario;

import media.platform.rmqmsgsim.AppInstance;
import media.platform.rmqmsgsim.common.CalendarUtil;
import media.platform.rmqmsgsim.common.MsgTypeChecker;
import media.platform.rmqmsgsim.common.ServiceType;
import media.platform.rmqmsgsim.config.Config;
import media.platform.rmqmsgsim.json.JsonEnum;
import media.platform.rmqmsgsim.json.JsonMapper;
import media.platform.rmqmsgsim.json.JsonModifier;
import media.platform.rmqmsgsim.json.JsonParse;
import media.platform.rmqmsgsim.rmq.RmqManager;
import media.platform.rmqmsgsim.rmq.module.RmqClient;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author dajin kim
 */
public class ScenarioProcess {
    static final Logger log = LoggerFactory.getLogger(ScenarioProcess.class);

    private static ScenarioProcess scenarioProcess = null;
    private ScheduledExecutorService scheduleService;

    private final AppInstance instance = AppInstance.getInstance();
    private final Config config = instance.getConfig();
    private final JsonMapper jsonMapper = JsonMapper.getInstance();
    private final JsonModifier modifier = JsonModifier.getInstance();
    private final ScenarioManager scenarioManager = ScenarioManager.getInstance();
    private final RmqManager rmqManager = RmqManager.getInstance();

    private ScenarioProcess() {
        // Do Nothing
    }

    public static ScenarioProcess getInstance() {
        if (scenarioProcess == null)
            scenarioProcess = new ScenarioProcess();
        return scenarioProcess;
    }

    public void start() {
        if (scheduleService == null) {
            log.warn("START ScenarioProcess");

            ThreadFactory threadFactory = new BasicThreadFactory.Builder()
                    .namingPattern("ScenarioProcess" + "-%d")
                    .daemon(true)
                    .priority(Thread.MAX_PRIORITY)
                    .build();

            scheduleService = Executors.newScheduledThreadPool(config.getScenarioThreadSize(), threadFactory);
            scheduleService.scheduleAtFixedRate(() -> {
                for (int i = 1; i <= config.getCallRate(); i++) {
                    this.newScenario();
                }
            }, 1000 - CalendarUtil.getMilliSecond(), 1000, TimeUnit.MILLISECONDS);
        }
    }

    private void newScenario() {
        if (!scenarioManager.isTestValid()) return;

        ServiceType serviceType = instance.getServiceType();

/*        log.debug("[{}] newScenario, RecvType?:{}, amfLogin:{}, awfHb:{}",
                serviceType, scenarioManager.isRecvType(0), instance.isAmfLogin(), instance.isAwfHb());*/

        // send, pause 로 시작하는 시나리오 처리
        try {
            if (scenarioManager.isPauseType(0) || scenarioManager.isSendType(0)) {
                switch (serviceType) {
    /*                case A2S:
                        if (instance.isAmfLogin() && instance.isAwfHb()) {
                            process(0, null);
                        }
                        break;*/

                    case A2S:
                    case AMF:
                        if (instance.isAmfLogin()) {
                            process(0, null);
                        }
                        break;

                    case AWF:
                        if (instance.isAwfHb()) {
                            process(0, null);
                        }
                        break;

                    default:

                        break;
                }
            }
        } catch (Exception e) {
            log.error("ScenarioProcess.newScenario.Exception ", e);
        }
    }

    public void stop() {
        if (scheduleService != null) {
            log.error("STOP ScenarioProcess");
            scheduleService.shutdown();
        }
    }

    private void process(int msgIdx, String prevMsg) {
        // 시나리오 마지막 인덱스까지 진행된 메시지 체크
        if (scenarioManager.getScenarioSize() <= msgIdx) {
            scenarioManager.increaseEndCallNum();
            log.warn("End of Scenario (EndCall:{}, ConfigCall:{}, ScenarioSize:{}, Idx:{})",
                    scenarioManager.getEndCallNum(), config.getCallNum(), scenarioManager.getScenarioSize(), msgIdx);
            return;
        }

        // Send, Pause 메시지 처리
        if (scenarioManager.isSendType(msgIdx)) {
            procSend(msgIdx, prevMsg);
        } else if (scenarioManager.isPauseType(msgIdx)) {
            procPause(msgIdx, prevMsg);
        }
    }

    private void procPause(int msgIdx, String prevMsg) {
        // 시나리오 첫번째 인덱스 체크
        if (!checkCallNum(msgIdx)) return;
        int pauseTime = scenarioManager.getPauseTime(msgIdx);
        log.debug("ScenarioProcess [{}] Pause ({} Sec)", msgIdx, pauseTime);

        // 다음 시나리오 메시지 처리
        this.scheduleService.schedule(() -> process(msgIdx + 1, prevMsg),
                pauseTime, TimeUnit.SECONDS);
    }

    public void procRecv(int msgIdx, String prevMsg) {
        // 시나리오 첫번째 인덱스 체크
        if (!checkCallNum(msgIdx)) return;
        log.debug("ScenarioProcess [{}:{}] Recv", msgIdx, scenarioManager.getType(msgIdx));

        // 다음 시나리오 메시지 처리
        process(msgIdx + 1, prevMsg);
    }

    private void procSend(int msgIdx, String prevMsg) {
        // 시나리오 첫번째 인덱스 체크
        if (!checkCallNum(msgIdx)) return;
        log.debug("ScenarioProcess [{}:{}] Send", msgIdx, scenarioManager.getType(msgIdx));

        JSONObject jsonObject = scenarioManager.getScenario(msgIdx);

        if (jsonObject == null) {
            log.warn("ScenarioProcess.procSend {}-Scenario is Null", msgIdx);
            return;
        }

        String target = getSendQueue(msgIdx, prevMsg);
        String sendMsg = modifier.delDirectionField(jsonObject.toJSONString());
        sendMsg = jsonMapper.jsonMsgMapping(prevMsg, sendMsg);

        // rmqClient 이용해 메시지 전송
        //log.debug("ScenarioProcess [{}] SendQueue: {}", msgIdx, target);
        RmqClient rmqClient = rmqManager.getRmqClient(target);
        rmqClient.send(sendMsg);

        // 다음 시나리오 메시지 처리
        process(msgIdx + 1, sendMsg);
    }

    private String getSendQueue(int idx, String prevMsg) {
        // 현재 idx 보내려는 메시지 타입이 Res 라면 이전 Req 메시지에서 msgFrom 파싱
        if (prevMsg != null) {
            String msgType = scenarioManager.getType(idx);
            JsonParse jsonParse = JsonParse.getInstance();
            String prevMsgType = jsonParse.headerParse(prevMsg, JsonEnum.TYPE.getValue());
            //log.debug("prevMsgType: {} -> curMsgType: {}", prevMsgType, msgType);

/*            RmqConsumer.RmqParsing rmqParsing = new RmqConsumer.RmqParsing(prevMsg).invoke();
            RmqMessage rmqMsg = rmqParsing.getMsg();
            String prevMsgType = rmqMsg.getMsgType();*/

            if (MsgTypeChecker.checkResType(msgType) &&
                    MsgTypeChecker.checkReqType(prevMsgType)) {
                log.debug("get targetQueue from prevMsg msg_from");
                return jsonParse.headerParse(prevMsg, JsonEnum.MSG_FROM.getValue());
                //return rmqMsg.getHeader().getMsgFrom();
            }
        }

        // 시나리오 파일 direction 설정값 통해 getTargetQueue
        String sendModule  = scenarioManager.getSendModule(idx);
        if (sendModule != null) {
            sendModule = sendModule.toLowerCase();

            if (sendModule.contains("a2s")) {
                return config.getRmqA2S();
            } else if (sendModule.contains("amf")) {
                return config.getRmqAmf();
            } else if (sendModule.contains("awf")) {
                return config.getRmqAwf();
            }
        }
        return config.getRmqClientQueue();
    }

    private boolean checkCallNum(int msgIdx) {
        // 시나리오 첫번째 메시지를 처리하는 경우 totalCallNum 확인 후 CallNum 중가
        boolean result = true;
        //log.debug("checkCallNum index:{}, callNum:{}, configCallNum:{}", msgIdx, scenarioManager.getTestCallNum(), scenarioManager.getConfigCallNum());
        if (msgIdx == 0) {
            if (scenarioManager.isTestValid()) {
                scenarioManager.increaseCallNum();
            } else {
                result = false;
            }
            // todo log level?
            log.warn("SCENARIO START RESULT [{}], (CallNum: {}, ConfigCall: {})",
                    result, scenarioManager.getCallNum(), config.getCallNum());
        }
        return result;
    }

}
