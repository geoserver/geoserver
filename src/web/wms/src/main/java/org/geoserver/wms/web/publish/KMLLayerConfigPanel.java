/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
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
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.opengis.feature.type.PropertyDescriptor;

/** Configures a layer KML related attributes (coming from metadata) */
public class KMLLayerConfigPanel extends PublishedConfigurationPanel<LayerInfo> {

    private static final long serialVersionUID = 6469105227923320272L;
    /** TODO: replace this with a list coming from the KML regionation classes */
    static final List<String> KML_STRATEGIES =
            Arrays.asList("external-sorting", "geometry", "native-sorting", "random");

    public KMLLayerConfigPanel(String id, IModel<LayerInfo> model) {
        super(id, model);

        PropertyModel<MetadataMap> metadata =
                new PropertyModel<MetadataMap>(model, "resource.metadata");
        add(
                new DropDownChoice<String>(
                        "kml.regionateAttribute",
                        new MapModel(metadata, "kml.regionateAttribute"),
                        new AttributeNamesModel(new PropertyModel(model, "resource"))));
        add(
                new DropDownChoice<String>(
                        "kml.regionateStrategy",
                        new MapModel(metadata, "kml.regionateStrategy"),
                        KML_STRATEGIES));
        TextField<Integer> maxFeatures =
                new TextField<Integer>(
                        "kml.regionateFeatureLimit",
                        new MapModel(metadata, "kml.regionateFeatureLimit"),
                        Integer.class);
        maxFeatures.add(RangeValidator.minimum(1));
        add(maxFeatures);
    }

    private static class AttributeNamesModel extends LoadableDetachableModel<List<String>> {
        private static final long serialVersionUID = 2480902398710400909L;

        IModel<FeatureTypeInfo> featureTypeInfo;

        public AttributeNamesModel(IModel<FeatureTypeInfo> featureTypeInfo) {
            this.featureTypeInfo = featureTypeInfo;
        }

        @Override
        protected List<String> load() {
            try {
                FeatureTypeInfo fti = featureTypeInfo.getObject();
                List<String> result = new ArrayList<String>();
                for (PropertyDescriptor property : fti.getFeatureType().getDescriptors()) {
                    result.add(property.getName().getLocalPart());
                }
                Collections.sort(result);
                return result;
            } catch (IOException e) {
                throw new RuntimeException("Could not load feature type attribute list", e);
            }
        }
    }
}
