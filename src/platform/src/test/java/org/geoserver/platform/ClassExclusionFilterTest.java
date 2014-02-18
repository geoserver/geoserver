/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.geoserver.platform;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author surzhin.konstantin
 */
public class ClassExclusionFilterTest {
    
    public ClassExclusionFilterTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getBeanClass method, of class ClassExclusionFilter.
     */
    @Test
    public void testGetBeanClass() {
        System.out.println("getBeanClass");
        final ClassExclusionFilter instance = new ClassExclusionFilter();
        final Class result = instance.getBeanClass();
        assertEquals("beanClass is not null", null, result);

    }

    /**
     * Test of setBeanClass method, of class ClassExclusionFilter.
     */
    @Test
    public void testSetBeanClass() {
        System.out.println("setBeanClass");
        final ClassExclusionFilter instance = new ClassExclusionFilter();
        instance.setBeanClass(GeoServerExtensionsTest.class);
        final Class result = instance.getBeanClass();
        assertEquals("beanClass is not equals GeoServerExtensionsTest.class", GeoServerExtensionsTest.class, result);
    }

    /**
     * Test of isMatchSubclasses method, of class ClassExclusionFilter.
     */
    @Test
    public void testIsMatchSubclasses() {
        System.out.println("isMatchSubclasses");
        final ClassExclusionFilter instance = new ClassExclusionFilter();
        final boolean result = instance.isMatchSubclasses();
        assertEquals("matchSubclasses is not false", false, result);
    }

    /**
     * Test of setMatchSubclasses method, of class ClassExclusionFilter.
     */
    @Test
    public void testSetMatchSubclasses() {
        System.out.println("setMatchSubclasses");
        final ClassExclusionFilter instance = new ClassExclusionFilter();
        instance.setMatchSubclasses(true);
        final boolean result = instance.isMatchSubclasses();
        assertEquals("matchSubclasses is not true", true, result);
    }

    /**
     * Test of exclude method, of class ClassExclusionFilter.
     */
    @Test
    public void testExclude() {
        System.out.println("exclude");
        final ClassExclusionFilter instance = new ClassExclusionFilter();
        final boolean result = instance.exclude("", null);
        assertEquals("should return false", false, result);
        //TODO: Not all cases are covered.
        fail("FIXME: The test case is a prototype.");
    }
}
