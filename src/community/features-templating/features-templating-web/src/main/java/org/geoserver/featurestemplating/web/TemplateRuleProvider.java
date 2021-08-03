/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.LiveCollectionModel;

class TemplateRuleProvider extends GeoServerDataProvider<TemplateRule> {
    static final Property<TemplateRule> PRIORITY = new BeanProperty<>("priority", "priority");
    static final Property<TemplateRule> NAME = new BeanProperty<>("name", "templateName");
    static final Property<TemplateRule> OUTPUT_FORMAT =
            new BeanProperty<>("outputFormat", "outputFormat.format");

    static final Property<TemplateRule> CQL_FILTER = new BeanProperty<>("cqlFilter", "cqlFilter");

    static final Property<TemplateRule> PROFILE_FILTER =
            new BeanProperty<>("profileFilter", "profileFilter");

    private LiveCollectionModel<TemplateRule, Set<TemplateRule>> model;

    TemplateRuleProvider(LiveCollectionModel<TemplateRule, Set<TemplateRule>> model) {
        this.model = model;
    }

    @Override
    protected List<Property<TemplateRule>> getProperties() {
        return Arrays.asList(PRIORITY, NAME, OUTPUT_FORMAT, PROFILE_FILTER, CQL_FILTER);
    }

    @Override
    protected List<TemplateRule> getItems() {
        List<TemplateRule> entries;
        if (model != null && model.getObject() != null) {
            entries = new ArrayList<>(model.getObject());
            entries.sort(new TemplateRule.TemplateRuleComparator());
        } else {
            entries = Collections.emptyList();
        }
        return entries;
    }
}
