package org.geoserver.gwc.web.blob;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.config.FileBlobStoreConfig;

public class FileBlobStoreType implements BlobStoreType<FileBlobStoreConfig> {
    private static final long serialVersionUID = 6825505034831901062L;

    @Override
    public String toString() {
        return "File BlobStore";
    }

    @Override
    public FileBlobStoreConfig newConfigObject() {
        FileBlobStoreConfig config = new FileBlobStoreConfig();
        config.setEnabled(true);
        config.setFileSystemBlockSize(4096);
        return config;
    }

    @Override
    public Class<FileBlobStoreConfig> getConfigClass() {
        return FileBlobStoreConfig.class;
    }

    @Override
    public Panel createPanel(String id, IModel<FileBlobStoreConfig> model) {
        return new FileBlobStorePanel(id, model);
    }
}
