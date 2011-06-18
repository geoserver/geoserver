/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import junit.framework.Test;

public class GIFMapResponseTest extends RenderedImageMapOutputFormatTest {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GIFMapResponseTest());
    }

    protected RenderedImageMapOutputFormat getProducerInstance() {
        return new RenderedImageMapOutputFormat("image/gif", getWMS());
    }

}
