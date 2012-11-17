/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Map;

public class TestMangler implements URLMangler {

    public void mangleURL(StringBuilder baseURL, StringBuilder path, Map<String, String> kvp,
            URLType type) {
        kvp.put("here", "iam");
    }
    
}
