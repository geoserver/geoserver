/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.gwc.GWC;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.grid.GridSet;

public abstract class GridSetTableProvider extends GeoServerDataProvider<GridSet> {

    private static final long serialVersionUID = 399110981279814481L;

    static final Property<GridSet> NAME = new BeanProperty<GridSet>("name", "name");

    static final Property<GridSet> EPSG_CODE =
            new AbstractProperty<GridSet>("epsg_code") {
                private static final long serialVersionUID = -4311392731568045337L;

                @Override
                public Object getPropertyValue(GridSet item) {
                    return item.getSrs().toString();
                }
            };

    static final Property<GridSet> TILE_DIMENSION =
            new AbstractProperty<GridSet>("tile_dimension") {
                private static final long serialVersionUID = 7300188694215155063L;

                @Override
                public Object getPropertyValue(GridSet item) {
                    return item.getTileWidth() + " x " + item.getTileHeight();
                }
            };

    static final Property<GridSet> ZOOM_LEVELS =
            new AbstractProperty<GridSet>("zoom_levels") {
                private static final long serialVersionUID = 3155098860179765581L;

                @Override
                public Integer getPropertyValue(GridSet item) {
                    return item.getNumLevels(); // this may fail if item.gridLevels is null
                }
            };

    static final Property<GridSet> QUOTA_USED =
            new AbstractProperty<GridSet>("quota_used") {
                private static final long serialVersionUID = 1152149141759317288L;

                @Override
                public Object getPropertyValue(GridSet item) {
                    String gridSetName = item.getName();
                    Quota usedQuotaByGridSet = GWC.get().getUsedQuotaByGridSet(gridSetName);
                    return usedQuotaByGridSet;
                }
            };

    static final Property<GridSet> ACTION_LINK =
            new AbstractProperty<GridSet>("") {
                private static final long serialVersionUID = -7593097569735264194L;

                @Override
                public Object getPropertyValue(GridSet item) {
                    return item.getName();
                }
            };

    public GridSetTableProvider() {}

    @Override
    public abstract List<GridSet> getItems();

    @Override
    protected List<Property<GridSet>> getProperties() {
        return Arrays.asList(NAME, EPSG_CODE, TILE_DIMENSION, ZOOM_LEVELS, QUOTA_USED, ACTION_LINK);
    }

    @Override
    protected Comparator<GridSet> getComparator(final SortParam<?> sort) {
        return super.getComparator(sort);
    }

    @Override
    public IModel<GridSet> newModel(GridSet gridset) {
        String name = gridset.getName();
        return new GridSetDetachableModel(name);
    }
}
