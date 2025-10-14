/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import java.io.Serial;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.geoserver.gwc.GWC;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geowebcache.config.BlobStoreInfo;

/**
 * Provider for Table of Blobstores.
 *
 * @author Niels Charlier
 */
public class BlobStoresProvider extends GeoServerDataProvider<BlobStoreInfo> {

    @Serial
    private static final long serialVersionUID = 4400431816195261839L;

    public static final Property<BlobStoreInfo> ID = new BeanProperty<>("id", "id");

    public static final Property<BlobStoreInfo> TYPE = new BeanProperty<>("type", "class");

    public static final Property<BlobStoreInfo> ENABLED = new BeanProperty<>("enabled", "enabled");

    public static final Property<BlobStoreInfo> DEFAULT = new BeanProperty<>("default", "default");

    @Override
    protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<BlobStoreInfo>> getProperties() {
        return Arrays.asList(ID, TYPE, ENABLED, DEFAULT);
    }

    @Override
    protected Comparator<BlobStoreInfo> getComparator(final SortParam<?> sort) {
        if (sort != null && sort.getProperty().equals(TYPE.getName())) {

            return (o1, o2) -> {
                int r = BlobStoreTypes.getFromClass(o1.getClass())
                        .toString()
                        .compareTo(BlobStoreTypes.getFromClass(o2.getClass()).toString());
                return sort.isAscending() ? r : -r;
            };

        } else {
            return super.getComparator(sort);
        }
    }

    @Override
    protected List<BlobStoreInfo> getItems() {
        return GWC.get().getBlobStores();
    }
}
