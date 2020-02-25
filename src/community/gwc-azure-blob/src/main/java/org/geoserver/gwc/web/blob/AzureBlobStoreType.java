/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.azure.AzureBlobStoreInfo;

public class AzureBlobStoreType implements BlobStoreType<AzureBlobStoreInfo> {
    private static final long serialVersionUID = 7349157660150568235L;

    @Override
    public String toString() {
        return "Azure BlobStore";
    }

    @Override
    public AzureBlobStoreInfo newConfigObject() {
        AzureBlobStoreInfo config = new AzureBlobStoreInfo();
        config.setEnabled(true);
        config.setMaxConnections(
                Integer.valueOf(AzureBlobStoreInfo.DEFAULT_CONNECTIONS).toString());
        return config;
    }

    @Override
    public Class<AzureBlobStoreInfo> getConfigClass() {
        return AzureBlobStoreInfo.class;
    }

    @Override
    public Panel createPanel(String id, IModel<AzureBlobStoreInfo> model) {
        return new AzureBlobStorePanel(id, model);
    }
}
