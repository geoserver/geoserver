package org.geoserver.platform.exception;

import java.util.Locale;

import junit.framework.TestCase;

public class GeoServerExceptionsTest extends TestCase {

    public void test() throws Exception {
        GeoServerException e = new TestException().id("hi");
        assertEquals("hello", GeoServerExceptions.localize(e));
    }

    public void testLocale() throws Exception {
        GeoServerException e = new TestException().id("hi");
        assertEquals("bonjour", GeoServerExceptions.localize(e, Locale.FRENCH));
    }

    public void testUnknownLocale() throws Exception {
        GeoServerException e = new TestException().id("hi");
        assertEquals("hello", GeoServerExceptions.localize(e, Locale.GERMAN));
    }

    public void testWithArgs() throws Exception {
        GeoServerException e = new TestException().id("hey").args("neo");
        assertEquals("hello neo", GeoServerExceptions.localize(e));
        assertEquals("bonjour neo", GeoServerExceptions.localize(e, Locale.FRENCH));
    }

    public void testWithNewDefault() throws Exception {
        Locale old = Locale.getDefault();
        Locale.setDefault(Locale.FRENCH);
        try {
            assertEquals("bonjour", GeoServerExceptions.localize(new TestException().id("hi")));
        }
        finally {
            Locale.setDefault(old);
        }
    }

//    class TestResourceBundleLoader implements ResourceBundleLoader {
//
//        Map<String,Map<String,Properties>> keys;
//
//        TestResourceBundleLoader() {
//            keys = new HashMap();
//
//            Properties p = new Properties();
//            p.put("TestException.hi", "hello");
//            p.put("TestException.hey", "hello {0}");
//
//            Map map = new HashMap();
//            map.put(TestException.class.getSimpleName(), p);
//            keys.put(Locale.ENGLISH.getLanguage(), map);
//            
//            p = new Properties();
//            p.put("TestException.hi", "bonjour");
//            p.put("TestException.hey", "bonjour {0}");
//
//            map = new HashMap();
//            map.put(TestException.class.getSimpleName(), p);
//            keys.put(Locale.FRENCH.getLanguage(), map);
//        }
//
//        @Override
//        public ResourceBundle load(String baseName, Locale locale,
//                ClassLoader classLoader) throws IOException {
//            
//            Map<String,Properties> map = keys.get(locale.getLanguage());
//            if (map == null) {
//                return null;
//            }
//
//            Properties p = map.get(baseName);
//            if (p == null) {
//                return null;
//            }
//
//            ByteArrayOutputStream bout = new ByteArrayOutputStream();
//            p.store(bout, null);
//
//            return new PropertyResourceBundle(new ByteArrayInputStream(bout.toByteArray()));
//        }
//    
//    }
}
