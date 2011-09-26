package org.geoserver.web.data.store.aggregate;

import org.geotools.data.aggregate.AggregateTypeConfiguration;


public class ConfigEditPage extends AbstractConfigPage {

    public ConfigEditPage(AggregateStoreEditPanel master, AggregateTypeConfiguration config) {
        super(master);
        initUI(config);
    }

    @Override
    protected boolean onSubmit() {
        return true;
    }

}
