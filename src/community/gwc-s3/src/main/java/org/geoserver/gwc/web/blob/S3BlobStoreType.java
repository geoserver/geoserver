package org.geoserver.gwc.web.blob;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.s3.S3BlobStoreConfig;

public class S3BlobStoreType implements BlobStoreType<S3BlobStoreConfig> {
    private static final long serialVersionUID = 7349157660150568235L;

    @Override
    public String toString() {
        return "S3 BlobStore";
    }

    @Override
    public S3BlobStoreConfig newConfigObject() {
        S3BlobStoreConfig config = new S3BlobStoreConfig();
        config.setEnabled(true);
        config.setMaxConnections(50);
        return config;
    }

    @Override
    public Class<S3BlobStoreConfig> getConfigClass() {
        return S3BlobStoreConfig.class;
    }

    @Override
    public Panel createPanel(String id, IModel<S3BlobStoreConfig> model) {
        return new S3BlobStorePanel(id, model);
    }
}
