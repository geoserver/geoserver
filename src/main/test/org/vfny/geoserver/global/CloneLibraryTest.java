/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.vfny.geoserver.global.dto.CloneLibrary;
import org.vfny.geoserver.global.dto.EqualsLibrary;

import org.locationtech.jts.geom.Envelope;


/**
 * CloneLibraryTest purpose.
 * 
 * <p>
 * Description of CloneLibraryTest ...
 * </p>
 * 
 * <p></p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @version $Id$
 */
public class CloneLibraryTest extends TestCase {
    /**
     * Constructor for CloneLibraryTest.
     *
     */
    public CloneLibraryTest(String arg0) {
        super(arg0);
    }

    /*
     * Test for List clone(List)
     */
    public void testCloneList() {
        List a;
        List b;
        a = new LinkedList();
        b = null;
        a.add("a");
        a.add("b");
        a.add("c");

        try {
            b = CloneLibrary.clone(a);
        } catch (CloneNotSupportedException e) {
            fail(e.toString());
        }

        // requires EqualsLibrary tests to be completed
        assertTrue(EqualsLibrary.equals(a, b));

        a.add("d");
        assertTrue(!EqualsLibrary.equals(a, b));

        try {
            b = CloneLibrary.clone(a);
        } catch (CloneNotSupportedException e) {
            fail(e.toString());
        }

        assertTrue(EqualsLibrary.equals(a, b));

        a.remove("d");
        a.add("d");
        assertTrue(EqualsLibrary.equals(a, b));
    }

    /*
     * Test for Map clone(Map)
     */
    public void testCloneMap() {
        Map a;
        Map b;
        a = new HashMap();
        b = null;
        a.put("a", Integer.valueOf(0));
        a.put("b", Integer.valueOf(1));
        a.put("c", Integer.valueOf(2));

        try {
            b = CloneLibrary.clone(a);
        } catch (CloneNotSupportedException e) {
            fail(e.toString());
        }

        // requires EqualsLibrary tests to be completed
        assertTrue(EqualsLibrary.equals(a, b));

        a.put("d", Integer.valueOf(3));
        assertTrue(!EqualsLibrary.equals(a, b));

        try {
            b = CloneLibrary.clone(a);
        } catch (CloneNotSupportedException e) {
            fail(e.toString());
        }

        assertTrue(EqualsLibrary.equals(a, b));

        a.remove("d");
        a.put("d", Integer.valueOf(3));
        assertTrue(EqualsLibrary.equals(a, b));
    }

    /*
     * Test for Envelope clone(Envelope)
     */
    public void testCloneEnvelope() {
        Envelope a;
        Envelope b;
        a = new Envelope(1, 2, 3, 4);
        b = CloneLibrary.clone(a);
        assertTrue(a.equals(b));

        a.expandToInclude(5, 6);
        assertTrue(!a.equals(b));

        b = CloneLibrary.clone(a);
        assertTrue(a.equals(b));
    }
}
