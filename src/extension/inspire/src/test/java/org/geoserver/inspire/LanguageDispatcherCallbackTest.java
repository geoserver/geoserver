package org.geoserver.inspire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
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

    @Test
    public void testErrorMessage() {
        try {
            Request request = new Request();
            Map<String, Object> kvp = new HashMap<>();
            kvp.put("LANGUAGE", "english");
            request.setRawKvp(kvp);
            request.setKvp(kvp);
            new LanguagesDispatcherCallback().init(request);
        } catch (ServiceException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "A Language parameter was provided in the request but it cannot be resolved to an ISO lang code."));
        }
    }
}
