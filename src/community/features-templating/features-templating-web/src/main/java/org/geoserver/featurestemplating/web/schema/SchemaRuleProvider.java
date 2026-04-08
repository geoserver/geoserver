/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.geoserver.featurestemplating.configuration.schema.SchemaRule;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.LiveCollectionModel;

class SchemaRuleProvider extends GeoServerDataProvider<SchemaRule> {
    static final Property<SchemaRule> PRIORITY = new BeanProperty<>("priority", "priority");
    static final Property<SchemaRule> NAME = new BeanProperty<>("name", "schemaName");
    static final Property<SchemaRule> OUTPUT_FORMAT = new BeanProperty<>("outputFormat", "outputFormat.format");

    static final Property<SchemaRule> CQL_FILTER = new BeanProperty<>("cqlFilter", "cqlFilter");

    static final Property<SchemaRule> PROFILE_FILTER = new BeanProperty<>("profileFilter", "profileFilter");

    private LiveCollectionModel<SchemaRule, Set<SchemaRule>> model;

    SchemaRuleProvider(LiveCollectionModel<SchemaRule, Set<SchemaRule>> model) {
        this.model = model;
    }

    @Override
    protected List<Property<SchemaRule>> getProperties() {
        return Arrays.asList(PRIORITY, NAME, OUTPUT_FORMAT, PROFILE_FILTER, CQL_FILTER);
    }

    @Override
    protected List<SchemaRule> getItems() {
        List<SchemaRule> entries;
        if (model != null && model.getObject() != null) {
            entries = new ArrayList<>(model.getObject());
            entries.sort(new SchemaRule.SchemaRuleComparator());
        } else {
            entries = Collections.emptyList();
        }
        return entries;
    }
}
