/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.solr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;
import org.apache.commons.collections.CollectionUtils;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.data.solr.SolrAttribute;

/** Provide attributes from SOLR fields */
public class SolrAttributeProvider extends GeoServerDataProvider<SolrAttribute> {

    private static final long serialVersionUID = -1021780286733349153L;

    private List<SolrAttribute> attributes = new ArrayList<SolrAttribute>();

    private Boolean hideEmpty = true;

    /** Name of field */
    protected static final Property<SolrAttribute> NAME =
            new BeanProperty<SolrAttribute>("name", "name");

    /** Class type of field */
    protected static final Property<SolrAttribute> TYPE =
            new AbstractProperty<SolrAttribute>("type") {

                private static final long serialVersionUID = 4454312983828267130L;

                @Override
                public Object getPropertyValue(SolrAttribute item) {
                    if (item.getType() != null) {
                        return item.getType().getSimpleName();
                    }
                    return null;
                }
            };

    /** SRID of geometric field */
    protected static final Property<SolrAttribute> SRID =
            new BeanProperty<SolrAttribute>("srid", "srid");

    /** Mark as the default geometry */
    protected static final Property<SolrAttribute> DEFAULT_GEOMETRY =
            new BeanProperty<SolrAttribute>("defaultGeometry", "defaultGeometry");

    /** Store if the field is PK */
    protected static final Property<SolrAttribute> PK = new BeanProperty<SolrAttribute>("pk", "pk");

    /** Store if the field is in use in datastore */
    protected static final Property<SolrAttribute> USE =
            new BeanProperty<SolrAttribute>("use", "use");

    /** Store if the field has no data on SOLR document */
    protected static final Property<SolrAttribute> EMPTY =
            new BeanProperty<SolrAttribute>("empty", "empty");

    /**
     * Build attribute provider
     *
     * @param attributes list to use as source for provider
     */
    public SolrAttributeProvider(List<SolrAttribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<SolrAttribute>>
            getProperties() {
        return Arrays.asList(EMPTY, USE, NAME, TYPE, SRID, DEFAULT_GEOMETRY, PK);
    }

    @Override
    protected List<SolrAttribute> getItems() {
        if (hideEmpty) {
            BeanPropertyValueEqualsPredicate predicate =
                    new BeanPropertyValueEqualsPredicate("empty", Boolean.FALSE);
            // filter the Collection
            ArrayList<SolrAttribute> att =
                    new ArrayList<SolrAttribute>(CollectionUtils.select(attributes, predicate));
            return att;
        } else {
            return attributes;
        }
    }

    /**
     * Allows to reload the provider and to show/hide empty fields
     *
     * @param hideEmpty if true the provider reloads but hides empty SOLR attributes
     */
    protected void reload(Boolean hideEmpty) {
        this.hideEmpty = hideEmpty;
    }
}
