/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.elasticsearch;

import java.util.Arrays;
import java.util.List;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.data.elasticsearch.ElasticAttribute;

/** Provide attributes from Elasticsearch fields. */
class ElasticAttributeProvider extends GeoServerDataProvider<ElasticAttribute> {

    private static final long serialVersionUID = -1021780286733349153L;

    private final List<ElasticAttribute> attributes;

    /** Name of field */
    static final Property<ElasticAttribute> NAME = new BeanProperty<>("name", "displayName");

    /** Class type of field */
    static final Property<ElasticAttribute> TYPE =
            new AbstractProperty<ElasticAttribute>("type") {

                private static final long serialVersionUID = 4454312983828267130L;

                @Override
                public Object getPropertyValue(ElasticAttribute item) {
                    if (item.getType() != null) {
                        return item.getType().getSimpleName();
                    }
                    return null;
                }
            };

    /** Mark as the default geometry */
    static final Property<ElasticAttribute> DEFAULT_GEOMETRY =
            new BeanProperty<>("defaultGeometry", "defaultGeometry");

    /** SRID of geometric field */
    static final Property<ElasticAttribute> SRID = new BeanProperty<>("srid", "srid");

    /** Use field in datastore */
    static final Property<ElasticAttribute> USE = new BeanProperty<>("use", "use");

    /** Store if the field is in use in datastore */
    static final Property<ElasticAttribute> DATE_FORMAT =
            new BeanProperty<>("dateFormat", "dateFormat");

    /** If field is analyzed */
    static final Property<ElasticAttribute> ANALYZED = new BeanProperty<>("analyzed", "analyzed");

    /** If field is stored */
    static final Property<ElasticAttribute> STORED = new BeanProperty<>("stored", "stored");

    /** Order of the field */
    static final Property<ElasticAttribute> ORDER = new BeanProperty<>("order", "order");

    /** Custom name of the field */
    static final Property<ElasticAttribute> CUSTOM_NAME =
            new BeanProperty<>("customName", "customName");

    /**
     * Build attribute provider
     *
     * @param attributes list to use as source for provider
     */
    public ElasticAttributeProvider(List<ElasticAttribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<ElasticAttribute>>
            getProperties() {
        return Arrays.asList(
                USE,
                NAME,
                TYPE,
                ORDER,
                CUSTOM_NAME,
                DEFAULT_GEOMETRY,
                STORED,
                ANALYZED,
                SRID,
                DATE_FORMAT);
    }

    @Override
    protected List<ElasticAttribute> getItems() {
        return attributes;
    }
}
