/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.Version;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

        
/**
 * Test for the format options / SQL View parameters in a request.
 * 
 * @author Robert Coup
 */
public class FormatOptionsKvpParserTest extends TestCase {
    
    private FormatOptionsKvpParser parser;
    
    Map<String, String> kvp = new HashMap<String,String>();

    @Override
    protected void setUp() throws Exception {
        parser = new FormatOptionsKvpParser();
        new GeoServerExtensions().setApplicationContext(null);
        Dispatcher.REQUEST.remove();
        
        kvp.put("SERVICE", "TestService");
        kvp.put("VERSION", "TestVersion");
        kvp.put("REQUEST", "TestRequest");
    }
    
    /**
     * Tests normal-style format options
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testPairs() throws Exception {
        Map<String,String> expected = new HashMap<String, String>()
        {
            {
                put("key1", "value1");
                put("key2", "value2");
                put("key3", "true");
                put("key4", "value4");
            }
        };        
        
        Map<String,String> actual = (Map<String,String>)parser.parse("key1:value1;key2:value2;key3;key4:value4");
        assertEquals(expected, actual);
    }

    /**
     * Tests format options with escaped separators
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testEscapedSeparators() throws Exception {
        Map<String,String> expected = new HashMap<String, String>()
        {
            {
                put("key1", "value:1");
                put("key2", "value:2");
                put("key3", "value:3;ZZZ");
            }
        };        
        
        Map<String,String> actual = (Map<String,String>)parser.parse("key1:value\\:1;key2:value\\:2;key3:value\\:3\\;ZZZ");
        assertEquals(expected, actual);
    }

    /**
     * Tests format options with embedded separators
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testEmbeddedSeparators() throws Exception {
        Map<String,String> expected = new HashMap<String, String>()
        {
            {
                put("key1", "value:1");
                put("key2", "value:2");
                put("key3", "value:3:ZZ;XX");
            }
        };        
        
        Map<String,String> actual = (Map<String,String>)parser.parse("key1:value:1;key2:value:2;key3:value:3\\:ZZ\\;XX");
        assertEquals(expected, actual);
    }

    /**
     * Tests format options with embedded separators
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testErrors() throws Exception {
        Map<String,String> expected = new HashMap<String, String>()
        {
            {
                put("key1", "value:1");
                put("key2", "value:2");
                put("key3", "value:3");
            }
        };        
        
        Map<String,String> actual = (Map<String,String>)parser.parse("key1:value:1;key2:value:2;key3:value:3");
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }
    
    /**
     * Tests that values are parsed using the set of KvpParser configured for 
     * the current service/version/request.
     */
    public void testParseUsingServiceParsers() throws Exception {
        // configure a parser that parses "test" key as a List
        // (look at applicationContext-test.xml)
        parser.setService("TestService");
        parser.setRequest("TestRequest");
        parser.setVersion(new Version("TestVersion"));
        
        // first test without any configured parser
        Map actual = (Map)parser.parse("test:dummy");
        assertTrue(actual.containsKey("test"));
        assertNotNull(actual.get("test"));
        assertTrue(actual.get("test") instanceof String);
        
        
        // load test parser from application context
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext-test.xml");
        context.refresh();
        new GeoServerExtensions().setApplicationContext(context);
        
        actual = (Map)parser.parse("test:dummy");
        assertTrue(actual.containsKey("test"));
        assertNotNull(actual.get("test"));
        assertTrue(actual.get("test") instanceof List);
    }
    
    /**
     * Tests that values are parsed using the set of KvpParser configured for the current service/version/request.
     */
    public void testParseUsingWrongServiceParsers() throws Exception {
        // we configure the parser with an unknown service,
        // so that default parsing is done
        parser.setService("WrongService");
        parser.setRequest("TestRequest");
        parser.setVersion(new Version("TestVersion"));
        
        
        // load test parser from application context
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext-test.xml");
        context.refresh();
        new GeoServerExtensions().setApplicationContext(context);
        
        Map actual = (Map)parser.parse("test:dummy");
        assertTrue(actual.containsKey("test"));
        assertNotNull(actual.get("test"));
        // still a String, no parser is matching
        assertTrue(actual.get("test") instanceof String);
    }

}
