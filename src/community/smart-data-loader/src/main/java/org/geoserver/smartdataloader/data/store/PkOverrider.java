package org.geoserver.smartdataloader.data.store;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Strings;
import org.geoserver.smartdataloader.domain.entities.DomainEntitySimpleAttribute;

/** Class responsible for overriding primary key attributes based on provided rules. */
public class PkOverrider {

    private final Map<String, String> overridePks;

    public PkOverrider(Map<String, String> overrideRules) {
        overridePks = new HashMap<>();
        for (String key : overrideRules.keySet()) {
            if (isPkOverrideRule(key)) {
                overridePks.put(key, overrideRules.get(key));
            }
        }
    }

    /**
     * Overrides the identifier status of the given attribute based on the override rules.
     *
     * @param currentEntityName the name of the current entity
     * @param attribute the attribute to potentially override
     */
    public void attributeOverride(String currentEntityName, DomainEntitySimpleAttribute attribute) {
        if (!overridePks.containsKey(currentEntityName)) {
            return;
        }
        if (attribute.isIdentifier() && !Strings.CI.equals(attribute.getName(), overridePks.get(currentEntityName))) {
            attribute.setIdentifier(false);
        }
        if (!attribute.isIdentifier() && Strings.CI.equals(attribute.getName(), overridePks.get(currentEntityName))) {
            attribute.setIdentifier(true);
        }
    }

    private boolean isPkOverrideRule(String key) {
        return !key.contains(".");
    }
}
