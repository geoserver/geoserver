/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.aggregate;

import org.geotools.data.aggregate.AggregateTypeConfiguration;


public class ConfigNewPage extends AbstractConfigPage {

    public ConfigNewPage(AggregateStoreEditPanel master) {
        super(master);
        initUI(new AggregateTypeConfiguration(""));
    }

    @Override
    protected boolean onSubmit() {
        master.addConfiguration(configModel.getObject());
        return true;
    }

}
