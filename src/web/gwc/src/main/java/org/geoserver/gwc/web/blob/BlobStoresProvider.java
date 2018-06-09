/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.geoserver.gwc.GWC;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geowebcache.config.BlobStoreConfig;

/**
 * Provider for Table of Blobstores.
 *
 * @author Niels Charlier
 */
public class BlobStoresProvider extends GeoServerDataProvider<BlobStoreConfig> {

    private static final long serialVersionUID = 4400431816195261839L;

    public static final Property<BlobStoreConfig> ID =
            new BeanProperty<BlobStoreConfig>("id", "id");

    public static final Property<BlobStoreConfig> TYPE =
            new BeanProperty<BlobStoreConfig>("type", "class");

    public static final Property<BlobStoreConfig> ENABLED =
            new BeanProperty<BlobStoreConfig>("enabled", "enabled");

    public static final Property<BlobStoreConfig> DEFAULT =
            new BeanProperty<BlobStoreConfig>("default", "default");

    @Override
    protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<BlobStoreConfig>>
            getProperties() {
        return Arrays.asList(ID, TYPE, ENABLED, DEFAULT);
    }

    @Override
    protected Comparator<BlobStoreConfig> getComparator(final SortParam<?> sort) {
        if (sort != null && sort.getProperty().equals(TYPE.getName())) {

            return new Comparator<BlobStoreConfig>() {
                @Override
                public int compare(BlobStoreConfig o1, BlobStoreConfig o2) {
                    int r =
                            BlobStoreTypes.getFromClass(o1.getClass())
                                    .toString()
                                    .compareTo(
                                            BlobStoreTypes.getFromClass(o2.getClass()).toString());
                    return sort.isAscending() ? r : -r;
                }
            };

        } else {
            return super.getComparator(sort);
        }
    }

    @Override
    protected List<BlobStoreConfig> getItems() {
        return (List<BlobStoreConfig>) GWC.get().getBlobStores();
    }
}
