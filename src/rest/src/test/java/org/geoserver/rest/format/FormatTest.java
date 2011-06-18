/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.rest.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.geoserver.test.GeoServerTestSupport;

/**
 * A set of generic tests for output formats to just check that they can read their own output. 
 * Format classes should probably still have their own custom tests to verify the format itself is correct.
 */
public class FormatTest extends GeoServerTestSupport {
    static List formats;
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new FormatTest());
    }

    public void oneTimeSetUp() throws Exception{
    	super.oneTimeSetUp();
    	// Add to this list if you would like to test another general format
        formats = new ArrayList();
        formats.add(new MapXMLFormat());
        formats.add(new MapJSONFormat());
    }

    public void testFormatMap(){
        Iterator it = formats.iterator();
        while (it.hasNext()){
            try{
                DataFormat format = (DataFormat)it.next();
                Map input = new HashMap();
                input.put("Hello", "Goodbye");

                Map result = (Map)format.toObject(format.toRepresentation(input));
                assertEquals(result.size(), input.size());
                Iterator mapIt = input.entrySet().iterator();
                while (mapIt.hasNext()){
                    Map.Entry ent = (Map.Entry)mapIt.next();
                    assertEquals(result.get(ent.getKey()), ent.getValue());
                }
            } catch (Exception e){
                // should log this or something? does JUnit let you log a failure without quitting the test?
            }
        }
    }

    public void testFormatList(){
        Iterator it = formats.iterator();
        while (it.hasNext()){
            try{
                DataFormat format = (DataFormat)it.next();
                List input = new ArrayList();
                input.add("Hello");

                List result = (List)format.toObject(format.toRepresentation(input));
                assertEquals(result.size(), input.size());
                for (int i = 0; i < result.size(); i++){
                    assertEquals(input.get(i), result.get(i)); }
            } catch (Exception e){
                // should log this or something? does JUnit let you log a failure without quitting the test?
            }
        }
    }

    // TODO: Should we worry about serializing single strings?  Arbitrary objects?
    public void dontTestFormatScalar(){
        Iterator it = formats.iterator();

        while (it.hasNext()){
            DataFormat format = (DataFormat)it.next();
            String input = "Hello";

            String result = (String) format.toObject(format.toRepresentation(input));
            assertEquals(input, result);
        }
    }
}
