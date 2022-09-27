/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2022, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.data.gen;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.gen.info.GeneralizationInfos;
import org.geotools.util.URLs;
import org.junit.Test;

public class PreGeneralizedDataStoreFactoryTest extends GeoServerSystemTestSupport {

    @Test
    public void testResourceStoreAccess() throws IOException {
        final AtomicBoolean configurationFileRequested = new AtomicBoolean(false);

        URL url = PreGeneralizedDataStoreFactoryTest.class.getResource("/");
        GeoServerResourceLoader resourceLoader =
                new GeoServerResourceLoader(URLs.urlToFile(url)) {
                    @Override
                    public Resource get(String path) {
                        if ("geninfo1.xml".equals(path)) {
                            configurationFileRequested.set(true);
                        }
                        return super.get(path);
                    }
                };
        GeoServerExtensionsHelper.clear();
        GeoServerExtensionsHelper.singleton(
                "resourceLoader", resourceLoader, GeoServerResourceLoader.class);

        org.geoserver.data.gen.info.GeneralizationInfosProviderImpl infoProvider =
                new org.geoserver.data.gen.info.GeneralizationInfosProviderImpl();
        assertNotNull(infoProvider);
        GeneralizationInfos genInfo = infoProvider.getGeneralizationInfos("geninfo1.xml");
        assertNotNull(genInfo);

        assertTrue(configurationFileRequested.get());
    }
}
