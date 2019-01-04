/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.web.wicket.GeoServerDataProvider;

class AttributesProvider extends GeoServerDataProvider<AttributeDescription> {

    /** serialVersionUID */
    private static final long serialVersionUID = -1478240785822735763L;

    List<AttributeDescription> attributes = new ArrayList<AttributeDescription>();

    static final Property<AttributeDescription> NAME =
            new BeanProperty<AttributeDescription>("name", "name");

    static final Property<AttributeDescription> BINDING =
            new BeanProperty<AttributeDescription>("binding", "binding");

    static final Property<AttributeDescription> NULLABLE =
            new BeanProperty<AttributeDescription>("nullable", "nullable");

    static final Property<AttributeDescription> SIZE =
            new BeanProperty<AttributeDescription>("size", "size");

    static final Property<AttributeDescription> CRS =
            new BeanProperty<AttributeDescription>("crs", "crs");

    static final PropertyPlaceholder<AttributeDescription> UPDOWN =
            new PropertyPlaceholder<AttributeDescription>("upDown");

    public AttributesProvider() {}

    public void addNewAttribute(AttributeDescription attribute) {
        attributes.add(attribute);
    }

    @Override
    protected List<AttributeDescription> getItems() {
        return attributes;
    }

    @Override
    protected List<Property<AttributeDescription>> getProperties() {
        return Arrays.asList(NAME, BINDING, NULLABLE, SIZE, CRS, UPDOWN);
    }

    public void removeAll(List<AttributeDescription> removed) {
        this.attributes.removeAll(removed);
    }

    public boolean isFirst(AttributeDescription attribute) {
        return attributes.get(0).equals(attribute);
    }

    public boolean isLast(AttributeDescription attribute) {
        return attributes.get(attributes.size() - 1).equals(attribute);
    }

    public void moveUp(AttributeDescription attribute) {
        int idx = attributes.indexOf(attribute);
        attributes.remove(idx);
        attributes.add(idx - 1, attribute);
    }

    public void moveDown(AttributeDescription attribute) {
        int idx = attributes.indexOf(attribute);
        attributes.remove(idx);
        attributes.add(idx + 1, attribute);
    }

    public List<AttributeDescription> getAttributes() {
        return attributes;
    }
}
