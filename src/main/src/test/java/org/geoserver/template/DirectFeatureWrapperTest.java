package org.geoserver.template;

public class DirectFeatureWrapperTest extends FeatureWrapperTest{
    
    @Override
    public FeatureWrapper createWrapper() {
        return new FeatureWrapper(new DirectTemplateFeatureCollectionFactory(true));
    }

}
