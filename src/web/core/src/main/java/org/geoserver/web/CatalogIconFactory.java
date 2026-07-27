/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.web.data.resource.DataStorePanelInfo;
import org.geoserver.web.wicket.GsIcon;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/** Utility class used to lookup icons, including for various catalog objects. */
@SuppressWarnings("serial")
public class CatalogIconFactory implements Serializable {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web");

    public static final String RASTER_ICON = "gs-icon-raster";
    public static final String VECTOR_ICON = "gs-icon-vector";
    public static final String MAP_ICON = "gs-icon-map";
    public static final String MAP_STORE_ICON = "gs-icon-server-map";
    public static final String POINT_ICON = "gs-icon-bullet-blue";
    public static final String LINE_ICON = "gs-icon-line-blue";
    public static final String POLYGON_ICON = "gs-icon-shape-square-blue";
    public static final String GEOMETRY_ICON = "gs-icon-vector";
    public static final String UNKNOWN_ICON = "gs-icon-error";
    public static final String GROUP_ICON = "gs-icon-layers";
    public static final String DISABLED_ICON = "gs-icon-error";
    public static final String ENABLED_ICON = "gs-icon-tick";

    static final CatalogIconFactory INSTANCE = new CatalogIconFactory();

    public static final CatalogIconFactory get() {
        return INSTANCE;
    }

    private CatalogIconFactory() {
        // private constructor, this is a singleton
    }

    /**
     * Returns a GsIcon component for the given CSS class.
     *
     * @param id Wicket id for the icon component
     * @param cssClass CSS class for the icon (e.g. "gs-icon-raster")
     * @return GsIcon component
     */
    public WebComponent getIcon(String id, String cssClass) {
        if (id == null) {
            id = "image";
        }
        if (cssClass == null) {
            cssClass = UNKNOWN_ICON;
        }
        return new GsIcon(id, cssClass);
    }

    /** Returns the appropriate icon CSS class for the specified layer */
    public String getLayerIcon(LayerInfo info) {
        String icon = UNKNOWN_ICON;
        if (info.getType() == PublishedType.VECTOR) icon = VECTOR_ICON;
        else if (info.getType() == PublishedType.RASTER) icon = RASTER_ICON;
        return icon;
    }

    /**
     * Returns the appropriate icon CSS class for the specified layer. This one distinguishes the geometry type inside
     * vector layers.
     */
    public String getSpecificLayerIcon(LayerInfo info) {
        if (info.getType() == PublishedType.RASTER) {
            return RASTER_ICON;
        } else if (info.getType() == PublishedType.VECTOR) {
            try {
                FeatureTypeInfo fti = (FeatureTypeInfo) info.getResource();
                GeometryDescriptor gd = fti.getFeatureType().getGeometryDescriptor();
                return getVectoryIcon(gd);
            } catch (Exception e) {
                return GEOMETRY_ICON;
            }
        } else if (info.getType() == PublishedType.WMS) {
            return MAP_ICON;
        } else if (info.getType() == PublishedType.WMTS) {
            return MAP_ICON;
        } else {
            return UNKNOWN_ICON;
        }
    }

    /** Returns the vector icon CSS class associated to the specified geometry descriptor */
    public String getVectoryIcon(GeometryDescriptor gd) {
        if (gd == null) {
            return GEOMETRY_ICON;
        }

        Class<?> geom = gd.getType().getBinding();
        return getVectorIcon(geom);
    }

    public String getVectorIcon(Class<?> geom) {
        if (Point.class.isAssignableFrom(geom) || MultiPoint.class.isAssignableFrom(geom)) {
            return POINT_ICON;
        } else if (LineString.class.isAssignableFrom(geom) || MultiLineString.class.isAssignableFrom(geom)) {
            return LINE_ICON;
        } else if (Polygon.class.isAssignableFrom(geom) || MultiPolygon.class.isAssignableFrom(geom)) {
            return POLYGON_ICON;
        } else {
            return GEOMETRY_ICON;
        }
    }

    /**
     * Returns the appropriate icon CSS class for the specified store.
     *
     * @see #getStoreIcon(Class)
     */
    public String getStoreIcon(final StoreInfo storeInfo) {

        Catalog catalog = storeInfo.getCatalog();
        final ResourcePool resourcePool = catalog.getResourcePool();

        if (storeInfo instanceof DataStoreInfo info1) {
            DataAccessFactory dataStoreFactory = null;
            try {
                dataStoreFactory = resourcePool.getDataStoreFactory(info1);
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "factory class for storeInfo " + storeInfo.getName() + " not found", e);
            }

            if (dataStoreFactory != null) {
                return getStoreIcon(dataStoreFactory.getClass());
            }

        } else if (storeInfo instanceof CoverageStoreInfo info) {
            AbstractGridFormat format = resourcePool.getGridCoverageFormat(info);
            if (format != null) {
                return getStoreIcon(format.getClass());
            }
        } else if (storeInfo instanceof WMSStoreInfo) {
            return MAP_STORE_ICON;
        } else if (storeInfo instanceof WMTSStoreInfo) {
            return MAP_STORE_ICON;
        } else {
            throw new IllegalStateException(storeInfo.getClass().getName());
        }

