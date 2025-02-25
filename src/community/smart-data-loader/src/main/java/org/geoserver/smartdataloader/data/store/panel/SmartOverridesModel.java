package org.geoserver.smartdataloader.data.store.panel;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.smartdataloader.data.SmartDataLoaderDataAccessFactory;
import org.geoserver.smartdataloader.data.store.SmartOverrideRulesParser;

class SmartOverridesModel implements IModel<Set<SmartOverrideEntry>> {

    private final IModel<DataStoreInfo> storeModel;

    SmartOverridesModel(IModel<DataStoreInfo> storeModel) {
        this.storeModel = storeModel;
    }

    @Override
    public Set<SmartOverrideEntry> getObject() {
        return getSmartOverrides();
    }

    @Override
    public void setObject(Set<SmartOverrideEntry> object) {
        setSmartOverrides(object);
    }

    public void add(SmartOverrideEntry entry) {
        Set<SmartOverrideEntry> smartOverrides = new HashSet<>(getSmartOverrides());
        smartOverrides.add(entry);
        setSmartOverrides(smartOverrides);
    }

    private Set<SmartOverrideEntry> getSmartOverrides() {
        Map<String, Serializable> connectionParameters = storeModel.getObject().getConnectionParameters();
        Serializable serializable = connectionParameters.get(SmartDataLoaderDataAccessFactory.SMART_OVERRIDE_PARAM.key);
        String smartOverrides = null;
        if (serializable instanceof String) {
            smartOverrides = (String) serializable;
        }
        if (StringUtils.isBlank(smartOverrides)) {
            return Collections.emptySet();
        }
        return SmartOverrideRulesParser.INSTANCE.parse(smartOverrides).entrySet().stream()
                .map(e -> new SmartOverrideEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
    }

    private void setSmartOverrides(Set<SmartOverrideEntry> smartOverrides) {
        Map<String, String> smartOverridesMap = smartOverrides.stream()
                .collect(Collectors.toMap(SmartOverrideEntry::getKey, SmartOverrideEntry::getExpression));
        String encodedOverrides = SmartOverrideRulesParser.INSTANCE.encode(smartOverridesMap);
        storeModel
                .getObject()
                .getConnectionParameters()
                .put(SmartDataLoaderDataAccessFactory.SMART_OVERRIDE_PARAM.key, encodedOverrides);
    }
}
