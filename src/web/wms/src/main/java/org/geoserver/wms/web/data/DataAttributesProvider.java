/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.util.Converters;
import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

/** A provider to show data attributes, sample values and some eventual stats */
class DataAttributesProvider extends GeoServerDataProvider<DataAttribute> {

    static final String COMPUTE_STATS = "computeStats";

    static final int MAX_SAMPLE_LENGTH = 200;

    /** */
    private static final long serialVersionUID = 3228269047960562646L;

    private final List<DataAttribute> attributes;

    public DataAttributesProvider(Feature sample) {
        this.attributes = new ArrayList<DataAttribute>();
        for (PropertyDescriptor pd : sample.getType().getDescriptors()) {
            Name name = pd.getName();
            Object value = sample.getProperty(name).getValue();
            String sampleValue = Converters.convert(value, String.class);
            if (sampleValue != null && sampleValue.length() > MAX_SAMPLE_LENGTH) {
                sampleValue = sampleValue.substring(0, MAX_SAMPLE_LENGTH - 3) + "...";
            }
            String typeName = pd.getType().getBinding().getSimpleName();
            DataAttribute ad = new DataAttribute(name.getLocalPart(), typeName, sampleValue);
            attributes.add(ad);
        }
    }

    @Override
    public List<Property<DataAttribute>> getProperties() {
        List<Property<DataAttribute>> props = new ArrayList<Property<DataAttribute>>();
        props.add(new BeanProperty<DataAttribute>("name", "name"));
        props.add(new BeanProperty<DataAttribute>("type", "type"));
        props.add(new BeanProperty<DataAttribute>("sample", "sample"));
        props.add(new BeanProperty<DataAttribute>("min", "min"));
        props.add(new BeanProperty<DataAttribute>("max", "max"));
        props.add(new PropertyPlaceholder<DataAttribute>(COMPUTE_STATS));

        return props;
    }

    @Override
    public List<DataAttribute> getItems() {
        return Collections.unmodifiableList(attributes);
    }
}
