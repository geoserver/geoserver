/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.collections.map.HashedMap;
import org.geotools.renderer.style.FontCache;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/fonts",
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
)
public class FontListController extends RestBaseController {

    @GetMapping
    public Map<String, Set<String>> fontsGet() {
        FontCache cache = FontCache.getDefaultInstance();

        Map<String, Set<String>> fonts = new HashedMap();

        fonts.put("fonts", new TreeSet<>(cache.getAvailableFonts()));

        return fonts;
    }
}
