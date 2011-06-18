/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.svg;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.geoserver.wms.WMS;

class SVG {

    public static final String MIME_TYPE = "image/svg+xml";

    public static final Set<String> OUTPUT_FORMATS = Collections
            .unmodifiableSet(new HashSet<String>(Arrays.asList(MIME_TYPE, "image/svg xml",
                    "image/svg")));

    private SVG() {
        //
    }

    public static boolean canHandle(WMS config, String myself) {

        String svgRendererTypeSetting = config.getSvgRenderer();
        if (null == svgRendererTypeSetting) {
            svgRendererTypeSetting = WMS.SVG_SIMPLE;
        }

        return svgRendererTypeSetting.equals(myself);
    }
}
