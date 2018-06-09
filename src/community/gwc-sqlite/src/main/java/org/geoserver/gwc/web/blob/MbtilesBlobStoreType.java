/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.sqlite.MbtilesConfiguration;

/** Defines the MBTiles blob store. */
public class MbtilesBlobStoreType implements BlobStoreType<MbtilesConfiguration> {

    @Override
    public Class<MbtilesConfiguration> getConfigClass() {
        return MbtilesConfiguration.class;
    }

    @Override
    public MbtilesConfiguration newConfigObject() {
        MbtilesConfiguration configuration = new MbtilesConfiguration();
        configuration.setEnabled(true);
        return configuration;
    }

    @Override
    public Panel createPanel(String id, IModel<MbtilesConfiguration> model) {
        return new MbtilesBlobStorePanel(id, model);
    }

    @Override
    public String toString() {
        return "MBTiles BlobStore";
    }
}
