package org.mike;

import org.junit.Before;
import org.junit.Test;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mike.connector.utils.Utils.fromXmlNode;

/**
 * Created by levitskiym on 11.12.21
 */
public class XPathTest {

    private static final String date = "2021-01-01";
    private static final String datePattern = "2021-[0-9]{2}-[0-9]{2}";

    private byte[] xml;
    private byte[] p7s;

    @Before
    public void setUp() throws Exception {
        xml = ("<doc><header><some_data>some_data</some_data><some_date>"+date+"</some_date></header></doc>").getBytes();
        p7s = ("bchsfobfnvkv>>>some sign bytes<<<bchsfobfnvkv" + new String(xml) + "bchsfobfnvkv>>>some sign bytes<<<bchsfobfnvkv").getBytes();
    }

    @Test
    public void xpath_from_xml() throws Exception {

        final String xpath = "doc/header/some_date";
        assertNotNull(fromXmlNode(xml, xpath));
        assertEquals(date, fromXmlNode(xml, xpath));
    }

    @Test
    public void xpath_from_xml_pattern_matching() throws Exception {
        assertTrue(Pattern.matches(datePattern, fromXmlNode(xml, "doc/header/some_date")));
    }

    @Test
    public void xpath_from_p7s() throws Exception {

        final String xpath = "doc/header/some_date";
        assertNotNull(fromXmlNode(p7s, xpath));
        assertEquals(date, fromXmlNode(p7s, xpath));
    }

    @Test
    public void xpath_from_p7s_pattern_matching() throws Exception {
        assertTrue(Pattern.matches(datePattern, fromXmlNode(p7s, "doc/header/some_date")));
    }

    @Test
    public void xpath_from_xml_eq_from_p7s() throws Exception {
        final String xpath = "doc/header/some_date";
        assertEquals(fromXmlNode(xml, xpath), fromXmlNode(p7s, xpath));
    }


}
