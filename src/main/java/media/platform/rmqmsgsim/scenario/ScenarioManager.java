package media.platform.rmqmsgsim.scenario;

import media.platform.rmqmsgsim.AppInstance;
import media.platform.rmqmsgsim.config.Config;
import media.platform.rmqmsgsim.json.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dajin kim
 */
public class ScenarioManager {
    static final Logger log = LoggerFactory.getLogger(ScenarioManager.class);

    private static ScenarioManager scenarioManager = null;
    private JSONArray scenario = null;
    private final List<String> msgTypeList = new ArrayList<>();
    private final List<DirType> dirList = new ArrayList<>();
    private final AtomicInteger callNum = new AtomicInteger();
    private final AtomicInteger endCallNum = new AtomicInteger();
    private int configCallNum;

    private ScenarioManager() {
        // Do Nothing
    }

    public static ScenarioManager getInstance() {
        if (scenarioManager == null)
            scenarioManager = new ScenarioManager();
        return scenarioManager;
    }

    public void reset() {
        scenario = null;
        msgTypeList.clear();
        dirList.clear();
        callNum.set(0);
        endCallNum.set(0);
        log.debug("ScenarioManager Reset callNum:{}, endCall:{}", callNum.get(), endCallNum.get());
    }

    public boolean init(String scenarioFile) {
        AppInstance.getInstance().setLastMsgTime();
        Config config = AppInstance.getInstance().getConfig();
        String scenarioPath = config.getScenarioPath() + File.separator + scenarioFile + JsonEnum.FILE_EXTENSION.getValue();
        configCallNum = config.getCallNum();
        scenario = ScenarioParser.file2JsonArr(scenarioPath);

        if (scenario == null) {
            endCallNum.set(configCallNum);
            return false;
        }

        JsonParse jsonParse = JsonParse.getInstance();
        for (Object object : scenario) {
            msgTypeList.add(jsonParse.headerParse((JSONObject) object, JsonEnum.TYPE.getValue()));
            String msgDirection = jsonParse.sendDirParse((JSONObject) object);
            dirList.add(DirType.getTypeEnum(msgDirection));
        }


        log.warn("\n << INIT SCENARIO, TEST {} CALLS >>\n => SCENARIO: [{}]\n => MSG     : {}\n => DIR     : {}",
                configCallNum, scenarioPath, msgTypeList, dirList);

        return true;
    }

    // scenario
    public int getScenarioSize() {
        return scenario == null? 0 : this.scenario.size();
    }

    public JSONObject getScenario(int index) {
        return scenario == null? null : (JSONObject) scenario.get(index);
    }

    public JSONArray getScenario() {
        return this.scenario;
    }

    // msgTypeList
    public String getType(int idx) {
        return msgTypeList.isEmpty()? null : msgTypeList.get(idx);
    }

    public List<String> getMsgTypeList() {
        return msgTypeList;
    }

    public boolean hasMsgType(String type) {
        return msgTypeList.contains(type);
    }

    public int getIdxByMsgType(String type) {
        return msgTypeList.indexOf(type);
    }

    // dirList
    public DirType getDirType(int idx) {
        return dirList.isEmpty()? null : dirList.get(idx);
    }

    public boolean isSendType(int idx) {
        return DirType.SEND.equals(getDirType(idx));
    }

    public boolean isPauseType(int idx) {
        return DirType.PAUSE.equals(getDirType(idx));
    }

    public boolean isRecvType(int idx) {
        return DirType.RECV.equals(getDirType(idx));
    }

    // CallNum
    public boolean isTestValid() {
        return callNum.get() < configCallNum;
    }

    public int getCallNum() {
        return callNum.get();
    }

    public void increaseCallNum() {
        callNum.incrementAndGet();
    }

    public int getEndCallNum() {
        return endCallNum.get();
    }

    public void increaseEndCallNum() {
        endCallNum.incrementAndGet();
    }

    // get Scenario Parsed Info
    public int getPauseTime(int idx) {
        if (isPauseType(idx)) {
            String pauseType = getType(idx);
/*
            if (pauseType != null) return Integer.parseInt(pauseType.substring(5));
*/
            if (pauseType != null) {
                pauseType = pauseType.replaceAll("[^0-9]", "");
                return Integer.parseInt(pauseType);
            }
        }
        return 0;
    }

    public String getDirection(int idx) {
        JsonParse jsonParse = JsonParse.getInstance();
        JSONObject jsonObject = getScenario(idx);
        return jsonObject == null? null : jsonParse.sendDirParse(getScenario(idx));
    }

    public String getSendModule(int idx) {
        JsonParse jsonParse = JsonParse.getInstance();
        JSONObject jsonObject = getScenario(idx);
        return jsonObject == null? null : jsonParse.sendModuleParse(getScenario(idx));
    }

    public static void main(String[] args) {
        parseStr("pause 5");
        parseStr("Pause5");
        parseStr("PAUSE5");
        parseStr("pAuSe5");
        parseStr("pAuSe5.5");
    }

    public static int parseStr(String pause) {
        try {
            String replacePause = pause.replaceAll("[^0-9]", "");
            int time = Integer.parseInt(replacePause);
            System.out.println(pause + " -> " + replacePause + " ->" + time);
            return time;
        } catch (NumberFormatException e) {
            System.out.println("parseStr.Eception");
        }

        return 0;
    }
}
