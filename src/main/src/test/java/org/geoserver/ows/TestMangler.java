package org.geoserver.ows;

import java.util.Map;

public class TestMangler implements URLMangler {

    public void mangleURL(StringBuilder baseURL, StringBuilder path, Map<String, String> kvp,
            URLType type) {
        kvp.put("here", "iam");
    }
    
}
