/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.sqlite.MbtilesInfo;

/** Defines the MBTiles blob store. */
public class MbtilesBlobStoreType implements BlobStoreType<MbtilesInfo> {

    @Override
    public Class<MbtilesInfo> getConfigClass() {
        return MbtilesInfo.class;
    }

    @Override
    public MbtilesInfo newConfigObject() {
        MbtilesInfo configuration = new MbtilesInfo();
        configuration.setEnabled(true);
        return configuration;
    }

    @Override
    public Panel createPanel(String id, IModel<MbtilesInfo> model) {
        return new MbtilesBlobStorePanel(id, model);
    }

    @Override
    public String toString() {
        return "MBTiles BlobStore";
    }
}
