package media.platform.rmqmsgsim.rmq.module;

import media.platform.rmqmsgsim.common.MsgTypeChecker;
import media.platform.rmqmsgsim.common.PrintMsgLog;
import media.platform.rmqmsgsim.service.HeartbeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import media.platform.rmqmsgsim.common.SleepUtil;
import media.platform.rmqmsgsim.rmq.types.RmqMessage;
import media.platform.rmqmsgsim.scenario.ScenarioManager;
import media.platform.rmqmsgsim.scenario.ScenarioProcess;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * RmqReceiver 가 BlockingQueue 에 넣어놓은 메시지 처리
 * */
public class RmqConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(RmqConsumer.class);

    private final ScenarioManager scenarioManager = ScenarioManager.getInstance();
    private final ScenarioProcess scenarioProcess = ScenarioProcess.getInstance();

    private final BlockingQueue<String> queue;
    private boolean isQuit = false;

    public RmqConsumer(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        queueProcessing();
    }

    private void queueProcessing(){
        while (!isQuit){
            try {
                String msg = queue.poll(10, TimeUnit.MILLISECONDS);

                if (msg == null){
                    SleepUtil.trySleep(10);
                    continue;
                }
                parseRmqMsg(msg);

            }catch (InterruptedException e){
                log.error("RmqConsumer.queueProcessing",e);
                isQuit = true;
                Thread.currentThread().interrupt();
            }
        }
    }

    // JSON -> RmqMessage
    private void parseRmqMsg(String json){
        RmqParsing rmqParsing = new RmqParsing(json).invoke();
        if (!rmqParsing.isParsed()) return;

        // Parse RmqMessage
        RmqMessage rmqMessage = rmqParsing.getMsg();
        msgProcessing(rmqMessage, json);
    }

    private void msgProcessing(RmqMessage rmqMsg, String json){
        int msgIdx = scenarioManager.getIdxByMsgType(rmqMsg.getMsgType());
        String msgType = rmqMsg.getMsgType();

        // 시나리오에 존재하지 않는 메시지는 처리불가
        if (msgIdx < 0) {
            if (MsgTypeChecker.checkLogType(msgType)) {
                log.warn("INVALID MSG RECV [{}] <-- [{}]", msgType, rmqMsg.getHeader().getMsgFrom());
            } else {
                HeartbeatService heartbeatService = HeartbeatService.getInstance();
                heartbeatService.sendResMsg(msgType, json);
            }
            return;
        }

        PrintMsgLog.printLog(rmqMsg, json, false, null);

/*        if (!PrintMsgLog.printLog(rmqMsg, json, false, null)) {
            HeartbeatService heartbeatService = HeartbeatService.getInstance();
            heartbeatService.sendResMsg(msgType, json);
            return;
        }*/

        //log.debug("[{}] Recv : \n'{}'", instance.getServiceType(), JsonPretty.getFormatJson(json));

        // todo reasonCode 비교?
        // 수신 메시지 처리
        scenarioProcess.procRecv(msgIdx, json);
    }

    public static class RmqParsing {
        /**
         * RmqParser 이용해서
         * json  -> RmqMessage class 변환 후, 결과 저장
         * */
        private boolean parseResult;
        private final String json;
        private RmqMessage msg = null;

        public RmqParsing(String json){
            this.json = json;
        }

        public boolean isParsed(){
            return parseResult;
        }

        public RmqMessage getMsg(){
            return msg;
        }

        public RmqParsing invoke(){
            try {
                msg = RmqParser.parse(json);
                if (msg == null){
                    parseResult = false;
                    return this;
                }
            } catch (Exception e) {
                log.error("RmqParsing invoke Error ", e);
            }
            parseResult = true;
            return this;
        }
    }

}
