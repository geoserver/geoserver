package org.geoserver.inspire;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.ows.Request;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class LanguageDispatcherCallbackTest extends GeoServerSystemTestSupport {

    @Test
    public void testLanguageToAcceptLanguages() {
        Request request = new Request();
        Map<String, Object> kvp = new HashMap<>();
        kvp.put("LANGUAGE", "ita");
        request.setRawKvp(kvp);
        request.setKvp(kvp);
        new LanguagesDispatcherCallback().init(request);
        String acceptLanguagesRawKvp = request.getRawKvp().get("ACCEPTLANGUAGES").toString();
        String acceptLanguagesKvp = request.getKvp().get("ACCEPTLANGUAGES").toString();
        assertEquals("it", acceptLanguagesRawKvp);
        assertEquals("it", acceptLanguagesKvp);
    }
}
