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
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.DataAccessFactory;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.coverage.grid.Format;
import org.opengis.feature.type.GeometryDescriptor;

/** Utility class used to lookup icons for various catalog objects */
@SuppressWarnings("serial")
public class CatalogIconFactory implements Serializable {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web");

    public static final PackageResourceReference RASTER_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/geosilk/raster.png");

    public static final PackageResourceReference VECTOR_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/geosilk/vector.png");

    public static final PackageResourceReference MAP_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/geosilk/map.png");

    public static final PackageResourceReference MAP_STORE_ICON =
            new PackageResourceReference(
                    GeoServerBasePage.class, "img/icons/geosilk/server_map.png");

    public static final PackageResourceReference POINT_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/bullet_blue.png");

    public static final PackageResourceReference LINE_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/line_blue.png");

    public static final PackageResourceReference POLYGON_ICON =
            new PackageResourceReference(
                    GeoServerBasePage.class, "img/icons/silk/shape_square_blue.png");

    public static final PackageResourceReference GEOMETRY_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/geosilk/vector.png");

    public static final PackageResourceReference UNKNOWN_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/error.png");

    public static final PackageResourceReference GROUP_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/layers.png");

    public static final PackageResourceReference DISABLED_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/error.png");

    public static final PackageResourceReference ENABLED_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/tick.png");

    static final CatalogIconFactory INSTANCE = new CatalogIconFactory();

    public static final CatalogIconFactory get() {
        return INSTANCE;
    }

    private CatalogIconFactory() {
        // private constructor, this is a singleton
    }

    /** Returns the appropriate icon for the specified layer */
    public PackageResourceReference getLayerIcon(LayerInfo info) {
        PackageResourceReference icon = UNKNOWN_ICON;
        if (info.getType() == PublishedType.VECTOR) icon = VECTOR_ICON;
        else if (info.getType() == PublishedType.RASTER) icon = RASTER_ICON;
        return icon;
    }

    /**
     * Returns the appropriate icon for the specified layer. This one distinguishes the geometry
     * type inside vector layers.
     */
    public PackageResourceReference getSpecificLayerIcon(LayerInfo info) {
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

    /** Returns the vector icon associated to the specified geometry descriptor */
    public PackageResourceReference getVectoryIcon(GeometryDescriptor gd) {
        if (gd == null) {
            return GEOMETRY_ICON;
        }

        Class<?> geom = gd.getType().getBinding();
        return getVectorIcon(geom);
    }

    public PackageResourceReference getVectorIcon(Class<?> geom) {
        if (Point.class.isAssignableFrom(geom) || MultiPoint.class.isAssignableFrom(geom)) {
            return POINT_ICON;
        } else if (LineString.class.isAssignableFrom(geom)
                || MultiLineString.class.isAssignableFrom(geom)) {
            return LINE_ICON;
        } else if (Polygon.class.isAssignableFrom(geom)
                || MultiPolygon.class.isAssignableFrom(geom)) {
            return POLYGON_ICON;
        } else {
            return GEOMETRY_ICON;
        }
    }

    /**
     * Returns the appropriate icon for the specified store.
     *
     * @see #getStoreIcon(Class)
     */
    public PackageResourceReference getStoreIcon(final StoreInfo storeInfo) {

        Catalog catalog = storeInfo.getCatalog();
        final ResourcePool resourcePool = catalog.getResourcePool();

        if (storeInfo instanceof DataStoreInfo) {
            DataAccessFactory dataStoreFactory = null;
            try {
                dataStoreFactory = resourcePool.getDataStoreFactory((DataStoreInfo) storeInfo);
            } catch (IOException e) {
                LOGGER.log(
                        Level.INFO,
                        "factory class for storeInfo " + storeInfo.getName() + " not found",
                        e);
            }

            if (dataStoreFactory != null) {
                return getStoreIcon(dataStoreFactory.getClass());
            }

        } else if (storeInfo instanceof CoverageStoreInfo) {
            AbstractGridFormat format =
                    resourcePool.getGridCoverageFormat((CoverageStoreInfo) storeInfo);
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

        LOGGER.info(
                "Could not determine icon for StoreInfo "
                        + storeInfo.getName()
                        + ". Using 'unknown' icon.");
        return UNKNOWN_ICON;
    }

    /**
     * Returns the appropriate icon given a data access or coverage factory class.
     *
     * <p>The lookup is performed by first searching for a registered {@link DataStorePanelInfo} for
     * the given store factory class, if not found, the icon for the {@link DataStorePanelInfo}
     * registered with the id {@code defaultVector} or {@code defaultRaster}, as appropriate will be
     * used.
     *
     * @param factoryClass either a {@link DataAccessFactory} or a {@link Format} class
     */
    public PackageResourceReference getStoreIcon(Class<?> factoryClass) {
        // look for the associated panel info if there is one
        final List<DataStorePanelInfo> infos;
        infos = GeoServerApplication.get().getBeansOfType(DataStorePanelInfo.class);

        for (DataStorePanelInfo panelInfo : infos) {
            if (factoryClass.equals(panelInfo.getFactoryClass())) {
                return new PackageResourceReference(panelInfo.getIconBase(), panelInfo.getIcon());
            }
        }

        if (DataAccessFactory.class.isAssignableFrom(factoryClass)) {
            // search for the declared default vector store icon
            for (DataStorePanelInfo panelInfo : infos) {
                if ("defaultVector".equals(panelInfo.getId())) {
                    return new PackageResourceReference(
                            panelInfo.getIconBase(), panelInfo.getIcon());
                }
            }

            // fall back on generic vector icon otherwise
            return new PackageResourceReference(
                    GeoServerApplication.class, "img/icons/geosilk/database_vector.png");

        } else if (Format.class.isAssignableFrom(factoryClass)) {
            // search for the declared default coverage store icon
            for (DataStorePanelInfo panelInfo : infos) {
                if ("defaultRaster".equals(panelInfo.getId())) {
                    return new PackageResourceReference(
                            panelInfo.getIconBase(), panelInfo.getIcon());
                }
            }

            // fall back on generic raster icon otherwise
            return new PackageResourceReference(
                    GeoServerApplication.class, "img/icons/geosilk/page_white_raster.png");
        }
        throw new IllegalArgumentException("Unrecognized store format class: " + factoryClass);
    }

    /**
     * Returns a reference to a general purpose icon to indicate an enabled/properly configured
     * resource
     */
    public PackageResourceReference getEnabledIcon() {
        return ENABLED_ICON;
    }

    /**
     * Returns a reference to a general purpose icon to indicate a
     * disabled/missconfigured/unreachable resource
     */
    public PackageResourceReference getDisabledIcon() {
        return DISABLED_ICON;
    }
}