        LOGGER.info("Could not determine icon for StoreInfo " + storeInfo.getName() + ". Using 'unknown' icon.");
        return UNKNOWN_ICON;
    }

    /**
     * Returns the appropriate icon CSS class given a data access or coverage factory class.
     *
     * <p>The lookup is performed by first searching for a registered {@link DataStorePanelInfo} for the given store
     * factory class, if not found, the icon for the {@link DataStorePanelInfo} registered with the id
     * {@code defaultVector} or {@code defaultRaster}, as appropriate will be used.
     *
     * @param factoryClass either a {@link DataAccessFactory} or a {@link Format} class
     */
    public String getStoreIcon(Class<?> factoryClass) {
        // look for the associated panel info if there is one
        final List<DataStorePanelInfo> infos = GeoServerApplication.get().getBeansOfType(DataStorePanelInfo.class);

        for (DataStorePanelInfo panelInfo : infos) {
            if (factoryClass.equals(panelInfo.getFactoryClass())) {
                return panelInfo.getIcon();
            }
        }

        if (DataAccessFactory.class.isAssignableFrom(factoryClass)) {
            // search for the declared default vector store icon
            for (DataStorePanelInfo panelInfo : infos) {
                if ("defaultVector".equals(panelInfo.getId())) {
                    return panelInfo.getIcon();
                }
            }

            // fall back on generic vector icon otherwise
            return "gs-icon-database-vector";

        } else if (Format.class.isAssignableFrom(factoryClass)) {
            // search for the declared default coverage store icon
            for (DataStorePanelInfo panelInfo : infos) {
                if ("defaultRaster".equals(panelInfo.getId())) {
                    return panelInfo.getIcon();
                }
            }

            // fall back on generic raster icon otherwise
            return "gs-icon-page-white-raster";
        }
        throw new IllegalArgumentException("Unrecognized store format class: " + factoryClass);
    }

    /**
     * Returns a {@link WebComponent} rendering the icon for the given store. For stores with a CSS class icon, returns
     * a {@link GsIcon}. For stores with a custom file-based icon (declared via {@code iconBase} in
     * {@link DataStorePanelInfo}), returns an {@link Image}.
     */
    public WebComponent getStoreIconComponent(String id, StoreInfo storeInfo) {
        if (storeInfo instanceof WMSStoreInfo || storeInfo instanceof WMTSStoreInfo) {
            return new GsIcon(id, MAP_STORE_ICON);
        }

        Catalog catalog = storeInfo.getCatalog();
        final ResourcePool resourcePool = catalog.getResourcePool();

        Class<?> factoryClass = null;
        if (storeInfo instanceof DataStoreInfo info1) {
            try {
                DataAccessFactory f = resourcePool.getDataStoreFactory(info1);
                if (f != null) factoryClass = f.getClass();
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "factory class for storeInfo " + storeInfo.getName() + " not found", e);
            }
        } else if (storeInfo instanceof CoverageStoreInfo info) {
            AbstractGridFormat format = resourcePool.getGridCoverageFormat(info);
            if (format != null) factoryClass = format.getClass();
        }

        if (factoryClass != null) {
            return getStoreIconComponent(id, factoryClass);
        }
        return new GsIcon(id, UNKNOWN_ICON);
    }

    /**
     * Returns a {@link WebComponent} rendering the icon for the given factory class. For CSS class icons returns a
     * {@link GsIcon}; for stores with a custom file-based icon declared via {@code iconBase} in
     * {@link DataStorePanelInfo}, returns an {@link Image}.
     */
    public WebComponent getStoreIconComponent(String id, Class<?> factoryClass) {
        final List<DataStorePanelInfo> infos = GeoServerApplication.get().getBeansOfType(DataStorePanelInfo.class);

        for (DataStorePanelInfo panelInfo : infos) {
            if (factoryClass.equals(panelInfo.getFactoryClass())) {
                return createIconComponent(id, panelInfo, factoryClass);
            }
        }
        // no exact match — fall back to default panel
        return new GsIcon(id, getStoreIcon(factoryClass));
    }

    private WebComponent createIconComponent(String id, DataStorePanelInfo panelInfo, Class<?> factoryClass) {
        if (panelInfo.getIconBase() != null && panelInfo.getIcon() != null) {
            return new Image(id, new PackageResourceReference(panelInfo.getIconBase(), panelInfo.getIcon()));
        }
        if (panelInfo.getIcon() != null) {
            return new GsIcon(id, panelInfo.getIcon());
        }
        return new GsIcon(id, getStoreIcon(factoryClass));
    }

    /** Returns the CSS class for a general purpose icon to indicate an enabled/properly configured resource */
    public String getEnabledIcon() {
        return ENABLED_ICON;
    }

    /** Returns the CSS class for a general purpose icon to indicate a disabled/misconfigured/unreachable resource */
    public String getDisabledIcon() {
        return DISABLED_ICON;
    }
}
