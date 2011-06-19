/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.rest;


import org.geotools.renderer.style.FontCache;

import java.util.*;


/**
 * Class to retrieve the list of fonts available in GeoServer
 *
 * @author Jose Garca
 */
public class FontListResource extends MapResource {

    @Override
    public Map getMap() throws Exception {
        FontCache cache = FontCache.getDefaultInstance();
        
        HashMap map = new HashMap();
        map.put("fonts", cache.getAvailableFonts());

        return map;
    }
}

