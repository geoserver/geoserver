/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.s3.S3BlobStoreInfo;

public class S3BlobStoreType implements BlobStoreType<S3BlobStoreInfo> {
    private static final long serialVersionUID = 7349157660150568235L;

    @Override
    public String toString() {
        return "S3 BlobStore";
    }

    @Override
    public S3BlobStoreInfo newConfigObject() {
        S3BlobStoreInfo config = new S3BlobStoreInfo();
        config.setEnabled(true);
        config.setMaxConnections(50);
        return config;
    }

    @Override
    public Class<S3BlobStoreInfo> getConfigClass() {
        return S3BlobStoreInfo.class;
    }

    @Override
    public Panel createPanel(String id, IModel<S3BlobStoreInfo> model) {
        return new S3BlobStorePanel(id, model);
    }
}
