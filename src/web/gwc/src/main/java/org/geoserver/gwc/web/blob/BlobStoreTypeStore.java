/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geowebcache.config.BlobStoreConfig;
import org.geowebcache.config.FileBlobStoreConfig;
import org.geowebcache.s3.S3BlobStoreConfig;

import com.google.common.base.Preconditions;

/**
 *
 * A singleton store of all supported blobstore types.
 *
 * @author Niels Charlier
 */
public class BlobStoreTypeStore {

    public static BlobStoreTypeStore newInstance() {
        return new BlobStoreTypeStore();
    }

    /**
     * 
     * Configures a blobstore type, function as factory for the config object and config panel.
     * 
     * @author Niels Charlier
     *
     * @param <T> subclass of BlobStoreConfig for this type
     */
    public static interface BlobStoreType<T extends BlobStoreConfig> extends Serializable {

        Class<T> getConfigClass();

        T newConfigObject();

        Panel createPanel(String id, IModel<T> model);

    }

    private Map<Class<? extends BlobStoreConfig>, BlobStoreType> types = new TreeMap<Class<? extends BlobStoreConfig>, BlobStoreType>(
            new Comparator<Class>() {
                @Override
                public int compare(Class o1, Class o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            });

    public BlobStoreType getFromClass(Class<? extends BlobStoreConfig> clazz) {
        return types.get(clazz);
    }

    public List<BlobStoreType> getAll() {
        return new ArrayList<BlobStoreType>(types.values());
    }

    protected void addBlobStoreType(BlobStoreType type) {
        types.put(type.getConfigClass(), type);
    }

    // --------------------------------------------------------------------------------------------------------------------------

    protected static class FileBlobStoreType implements BlobStoreType<FileBlobStoreConfig> {
        private static final long serialVersionUID = 6825505034831901062L;

        @Override
        public String toString(){
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

    protected static class S3BlobStoreType implements BlobStoreType<S3BlobStoreConfig> {
        private static final long serialVersionUID = 7349157660150568235L;

        @Override
        public String toString(){
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

    protected BlobStoreTypeStore() {
        addBlobStoreType(new FileBlobStoreType());
        addBlobStoreType(new S3BlobStoreType());
    }

    public static String getType(BlobStoreConfig blobStore) {
        Preconditions.checkNotNull(blobStore);
        if (blobStore instanceof FileBlobStoreConfig) {
            return "File  BlobStore";
        }
        if (blobStore instanceof S3BlobStoreConfig) {
            return "S3 BlobStore";
        }
        throw new IllegalArgumentException("Unknown blobstore config type: "
                + blobStore.getClass().getName());
    }

}