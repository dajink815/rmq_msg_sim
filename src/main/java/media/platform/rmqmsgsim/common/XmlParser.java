package media.platform.rmqmsgsim.common;

import media.platform.rmqmsgsim.AppInstance;
import media.platform.rmqmsgsim.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dajin kim
 */
public class XmlParser {
    private static final Logger log = LoggerFactory.getLogger(XmlParser.class);
    private final AppInstance instance = AppInstance.getInstance();
    private final Config config = instance.getConfig();
    private static final String HEADER = "/msg/header/name";
    private static final String BODY = "/msg/body/name";
    private static final String TYPE_HEADER = "INVITE";
    private static final String TYPE_BODY = "BYE";

    public XmlParser() {
        // Do Nothing
    }

    public void xmlParsing() {
        List<String> headerList = new ArrayList<>();
        headerList.add("transactionId");
        headerList.add("msgFrom");
        headerList.add("timestamp");
        instance.setHeaderList(headerList);

        //readXmlFile(TYPE_HEADER);
        readXmlFile(TYPE_BODY);
    }

    private void readXmlFile(String type) {
        String xpathType;
        List<String> valueList = new ArrayList<>();

        if (TYPE_HEADER.equalsIgnoreCase(type)) {
            xpathType = HEADER;
        } else {
            xpathType = BODY;
        }

        try {
            InputSource is = new InputSource(new FileReader(config.getXmlPath()));
            //InputSource is = new InputSource(new FileReader("/Users/kimdajin/Simulator/rmqmsgsim/src/main/resources/xml/jsonField.xml"));
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();

            NodeList nodeList = (NodeList) xpath.compile(xpathType).evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node nNode = nodeList.item(i);
                valueList.add(nNode.getTextContent());
            }

            if (TYPE_HEADER.equalsIgnoreCase(type)) {
                instance.setHeaderList(valueList);
            } else {
                instance.setBodyList(valueList);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readXmlFile() {

        List<String> headerList = new ArrayList<>();
        List<String> bodyList = new ArrayList<>();

        try {
            InputSource is = new InputSource(new FileReader("/Users/kimdajin/Simulator/rmqmsgsim/src/main/resources/xml/jsonField.xml"));
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();

            NodeList headerNodes = (NodeList) xpath.compile("/msg/header/name").evaluate(document, XPathConstants.NODESET);

            System.out.println(headerNodes.getLength());
            System.out.println();

            for (int i = 0; i < headerNodes.getLength(); i++) {
                Node nNode = headerNodes.item(i);
                headerList.add(nNode.getTextContent());

/*                System.out.println(i);
                System.out.println("getNodeValue : " + nNode.getNodeValue());
                System.out.println("getNodeName : " + nNode.getNodeName());
                System.out.println("getNodeType : " + nNode.getNodeType());
                System.out.println("getTextContent : " + nNode.getTextContent());
                System.out.println("getNodeValue : " + nNode.getFirstChild().getNodeValue());
                System.out.println();*/

            }

            System.out.println(headerList);


            NodeList bodyNodes = (NodeList) xpath.compile("/msg/body/name").evaluate(document, XPathConstants.NODESET);
            System.out.println(bodyNodes.getLength());
            System.out.println();

            for (int i = 0; i < bodyNodes.getLength(); i++) {
                Node nNode = headerNodes.item(i);
                bodyList.add(nNode.getTextContent());

/*                System.out.println(i);
                System.out.println("getNodeValue : " + nNode.getNodeValue());
                System.out.println("getNodeName : " + nNode.getNodeName());
                System.out.println("getNodeType : " + nNode.getNodeType());
                System.out.println("getTextContent : " + nNode.getTextContent());
                System.out.println("getNodeValue : " + nNode.getFirstChild().getNodeValue());
                System.out.println();*/

            }

            System.out.println(bodyList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
