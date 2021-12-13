package org.mike.connector.utils;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by levitskiym on 11.12.21
 */
public class Utils {

    private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    public static String fromXmlNode(byte[] xml, String xpath) {
        final int start = xpath.startsWith("/") ? 1 : 0;
        final String xpathRoot = xpath.substring(start, xpath.substring(start, xpath.length() - start).indexOf("/") + start);
        try (final InputStream is = new ByteArrayInputStream(getXmlFromP7S(xml, xpathRoot))) {
            final DocumentBuilder builder = builderFactory.newDocumentBuilder();
            final Document xmlDocument = builder.parse(is);
            final XPath xPath = XPathFactory.newInstance().newXPath();
            return (String) xPath.compile(xpath).evaluate(xmlDocument, XPathConstants.STRING);
        } catch (Exception e) {
            return "";
        }
    }

    private static byte[] getXmlFromP7S(final byte[] content, String rootTag) throws Exception {

        final String strContent = new String(content);
        final boolean isWindowsEncoding = !strContent.contains(rootTag) && strContent.contains("windows-1251");
        final String xml = isWindowsEncoding ? new String(content, "windows-1251") : strContent;
        final int start = xml.contains("<?xml") ? xml.indexOf("<?xml") : xml.indexOf("<" + rootTag + ">");
        final int end = xml.indexOf("</" + rootTag + ">") + rootTag.length() + 3;
        return isWindowsEncoding ?
                xml.substring(start, end).getBytes("windows-1251") :
                xml.substring(start, end).getBytes();
    }
}
