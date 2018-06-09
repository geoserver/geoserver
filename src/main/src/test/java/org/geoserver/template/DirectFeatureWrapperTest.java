/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.template;

import org.junit.After;

public class DirectFeatureWrapperTest extends FeatureWrapperTest {

    private DirectTemplateFeatureCollectionFactory fac =
            new DirectTemplateFeatureCollectionFactory();

    @Override
    public FeatureWrapper createWrapper() {
        return new FeatureWrapper(fac);
    }

    @After
    public void cleanUp() {
        fac.purge();
    }
}
