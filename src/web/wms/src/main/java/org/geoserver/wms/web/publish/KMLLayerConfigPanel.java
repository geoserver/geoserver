/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.NumberValidator;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.publish.LayerConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.opengis.feature.type.PropertyDescriptor;

/**
 * Configures a layer KML related attributes (coming from metadata)
 */
@SuppressWarnings("serial")
public class KMLLayerConfigPanel extends LayerConfigurationPanel {
    
    /**
     * TODO: replace this with a list coming from the KML regionation classes 
     */
    static final List<String> KML_STRATEGIES = Arrays.asList("external-sorting", "geometry", "native-sorting", "random");
    
    public KMLLayerConfigPanel(String id, IModel model){
        super(id, model);

        PropertyModel metadata = new PropertyModel(model, "resource.metadata");
        add(new DropDownChoice("kml.regionateAttribute", 
                new MapModel(metadata, "kml.regionateAttribute"), 
                new AttributeNamesModel(new PropertyModel(model, "resource"))));
        add(new DropDownChoice("kml.regionateStrategy", 
                    new MapModel(metadata, "kml.regionateStrategy"), KML_STRATEGIES)
           );
        TextField maxFeatures = new TextField("kml.regionateFeatureLimit",
                    new MapModel(metadata, "kml.regionateFeatureLimit"), Integer.class);
        maxFeatures.add(NumberValidator.minimum(1));
        add(maxFeatures);
    }
    
    private static class AttributeNamesModel extends LoadableDetachableModel {
        IModel featureTypeInfo;
        
        public AttributeNamesModel(IModel featureTypeInfo) {
            this.featureTypeInfo = featureTypeInfo;
        }

        @Override
        protected Object load() {
            try {
                FeatureTypeInfo fti = (FeatureTypeInfo) featureTypeInfo.getObject();
                List<String> result = new ArrayList<String>();
                for (PropertyDescriptor property :  fti.getFeatureType().getDescriptors()) {
                    result.add(property.getName().getLocalPart());
                }
                Collections.sort(result);
                return result;
            } catch(IOException e) {
                throw new RuntimeException("Could not load feature type attribute list", e);
            }
        }
        
    }
}
