/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.blob.gcs.web;

import java.io.Serial;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.gwc.web.blob.BlobStoreType;
import org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageBlobStoreInfo;

public class GcsBlobStoreType implements BlobStoreType<GoogleCloudStorageBlobStoreInfo> {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "Google Cloud Storage BlobStore";
    }

    @Override
    public GoogleCloudStorageBlobStoreInfo newConfigObject() {
        GoogleCloudStorageBlobStoreInfo config = new GoogleCloudStorageBlobStoreInfo();
        config.setEnabled(true);
        return config;
    }

    @Override
    public Class<GoogleCloudStorageBlobStoreInfo> getConfigClass() {
        return GoogleCloudStorageBlobStoreInfo.class;
    }

    @Override
    public Panel createPanel(String id, IModel<GoogleCloudStorageBlobStoreInfo> model) {
        return new GcsBlobStorePanel(id, model);
    }
}
