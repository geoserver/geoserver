/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.lang.reflect.Field;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import org.geotools.util.SoftValueHashMap;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * Unit test suite for {@link GeoServerExtensions}
 * 
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 */
public class GeoServerExtensionsTest {

    @Before
    public void setUp() throws Exception {
        System.setProperty("TEST_PROPERTY", "ABC");
    }

    @After
    public void tearDown() throws Exception {
        System.setProperty("TEST_PROPERTY", "");
    }

    @Test
    public void testSetApplicationContext() {
            ApplicationContext appContext1 = createMock(ApplicationContext.class);
            
            GeoServerExtensions gse = new GeoServerExtensions();
            
            gse.setApplicationContext(appContext1);
                      
        try {
            Field context = gse.getClass().getDeclaredField("context");
            context.setAccessible(true);

            ApplicationContext applicationContext = createMock(ApplicationContext.class);
            applicationContext = (ApplicationContext) context.get(applicationContext);

            assertSame(appContext1, applicationContext);

            Field f1 = gse.getClass().getDeclaredField("EXTENSIONS_CACHE");
            Field f2 = gse.getClass().getDeclaredField("SINGLETON_BEAN_CHACHE");

            f1.setAccessible(true);
            f2.setAccessible(true);

            SoftValueHashMap<Class, String[]> obj1 = new SoftValueHashMap<Class, String[]>(40);
            ConcurrentHashMap<String, Object> obj2 = new ConcurrentHashMap<String, Object>();

            obj1 = (SoftValueHashMap<Class, String[]>) f1.get(obj1);
            obj2 = (ConcurrentHashMap<String, Object>) f2.get(obj2);

            obj1.put(GeoServerExtensionsTest.class, new String[]{"fake"});
            obj2.put("fake", new Object());

            assertEquals("Test first side effect", 1, obj1.size());
            assertEquals("Test second side effect", 1, obj2.size());
            gse.setApplicationContext(appContext1);
            assertEquals("Test first side effect", 0, obj1.size());
            assertEquals("Test second side effect", 0, obj2.size());

        } catch (NoSuchFieldException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("NoSuchFieldException");
        } catch (SecurityException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("SecurityException");
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("IllegalArgumentException");
        } catch (IllegalAccessException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("IllegalArgumentException");
        }
    }

