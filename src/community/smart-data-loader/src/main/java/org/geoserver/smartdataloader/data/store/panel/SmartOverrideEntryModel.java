package org.geoserver.smartdataloader.data.store.panel;

import org.apache.wicket.model.IModel;

public class SmartOverrideEntryModel implements IModel<SmartOverrideEntry> {

    private SmartOverrideEntry entry;

    public SmartOverrideEntryModel(SmartOverrideEntry entry) {
        this.entry = entry;
    }

    @Override
    public SmartOverrideEntry getObject() {
        return this.entry;
    }

    @Override
    public void setObject(SmartOverrideEntry object) {
        this.entry = object;
    }

    @Override
    public void detach() {}
}
