/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;


public class GIFMapResponseTest extends RenderedImageMapOutputFormatTest {

 

    protected RenderedImageMapOutputFormat getProducerInstance() {
        return new RenderedImageMapOutputFormat("image/gif", getWMS());
    }

}
