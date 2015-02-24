/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
