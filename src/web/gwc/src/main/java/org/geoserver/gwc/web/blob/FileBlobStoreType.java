/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.config.FileBlobStoreInfo;

public class FileBlobStoreType implements BlobStoreType<FileBlobStoreInfo> {
    private static final long serialVersionUID = 6825505034831901062L;

    @Override
    public String toString() {
        return "File BlobStore";
    }

    @Override
    public FileBlobStoreInfo newConfigObject() {
        FileBlobStoreInfo config = new FileBlobStoreInfo();
        config.setEnabled(true);
        config.setFileSystemBlockSize(4096);
        return config;
    }

    @Override
    public Class<FileBlobStoreInfo> getConfigClass() {
        return FileBlobStoreInfo.class;
    }

    @Override
    public Panel createPanel(String id, IModel<FileBlobStoreInfo> model) {
        return new FileBlobStorePanel(id, model);
    }
}
