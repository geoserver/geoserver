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