    @Test
    public void testExtensions() {
        try {
            ApplicationContext appContext = createMock(ApplicationContext.class);
            GeoServerExtensions gse = new GeoServerExtensions();
            gse.setApplicationContext(appContext);
            
            Field f = gse.getClass().getDeclaredField("EXTENSIONS_CACHE");
            f.setAccessible(true);
            SoftValueHashMap<Class, String[]> obj = new SoftValueHashMap<Class, String[]>(40);
            obj = (SoftValueHashMap<Class, String[]>) f.get(obj);
            
            assertEquals(0, obj.size());
            expect(appContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
            expect(appContext.getBeanNamesForType(GeoServerExtensionsTest.class)).andReturn(
                    new String[] { "testKey", "fakeKey" });
            expect(appContext.getBeanNamesForType(ExtensionProvider.class)).andReturn(new String[0]);
            expect(appContext.getBean("testKey")).andReturn(this);
            // note I'm testing null is a valid value. If that's not the case, it
            // should be reflected in the code, but I'm writing the test after the
            // code so that's what it does
            expect(appContext.isSingleton((String) anyObject())).andReturn(true).anyTimes();
            expect(appContext.getBean("fakeKey")).andReturn(null);
            replay(appContext);
            
            List<GeoServerExtensionsTest> extensions = gse.extensions(GeoServerExtensionsTest.class);
            assertNotNull(extensions);
            assertEquals(2, extensions.size());
            assertTrue(extensions.contains(this));
            assertTrue(extensions.contains(null));
            
            assertEquals(3, obj.size());
            assertTrue(obj.containsKey(GeoServerExtensionsTest.class));
            assertNotNull(obj.get(GeoServerExtensionsTest.class));
            assertEquals(2, obj.get(GeoServerExtensionsTest.class).length);
            
            verify(appContext);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("NoSuchFieldException");
        } catch (SecurityException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("SecurityException");
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("IllegalArgumentException");
        } catch (IllegalAccessException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("IllegalArgumentException");
        }
    }

    /**
     * If a CONTEXT is explicitly provided that is not the one set through
 setApplicationContext(), the extensions() method shall look into it and
 bypass the cache
     */
    @Test
    public void testExtensionsApplicationContext() {
        try {
            ApplicationContext appContext = createMock(ApplicationContext.class);
            ApplicationContext customAppContext = createMock(ApplicationContext.class);
            
            GeoServerExtensions gse = new GeoServerExtensions();
            gse.setApplicationContext(appContext);
            
            Field f = gse.getClass().getDeclaredField("EXTENSIONS_CACHE");
            f.setAccessible(true);
            SoftValueHashMap<Class, String[]> obj = new SoftValueHashMap<Class, String[]>(40);
            obj = (SoftValueHashMap<Class, String[]>) f.get(obj);
            
            // setApplicationContext cleared the static cache
            assertEquals(0, obj.size());
            // set the expectation over the app CONTEXT used as argument
            expect(customAppContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
            expect(customAppContext.getBeanNamesForType(GeoServerExtensionsTest.class)).andReturn(
                    new String[] { "itDoesntMatterForThePurpose" });
            expect(customAppContext.getBeanNamesForType(ExtensionProvider.class)).andReturn(new String[0]);
            expect(customAppContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
            expect(customAppContext.getBean("itDoesntMatterForThePurpose")).andReturn(this);
            expect(appContext.isSingleton((String) anyObject())).andReturn(true).anyTimes();
            expect(customAppContext.isSingleton((String) anyObject())).andReturn(true).anyTimes();
            replay(customAppContext);
            replay(appContext);
            
            List<GeoServerExtensionsTest> extensions = GeoServerExtensions.extensions(
                    GeoServerExtensionsTest.class, customAppContext);
            
            assertNotNull(extensions);
            assertEquals(1, extensions.size());
            assertSame(this, extensions.get(0));
            // cache should be untouched after this since our own CONTEXT were used
            assertEquals(0, obj.size());
            
            verify(appContext);
            verify(customAppContext);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("NoSuchFieldException");
        } catch (SecurityException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("SecurityException");
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("IllegalArgumentException");
        } catch (IllegalAccessException ex) {
            Logger.getLogger(GeoServerExtensionsTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("IllegalArgumentException");
        }
    }
    
    @Test
    public void testExtensionFilterByName() {
        ApplicationContext appContext = createNiceMock(ApplicationContext.class);

        // setApplicationContext cleared the static cache
        // set the expectation over the app CONTEXT used as argument
        NameExclusionFilter filter = new NameExclusionFilter();
        filter.setBeanId("testId");
        expect(appContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[] { "filter" }).anyTimes();
        expect(appContext.getBean("filter")).andReturn(filter).anyTimes();
        expect(appContext.getBeanNamesForType(GeoServerExtensionsTest.class)).andReturn(new String[] { "testId" }).anyTimes();
        expect(appContext.getBean("testId")).andReturn(this).anyTimes();
        replay(appContext);

        // build extensions
        GeoServerExtensions gse = new GeoServerExtensions();
        gse.setApplicationContext(appContext);
        
        // check we get nothing
        List<GeoServerExtensionsTest> extensions = gse.extensions(GeoServerExtensionsTest.class);
        assertEquals(0, extensions.size());
        
        // change the bean id and we should get one result instead
        filter.setBeanId("holabaloo");
        extensions = gse.extensions(GeoServerExtensionsTest.class);
        assertEquals(1, extensions.size());
        assertSame(this, extensions.get(0));
    }
    
    @Test
    public void testExtensionFilterByClass() {
        ApplicationContext appContext = createNiceMock(ApplicationContext.class);

        // setApplicationContext cleared the static cache
        // set the expectation over the app CONTEXT used as argument
        ClassExclusionFilter filter = new ClassExclusionFilter();
        filter.setBeanClass(GeoServerExtensionsTest.class);
        expect(appContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[] { "filter" }).anyTimes();
        expect(appContext.getBean("filter")).andReturn(filter).anyTimes();
        expect(appContext.getBeanNamesForType(GeoServerExtensionsTest.class)).andReturn(new String[] { "testId" }).anyTimes();
        expect(appContext.getBean("testId")).andReturn(this).anyTimes();
        replay(appContext);

        // build extensions
        GeoServerExtensions gse = new GeoServerExtensions();
        gse.setApplicationContext(appContext);
        
        // check we get nothing
        List<GeoServerExtensionsTest> extensions = gse.extensions(GeoServerExtensionsTest.class);
        assertEquals(0, extensions.size());
        
        // change the bean id and we should get one result instead
        filter.setBeanClass(Integer.class);
        extensions = gse.extensions(GeoServerExtensionsTest.class);
        assertEquals(1, extensions.size());
        assertSame(this, extensions.get(0));
    }

    @Test
    public void testBeanString() {
        ApplicationContext appContext = createMock(ApplicationContext.class);

        GeoServerExtensions gse = new GeoServerExtensions();

        gse.setApplicationContext(null);
        assertNull(GeoServerExtensions.bean("beanName"));

        gse.setApplicationContext(appContext);

        expect(appContext.isSingleton((String) anyObject())).andReturn(true).anyTimes();
        expect(appContext.getBean("beanName")).andReturn(null); // call #1
        expect(appContext.getBean("beanName")).andReturn(this); // call #2
        replay(appContext);

        assertNull(GeoServerExtensions.bean("beanName")); // call #1
        assertSame(this, GeoServerExtensions.bean("beanName")); // call #2

        verify(appContext);
    }

    @Test
    public void testExtensionProvider() {
        ApplicationContext appContext = createMock(ApplicationContext.class);
        GeoServerExtensions gse = new GeoServerExtensions();
        gse.setApplicationContext(appContext);
        
        expect(appContext.getBeanNamesForType(ExtensionFilter.class)).andReturn(new String[0]);
        expect(appContext.getBeanNamesForType(GeoServerExtensionsTest.class)).andReturn(new String[0]);
        expect(appContext.getBeanNamesForType(ExtensionProvider.class))
            .andReturn(new String[]{"testKey2"});
        
        ExtensionProvider xp = createMock(ExtensionProvider.class);
        expect(xp.getExtensionPoint()).andReturn(GeoServerExtensionsTest.class);
        expect(xp.getExtensions(GeoServerExtensionsTest.class)).andReturn(Arrays.asList(this));
        expect(appContext.getBean("testKey2")).andReturn(xp);
        expect(appContext.isSingleton((String) anyObject())).andReturn(true).anyTimes();
        
        replay(xp);
        replay(appContext);
        assertEquals(1, GeoServerExtensions.extensions(GeoServerExtensionsTest.class).size());
        
        verify(xp);
        verify(appContext);
    }
    
    public void _testBeanClassOfT() {
        fail("Not yet implemented");
    }

    public void _testBeanClassOfTApplicationContext() {
        fail("Not yet implemented");
    }

    public void _testOnApplicationEvent() {
        fail("Not yet implemented");
    }

    public void _testCheckContext() {
        fail("Not yet implemented");
    }
    
    @Test
    public void testSystemProperty() {
        // check for a property we did set up in the setUp
        assertEquals("ABC", GeoServerExtensions.getProperty("TEST_PROPERTY", (ApplicationContext) null));
        assertEquals("ABC", GeoServerExtensions.getProperty("TEST_PROPERTY", (ServletContext) null));
    }
    
    @Test
    public void testWebProperty() {
        ServletContext servletContext = createMock(ServletContext.class);
        expect(servletContext.getInitParameter("TEST_PROPERTY")).andReturn("DEF").anyTimes();
        expect(servletContext.getInitParameter("WEB_PROPERTY")).andReturn("WWW").anyTimes();
        replay(servletContext);
        
        assertEquals("ABC", GeoServerExtensions.getProperty("TEST_PROPERTY", servletContext));
        assertEquals("WWW", GeoServerExtensions.getProperty("WEB_PROPERTY", servletContext));
    }

}
