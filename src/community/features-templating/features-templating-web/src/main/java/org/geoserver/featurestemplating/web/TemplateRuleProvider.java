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

public class TemplateRuleProvider extends GeoServerDataProvider<TemplateRule> {
    public static final Property<TemplateRule> PRIORITY =
            new BeanProperty<>("priority", "priority");
    public static final Property<TemplateRule> NAME = new BeanProperty<>("name", "templateName");
    public static final Property<TemplateRule> OUTPUT_FORMAT =
            new BeanProperty<>("outputFormat", "outputFormat.format");

    public static final Property<TemplateRule> CQL_FILTER =
            new BeanProperty<>("cqlFilter", "cqlFilter");

    private LiveCollectionModel<TemplateRule, Set<TemplateRule>> model;

    public TemplateRuleProvider(LiveCollectionModel<TemplateRule, Set<TemplateRule>> model) {
        this.model = model;
    }

    @Override
    protected List<Property<TemplateRule>> getProperties() {
        return Arrays.asList(PRIORITY, NAME, OUTPUT_FORMAT, CQL_FILTER);
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
