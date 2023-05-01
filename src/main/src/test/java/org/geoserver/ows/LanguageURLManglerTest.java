package org.geoserver.ows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class LanguageURLManglerTest extends GeoServerSystemTestSupport {

    @Test
    public void testLanguageParam() {
        final LanguageURLMangler languageURLMangler = new LanguageURLMangler();
        final Request wrappedRequest = new Request();
        wrappedRequest.setRawKvp(Collections.singletonMap(LanguageURLMangler.LANGUAGE, "it"));
        Dispatcher.REQUEST.set(wrappedRequest);

        final Map<String, String> accumulator = new HashMap<>();
        languageURLMangler.mangleURL(null, null, accumulator, URLMangler.URLType.SERVICE);

        assertEquals("it", accumulator.get(LanguageURLMangler.LANGUAGE));
    }

    @Test
    public void testAcceptLanguagesCommaParam() {
        final LanguageURLMangler languageURLMangler = new LanguageURLMangler();
        final Request wrappedRequest = new Request();
        wrappedRequest.setRawKvp(
                Collections.singletonMap(LanguageURLMangler.ACCEPT_LANGUAGES, "en, it, de"));
        Dispatcher.REQUEST.set(wrappedRequest);

        final Map<String, String> accumulator = new HashMap<>();
        languageURLMangler.mangleURL(null, null, accumulator, URLMangler.URLType.SERVICE);

        assertEquals("en", accumulator.get(LanguageURLMangler.LANGUAGE));
    }

    @Test
    public void testAcceptLanguagesSpaceParam() {
        final LanguageURLMangler languageURLMangler = new LanguageURLMangler();
        final Request wrappedRequest = new Request();
        wrappedRequest.setRawKvp(
                Collections.singletonMap(LanguageURLMangler.ACCEPT_LANGUAGES, "de fr it"));
        Dispatcher.REQUEST.set(wrappedRequest);

        final Map<String, String> accumulator = new HashMap<>();
        languageURLMangler.mangleURL(null, null, accumulator, URLMangler.URLType.SERVICE);

        assertEquals("de", accumulator.get(LanguageURLMangler.LANGUAGE));
    }

    @Test
    public void testNoLanguagesParams() {
        final LanguageURLMangler languageURLMangler = new LanguageURLMangler();
        final Request wrappedRequest = new Request();
        Dispatcher.REQUEST.set(wrappedRequest);

        final Map<String, String> accumulator = new HashMap<>();
        languageURLMangler.mangleURL(null, null, accumulator, URLMangler.URLType.SERVICE);

        assertTrue(accumulator.isEmpty());
    }

    @Test
    public void testPriorityLanguageParam() {
        final LanguageURLMangler languageURLMangler = new LanguageURLMangler();
        final Request wrappedRequest = new Request();
        Dispatcher.REQUEST.set(wrappedRequest);
        wrappedRequest.setRawKvp(
                Collections.singletonMap(LanguageURLMangler.ACCEPT_LANGUAGES, "de fr it"));
        wrappedRequest.setRawKvp(Collections.singletonMap(LanguageURLMangler.LANGUAGE, "it"));

        final Map<String, String> accumulator = new HashMap<>();
        languageURLMangler.mangleURL(null, null, accumulator, URLMangler.URLType.SERVICE);

        assertEquals("it", accumulator.get(LanguageURLMangler.LANGUAGE));
    }
}
