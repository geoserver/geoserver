/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web.schema;

import java.util.Arrays;
import java.util.List;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfo;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfoDAO;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class SchemaInfoProvider extends GeoServerDataProvider<SchemaInfo> {

    public static final Property<SchemaInfo> NAME = new BeanProperty<>("schemaName", "schemaName");
    public static final Property<SchemaInfo> EXTENSION = new BeanProperty<>("extension", "extension");
    public static final Property<SchemaInfo> WORKSPACE = new BeanProperty<>("workspace", "workspace");
    public static final Property<SchemaInfo> FEATURE_TYPE_INFO = new BeanProperty<>("featureTypeInfo", "featureType");

    @Override
    protected List<Property<SchemaInfo>> getProperties() {
        return Arrays.asList(NAME, EXTENSION, WORKSPACE, FEATURE_TYPE_INFO);
    }

    @Override
    protected List<SchemaInfo> getItems() {
        return SchemaInfoDAO.get().findAll();
    }
}
