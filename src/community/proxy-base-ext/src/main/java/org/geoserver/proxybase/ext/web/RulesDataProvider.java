/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext.web;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.proxybase.ext.config.ProxyBaseExtRuleDAO;
import org.geoserver.proxybase.ext.config.ProxyBaseExtensionRule;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * A model for a collection of the {@link
 * org.geoserver.proxybase.ext.config.ProxyBaseExtensionRule}.
 */
public class RulesDataProvider extends GeoServerDataProvider<ProxyBaseExtensionRule> {
    public static final Property<ProxyBaseExtensionRule> EDIT_BUTTON =
            new PropertyPlaceholder<>("Edit");
    public static final Property<ProxyBaseExtensionRule> ACTIVATE_BUTTON =
            new PropertyPlaceholder<>("Active");

    private static final List<Property<ProxyBaseExtensionRule>> PROPERTIES =
            Arrays.asList(
                    new BeanProperty<>("Position", "position"),
                    new BeanProperty<>("Matcher", "matcher"),
                    new BeanProperty<>("Transform", "transformer"),
                    ACTIVATE_BUTTON,
                    EDIT_BUTTON);

    @Override
    protected List<Property<ProxyBaseExtensionRule>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected List<ProxyBaseExtensionRule> getItems() {
        return getRulesModels();
    }

    /**
     * Returns the list of rules models.
     *
     * @return the list of rules models
     */
    public static List<ProxyBaseExtensionRule> getRulesModels() {
        List<ProxyBaseExtensionRule> ruleModels =
                ProxyBaseExtRuleDAO.getRules().stream()
                        .sorted(Comparator.comparingInt(ProxyBaseExtensionRule::getPosition))
                        .collect(Collectors.toList());
        return ruleModels;
    }

    /**
     * Saves or updates the rule.
     *
     * @param ruleModel the rule model
     */
    public static void saveOrUpdate(ProxyBaseExtensionRule ruleModel) {
        ProxyBaseExtRuleDAO.saveOrUpdateProxyBaseExtRule(ruleModel);
    }

    /**
     * Deletes the rules.
     *
     * @param rulesIds the rules ids
     */
    public static void delete(String... rulesIds) {
        ProxyBaseExtRuleDAO.deleteProxyBaseExtRules(rulesIds);
    }
}
