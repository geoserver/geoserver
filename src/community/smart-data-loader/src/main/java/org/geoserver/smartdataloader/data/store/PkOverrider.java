package org.geoserver.smartdataloader.data.store;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.smartdataloader.domain.entities.DomainEntitySimpleAttribute;

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

    public void attributeOverride(String currentEntityName, DomainEntitySimpleAttribute attribute) {
        if (!overridePks.containsKey(currentEntityName)) {
            return;
        }
        if (attribute.isIdentifier()
                && !StringUtils.equalsIgnoreCase(attribute.getName(), overridePks.get(currentEntityName))) {
            attribute.setIdentifier(false);
        }
        if (!attribute.isIdentifier()
                && StringUtils.equalsIgnoreCase(attribute.getName(), overridePks.get(currentEntityName))) {
            attribute.setIdentifier(true);
        }
    }

    private boolean isPkOverrideRule(String key) {
        return !key.contains(".");
    }
}
