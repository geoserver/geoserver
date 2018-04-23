/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.Unit;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;

import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WMTSStoreInfoImpl;
import org.geoserver.data.util.CoverageStoreUtils;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.TypeMap;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureTypes;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.resources.image.ImageUtilities;
import org.geotools.util.NumberRange;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.ColorInterpretation;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Builder class which provides convenience methods for interacting with the catalog.
 * <p>
 * Warning: this class is stateful, and is not meant to be accessed by multiple threads and should
 * not be an member variable of another class.
 * </p>
 * 
 * @author Justin Deoliveira, OpenGEO
 * 
 */
public class CatalogBuilder {

    static final Logger LOGGER = Logging.getLogger(CatalogBuilder.class);

    /** Default SRS; will be set on the provided feature type by lookupSRS methods if none was found */
    public static final String DEFAULT_SRS = "EPSG:404000";

    /**
     * the catalog
     */
    Catalog catalog;

    /**
     * the current workspace
     */
    WorkspaceInfo workspace;

    /**
     * the current store
     */
    StoreInfo store;

    public CatalogBuilder(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Sets the workspace to be used when creating store objects.
     */
    public void setWorkspace(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }

    /**
     * Sets the store to be used when creating resource objects.
     */
    public void setStore(StoreInfo store) {
        this.store = store;
    }

    /**
     * Updates a workspace with the properties of another.
     * 
     * @param original
     *            The workspace being updated.
     * @param update
     *            The workspace containing the new values.
     */
    public void updateWorkspace(WorkspaceInfo original, WorkspaceInfo update) {
        update(original, update, WorkspaceInfo.class);
    }

    /**
     * Updates a namespace with the properties of another.
     * 
     * @param original
     *            The namespace being updated.
     * @param update
     *            The namespace containing the new values.
     */
    public void updateNamespace(NamespaceInfo original, NamespaceInfo update) {
        update(original, update, NamespaceInfo.class);
    }

    /**
     * Updates a datastore with the properties of another.
     * 
     * @param original
     *            The datastore being updated.
     * @param update
     *            The datastore containing the new values.
     */
    public void updateDataStore(DataStoreInfo original, DataStoreInfo update) {
        update(original, update, DataStoreInfo.class);
    }

    /**
     * Updates a wms store with the properties of another.
     * 
     * @param original
     *            The wms store being updated.
     * @param update
     *            The wms store containing the new values.
     */
    public void updateWMSStore(WMSStoreInfo original, WMSStoreInfo update) {
        update(original, update, WMSStoreInfo.class);
    }

    /**
     * Updates a wmts store with the properties of another.
     *
     * @param original
     *            The wmts store being updated.
     * @param update
     *            The wmts store containing the new values.
     */
    public void updateWMTSStore(WMTSStoreInfo original, WMTSStoreInfo update) {
        update(original, update, WMTSStoreInfo.class);
    }

    /**
     * Updates a coveragestore with the properties of another.
     * 
     * @param original
     *            The coveragestore being updated.
     * @param update
     *            The coveragestore containing the new values.
     */
    public void updateCoverageStore(CoverageStoreInfo original, CoverageStoreInfo update) {
        update(original, update, CoverageStoreInfo.class);
    }

    /**
     * Updates a feature type with the properties of another.
     * 
     * @param original
     *            The feature type being updated.
     * @param update
     *            The feature type containing the new values.
     */
    public void updateFeatureType(FeatureTypeInfo original, FeatureTypeInfo update) {
        update(original, update, FeatureTypeInfo.class);
    }

    /**
     * Updates a coverage with the properties of another.
     * 
     * @param original
     *            The coverage being updated.
     * @param update
     *            The coverage containing the new values.
     */
    public void updateCoverage(CoverageInfo original, CoverageInfo update) {
        update(original, update, CoverageInfo.class);
    }

    /**
     * Updates a WMS layer with the properties of another.
     * 
     * @param original
     *            The wms layer being updated.
     * @param update
     *            The wms layer containing the new values.
     */
    public void updateWMSLayer(WMSLayerInfo original, WMSLayerInfo update) {
        update(original, update, WMSLayerInfo.class);
    }
    /**
     * Updates a WMTS layer with the properties of another.
     * 
     * @param original
     *            The wmts layer being updated.
     * @param update
     *            The wmts layer containing the new values.
     */
    public void updateWMTSLayer(WMTSLayerInfo original, WMTSLayerInfo update) {
        update(original, update, WMTSLayerInfo.class);
    }
    /**
     * Updates a layer with the properties of another.
     * 
     * @param original
     *            The layer being updated.
     * @param update
     *            The layer containing the new values.
     */
    public void updateLayer(LayerInfo original, LayerInfo update) {
        update(original, update, LayerInfo.class);
    }

    /**
     * Updates a layer group with the properties of another.
     * 
     * @param original
     *            The layer group being updated.
     * @param update
     *            The layer group containing the new values.
     */
    public void updateLayerGroup(LayerGroupInfo original, LayerGroupInfo update) {
        update(original, update, LayerGroupInfo.class);
    }

    /**
     * Updates a style with the properties of another.
     * 
     * @param original
     *            The style being updated.
     * @param update
     *            The style containing the new values.
     */
    public void updateStyle(StyleInfo original, StyleInfo update) {
        update(original, update, StyleInfo.class);
    }

    /**
     * Update method which uses reflection to grab property values from one object and set them on
     * another.
     * <p>
     * Null values from the <tt>update</tt> object are ignored.
     * </p>
     */
    <T> void update(T original, T update, Class<T> clazz) {
        OwsUtils.copy(update, original, clazz);
    }

    /**
     * Builds a new data store.
     */
    public DataStoreInfo buildDataStore(String name) {
        DataStoreInfo info = catalog.getFactory().createDataStore();
        buildStore(info, name);

        return info;
    }

    /**
     * Builds a new coverage store.
     */
    public CoverageStoreInfo buildCoverageStore(String name) {
        CoverageStoreInfo info = catalog.getFactory().createCoverageStore();
        buildStore(info, name);

        return info;
    }

    /**
     * Builds a new WMS store
     */
    public WMSStoreInfo buildWMSStore(String name) throws IOException {
        WMSStoreInfo info = catalog.getFactory().createWebMapServer();
        buildStore(info, name);
        info.setType("WMS");
        info.setMaxConnections(WMSStoreInfoImpl.DEFAULT_MAX_CONNECTIONS);
        info.setConnectTimeout(WMSStoreInfoImpl.DEFAULT_CONNECT_TIMEOUT);
        info.setReadTimeout(WMSStoreInfoImpl.DEFAULT_READ_TIMEOUT);

        return info;
    }
    
    /**
     * Builds a new WMTS store
     */
    public WMTSStoreInfo buildWMTSStore(String name) throws IOException {
        WMTSStoreInfo info = catalog.getFactory().createWebMapTileServer();
        buildStore(info, name);
        info.setType("WMTS");
        info.setMaxConnections(WMTSStoreInfoImpl.DEFAULT_MAX_CONNECTIONS);
        info.setConnectTimeout(WMTSStoreInfoImpl.DEFAULT_CONNECT_TIMEOUT);
        info.setReadTimeout(WMTSStoreInfoImpl.DEFAULT_READ_TIMEOUT);

        return info;
    }

    /**
     * Builds a store.
     * <p>
     * The workspace of the resulting store is {@link #workspace} if set, else the default workspace
     * from the catalog.
     * </p>
     */
    void buildStore(StoreInfo info, String name) {

        info.setName(name);
        info.setEnabled(true);

        // set workspace, falling back on default if none specified
        if (workspace != null) {
            info.setWorkspace(workspace);
        } else {
            info.setWorkspace(catalog.getDefaultWorkspace());
        }
    }

    /**
     * Builds a {@link FeatureTypeInfo} from the current datastore and the specified type name
     * <p>
     * The resulting object is not added to the catalog, it must be done by the calling code after
     * the fact.
     * </p>
     */
    public FeatureTypeInfo buildFeatureType(Name typeName) throws Exception {
        if (store == null || !(store instanceof DataStoreInfo)) {
            throw new IllegalStateException("Data store not set.");
        }

        DataStoreInfo dstore = (DataStoreInfo) store;
        return buildFeatureType(dstore.getDataStore(null).getFeatureSource(typeName));
    }

    /**
     * Builds a feature type from a geotools feature source. The resulting {@link FeatureTypeInfo}
     * will still miss the bounds and might miss the SRS. Use {@link #lookupSRS(FeatureTypeInfo,
     * true)} and {@link #setupBounds(FeatureTypeInfo)} if you want to force them in (and spend time
     * accordingly)
     * <p>
     * The resulting object is not added to the catalog, it must be done by the calling code after
     * the fact.
     * </p>
     */
    public FeatureTypeInfo buildFeatureType(FeatureSource featureSource) {
        if (store == null || !(store instanceof DataStoreInfo)) {
            throw new IllegalStateException("Data store not set.");
        }

        FeatureType featureType = featureSource.getSchema();

        FeatureTypeInfo ftinfo = catalog.getFactory().createFeatureType();
        ftinfo.setStore(store);
        ftinfo.setEnabled(true);

        // naming
        Name name = featureSource.getName();
        if (name == null) {
            name = featureType.getName();
        }
        ftinfo.setNativeName(name.getLocalPart());
        ftinfo.setName(name.getLocalPart());

        WorkspaceInfo workspace = store.getWorkspace();
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspace.getName());
        if (namespace == null) {
            namespace = catalog.getDefaultNamespace();
        }

        ftinfo.setNamespace(namespace);

        CoordinateReferenceSystem crs = featureType.getCoordinateReferenceSystem();
        if (crs == null && featureType.getGeometryDescriptor() != null) {
            crs = featureType.getGeometryDescriptor().getCoordinateReferenceSystem();
        }
        ftinfo.setNativeCRS(crs);

        // srs look and set (by default we just use fast lookup)
        try {
            lookupSRS(ftinfo, false);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "SRS lookup failed", e);
        }
        setupProjectionPolicy(ftinfo);

        // fill in metadata, first check if the datastore itself can provide some metadata for us
        try {
            setupMetadata(ftinfo, featureSource);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Metadata lookup failed", e);
        }
        
        return ftinfo;
    }

    /**
     * Sets the projection policy for a resource based on the following rules:
     * <ul>
     * <li>If getSRS() returns a non null value it is set to {@Link
     * ProjectionPolicy#FORCE_DECLARED}
     * <li>If getSRS() returns a null value it is set to {@link ProjectionPolicy#NONE}
     * </ul>
     * 
     * TODO: make this method smarter, and compare the native crs to figure out if prejection
     * actually needs to be done, and sync it up with setting proj policy on coverage layers.
     */
    public void setupProjectionPolicy(ResourceInfo rinfo) {
        if (rinfo.getSRS() != null) {
            rinfo.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        } else {
            rinfo.setProjectionPolicy(ProjectionPolicy.NONE);
        }
    }

    /**
     * Computes the native bounds for a {@link FeatureTypeInfo} explicitly providing the feature
     *  source.
     * <p>
     * This method calls through to {@link #doSetupBounds(ResourceInfo, Object)}.
     * </p>
     */
    public void setupBounds(FeatureTypeInfo ftinfo, FeatureSource featureSource) throws IOException {
        doSetupBounds(ftinfo, featureSource);
    }

    /**
     * Computes the native bounds for a {@link CoverageInfo} explicitly providing the coverage 
     *  reader.
     * <p>
     * This method calls through to {@link #doSetupBounds(ResourceInfo, Object)}.
     * </p>
     */
    public void setupBounds(CoverageInfo cinfo, GridCoverage2DReader coverageReader) 
        throws IOException {
        doSetupBounds(cinfo, coverageReader);
    }

    /**
     * Given a {@link ResourceInfo} this method:
     * <ul>
     * <li>computes, if missing, the native bounds (warning, this might be very expensive, cases in
     * which this case take minutes are not uncommon if the data set is made of million of features)
     * </li>
     * <li>updates, if possible, the geographic bounds accordingly by re-projecting the native
     * bounds into WGS84</li>
     * 
     * @param ftinfo
     * @throws IOException
     *             if computing the native bounds fails or if a transformation error occurs during
     *             the geographic bounds computation
     */
    public void setupBounds(ResourceInfo rinfo) throws IOException {
        doSetupBounds(rinfo, null);
    }

    /*
     * Helper method for setupBounds() methods which can optionally take a "data" object rather
     * than access it through the catalog. This allows for this method to be called for info objects
     * that might not be part of the catalog.
     */
    void doSetupBounds(ResourceInfo rinfo, Object data) throws IOException {
        // setup the native bbox if needed
        if (rinfo.getNativeBoundingBox() == null) {
            ReferencedEnvelope bounds = getNativeBounds(rinfo, data);
            rinfo.setNativeBoundingBox(bounds);
        }

        // setup the geographic bbox if missing and we have enough info
        rinfo.setLatLonBoundingBox(getLatLonBounds(rinfo.getNativeBoundingBox(), rinfo.getCRS()));
    }

    /**
     * Fills in metadata on the {@link FeatureTypeInfo} from an underlying feature source.
     */
    public void setupMetadata(FeatureTypeInfo ftinfo, FeatureSource featureSource) 
        throws IOException {

        org.geotools.data.ResourceInfo rinfo = null;
        try {
            rinfo = featureSource.getInfo();
        }
        catch(Exception ignore) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Unable to get resource info from feature source", ignore);
            }
        }

        if (ftinfo.getTitle() == null) {
            ftinfo.setTitle(rinfo != null ? rinfo.getTitle() : ftinfo.getName());
        }
        if (rinfo != null && ftinfo.getDescription() == null) {
            ftinfo.setDescription(rinfo.getDescription());
        }
        if (rinfo != null && (ftinfo.getKeywords() == null || ftinfo.getKeywords().isEmpty())) {
            if (rinfo.getKeywords() != null) {
                if (ftinfo.getKeywords() == null) {
                    ((FeatureTypeInfoImpl)ftinfo).setKeywords(new ArrayList());
                }
                for (String kw : rinfo.getKeywords()) {
                    if (kw == null || "".equals(kw.trim())) {
                        LOGGER.fine("Empty keyword ignored");
                        continue;
                    }
                    ftinfo.getKeywords().add(new Keyword(kw));
                }
            }
        }
    }

    /**
     * Computes the geographic bounds of a {@link ResourceInfo} by reprojecting the available native
     * bounds
     * 
     * @param rinfo
     * @return the geographic bounds, or null if the native bounds are not available
     * @throws IOException
     */
    public ReferencedEnvelope getLatLonBounds(ReferencedEnvelope nativeBounds,
            CoordinateReferenceSystem declaredCRS) throws IOException {
        if (nativeBounds != null && declaredCRS != null) {
            // make sure we use the declared CRS, not the native one, the may differ
            if (!CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, declaredCRS)) {
                // transform
                try {
                    ReferencedEnvelope bounds = new ReferencedEnvelope(nativeBounds, CRS.getHorizontalCRS(declaredCRS));
                    return bounds.transform(DefaultGeographicCRS.WGS84, true);
                } catch (Exception e) {
                    throw (IOException) new IOException("transform error").initCause(e);
                }
            } else {
                return new ReferencedEnvelope(nativeBounds, DefaultGeographicCRS.WGS84);
            }
        }
        return null;
    }

    /**
     * Computes the native bounds of a {@link ResourceInfo} taking into account the nature of the
     * data and the reprojection policy in act
     * 
     * @param rinfo
     * @return the native bounds, or null if the could not be computed
     * @throws IOException
     */
    public ReferencedEnvelope getNativeBounds(ResourceInfo rinfo) throws IOException {
        return getNativeBounds(rinfo, null);
    }

    /*
     * Helper method for getNativeBounds() methods which can optionally take a "data" object rather
     * than access it through the catalog. This allows for this method to be called for info objects
     * that might not be part of the catalog.
     */
    ReferencedEnvelope getNativeBounds(ResourceInfo rinfo, Object data) throws IOException {
        ReferencedEnvelope bounds = null;
        if (rinfo instanceof FeatureTypeInfo) {
            FeatureTypeInfo ftinfo = (FeatureTypeInfo) rinfo;

            // bounds
            if (data instanceof FeatureSource) {
                bounds = ((FeatureSource)data).getBounds();
            }
            else {
                bounds = ftinfo.getFeatureSource(null, null).getBounds();
            }

            // fix the native bounds if necessary, some datastores do
            // not build a proper referenced envelope
            CoordinateReferenceSystem crs = ftinfo.getNativeCRS();
            if (bounds != null && bounds.getCoordinateReferenceSystem() == null && crs != null) {
                bounds = new ReferencedEnvelope(bounds, crs);
            }

            if (bounds != null) {
                // expansion factor if the bounds are empty or one dimensional
                double expandBy = 1; // 1 meter
                if (bounds.getCoordinateReferenceSystem() instanceof GeographicCRS) {
                    expandBy = 0.0001;
                }
                if (bounds.getWidth() == 0 || bounds.getHeight() == 0) {
                    bounds.expandBy(expandBy);
                }
            }

        } else if (rinfo instanceof CoverageInfo) {
            // the coverage bounds computation path is a bit more linear, the
            // readers always return the bounds and in the proper CRS (afaik)
            CoverageInfo cinfo = (CoverageInfo) rinfo;            
            GridCoverage2DReader reader = null;
            if (data instanceof GridCoverage2DReader) {
                reader = (GridCoverage2DReader) data;
            }
            else {
                reader = (GridCoverage2DReader) 
                    cinfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
            }

            // get  bounds
            bounds = new ReferencedEnvelope(reader.getOriginalEnvelope());
           
        } else if(rinfo instanceof WMSLayerInfo) {
            // the logic to compute the native bounds is pretty convoluted,
            // let's rebuild the layer info
            WMSLayerInfo rebuilt = buildWMSLayer(rinfo.getStore(), rinfo.getNativeName());
            bounds = rebuilt.getNativeBoundingBox();

        } else if(rinfo instanceof WMTSLayerInfo) {
            // the logic to compute the native bounds is pretty convoluted,
            // let's rebuild the layer info
            WMTSLayerInfo rebuilt = buildWMTSLayer(rinfo.getStore(), rinfo.getNativeName());
            bounds = rebuilt.getNativeBoundingBox();
        }

        // apply the bounds, taking into account the reprojection policy if need be
        if (rinfo.getProjectionPolicy() == ProjectionPolicy.REPROJECT_TO_DECLARED && bounds != null) {
            try {
                bounds = bounds.transform(rinfo.getCRS(), true);
            } catch (Exception e) {
                throw (IOException) new IOException("transform error").initCause(e);
            }
        }

        return bounds;
    }

    /**
     * Looks up and sets the SRS based on the feature type info native
     * {@link CoordinateReferenceSystem}
     * 
     * @param ftinfo
     * @param extensive
     *            if true an extenstive lookup will be performed (more accurate, but might take
     *            various seconds)
     * @throws IOException
     */
    public void lookupSRS(FeatureTypeInfo ftinfo, boolean extensive) throws IOException {
        lookupSRS(ftinfo, null, extensive);
    }

    /**
     * Looks up and sets the SRS based on the feature type info native 
     * {@link CoordinateReferenceSystem}, obtained from an optional feature source.
     * 
     * @param ftinfo
     * @param data A feature source (possibily null)
     * @param extensive
     *            if true an extenstive lookup will be performed (more accurate, but might take
     *            various seconds)
     * @throws IOException
     */
    public void lookupSRS(FeatureTypeInfo ftinfo, FeatureSource data, boolean extensive) 
            throws IOException {
        CoordinateReferenceSystem crs = ftinfo.getNativeCRS();
        if (crs == null) {
            if (data != null) {
                crs = data.getSchema().getCoordinateReferenceSystem();
            }
            else {
                crs = ftinfo.getFeatureType().getCoordinateReferenceSystem();
            }
        }
        if (crs != null) {
            try {
                Integer code = CRS.lookupEpsgCode(crs, extensive);
                if (code != null)
                    ftinfo.setSRS("EPSG:" + code);
            } catch (FactoryException e) {
                throw (IOException) new IOException().initCause(e);
            }
        } else {
            ftinfo.setSRS(DEFAULT_SRS);
        }
    }

    /**
     * Initializes basic resource info.
     */
    private void initResourceInfo(ResourceInfo resInfo) throws Exception {
    	// set the name
    	if (resInfo.getNativeName() == null && resInfo.getName() != null) {
    		resInfo.setNativeName(resInfo.getName());
    	}
    	if (resInfo.getNativeName() != null && resInfo.getName() == null) {
    		resInfo.setName(resInfo.getNativeName());
    	}
    }

    /**
     * Initializes a feature type object setting any info that has not been set.
     */
    public void initFeatureType(FeatureTypeInfo featureType) throws Exception {
        if (featureType.getCatalog() == null) {
            featureType.setCatalog(catalog);
        }

        initResourceInfo(featureType);

        // setup the srs if missing
        if (featureType.getSRS() == null) {
            lookupSRS(featureType, true);
        }
        if (featureType.getProjectionPolicy() == null) {
            setupProjectionPolicy(featureType);
        }

        // deal with bounding boxes as possible
        CoordinateReferenceSystem crs = featureType.getCRS();
        if (featureType.getLatLonBoundingBox() == null
                && featureType.getNativeBoundingBox() == null) {
            // both missing, we compute them
            setupBounds(featureType);
        } else if (featureType.getLatLonBoundingBox() == null) {
            // native available but geographic to be computed
            setupBounds(featureType);
        } else if (featureType.getNativeBoundingBox() == null && crs != null) {
            // we know the geographic and we can reproject back to native
            ReferencedEnvelope boundsLatLon = featureType.getLatLonBoundingBox();
            featureType.setNativeBoundingBox(boundsLatLon.transform(crs, true));
        }
    }

    /**
     * Initializes a wms layer object setting any info that has not been set.
     */
    public void initWMSLayer(WMSLayerInfo wmsLayer) throws Exception {
        wmsLayer.setCatalog(catalog);

        initResourceInfo(wmsLayer);
        OwsUtils.resolveCollections(wmsLayer);

        // get a fully initialized version we can copy from
        WMSLayerInfo full = buildWMSLayer(store, wmsLayer.getNativeName());

        // setup the srs if missing
        if (wmsLayer.getSRS() == null) {
            wmsLayer.setSRS(full.getSRS());
        }
        if (wmsLayer.getNativeCRS() == null) {
            wmsLayer.setNativeCRS(full.getNativeCRS());
        }
        if (wmsLayer.getProjectionPolicy() == null) {
            wmsLayer.setProjectionPolicy(full.getProjectionPolicy());
        }

        // deal with bounding boxes as possible
        if (wmsLayer.getLatLonBoundingBox() == null
                && wmsLayer.getNativeBoundingBox() == null) {
            // both missing, we copy them
            wmsLayer.setLatLonBoundingBox(full.getLatLonBoundingBox());
            wmsLayer.setNativeBoundingBox(full.getNativeBoundingBox());
        } else if (wmsLayer.getLatLonBoundingBox() == null) {
            // native available but geographic to be computed
            setupBounds(wmsLayer);
        } else if (wmsLayer.getNativeBoundingBox() == null && wmsLayer.getNativeCRS() != null) {
            // we know the geographic and we can reproject back to native
            ReferencedEnvelope boundsLatLon = wmsLayer.getLatLonBoundingBox();
            wmsLayer.setNativeBoundingBox(boundsLatLon.transform(wmsLayer.getNativeCRS(), true));
        }

        //fill in missing metadata
        if (wmsLayer.getTitle() == null) {
            wmsLayer.setTitle(full.getTitle());
        }
        if (wmsLayer.getDescription() == null) {
            wmsLayer.setDescription(full.getDescription());
        }
        if (wmsLayer.getAbstract() == null) {
            wmsLayer.setAbstract(full.getAbstract());
        }
        if (wmsLayer.getKeywords().isEmpty()) {
            wmsLayer.getKeywords().addAll(full.getKeywords());
        }
    }

    /**
     * Initializes a wmts layer object setting any info that has not been set.
     */
    public void initWMTSLayer(WMTSLayerInfo layer) throws Exception {
        layer.setCatalog(catalog);

        initResourceInfo(layer);
        OwsUtils.resolveCollections(layer);

        // get a fully initialized version we can copy from
        WMTSLayerInfo full = buildWMTSLayer(store, layer.getNativeName());

        // setup the srs if missing
        if (layer.getSRS() == null) {
            layer.setSRS(full.getSRS());
        }
        if (layer.getNativeCRS() == null) {
            layer.setNativeCRS(full.getNativeCRS());
        }
        if (layer.getProjectionPolicy() == null) {
            layer.setProjectionPolicy(full.getProjectionPolicy());
        }

        // deal with bounding boxes as possible
        if (layer.getLatLonBoundingBox() == null
                && layer.getNativeBoundingBox() == null) {
            // both missing, we copy them
            layer.setLatLonBoundingBox(full.getLatLonBoundingBox());
            layer.setNativeBoundingBox(full.getNativeBoundingBox());
        } else if (layer.getLatLonBoundingBox() == null) {
            // native available but geographic to be computed
            setupBounds(layer);
        } else if (layer.getNativeBoundingBox() == null && layer.getNativeCRS() != null) {
            // we know the geographic and we can reproject back to native
            ReferencedEnvelope boundsLatLon = layer.getLatLonBoundingBox();
            layer.setNativeBoundingBox(boundsLatLon.transform(layer.getNativeCRS(), true));
        }

        //fill in missing metadata
        if (layer.getTitle() == null) {
            layer.setTitle(full.getTitle());
        }
        if (layer.getDescription() == null) {
            layer.setDescription(full.getDescription());
        }
        if (layer.getAbstract() == null) {
            layer.setAbstract(full.getAbstract());
        }
        if (layer.getKeywords().isEmpty()) {
            layer.getKeywords().addAll(full.getKeywords());
        }
    }

    /**
     * Initialize a coverage object and set any unset info.
     */
    public void initCoverage(CoverageInfo cinfo) throws Exception {
        initCoverage(cinfo, null);
    }
    
    /**
     * Initialize a coverage object and set any unset info.
     */
    public void initCoverage(CoverageInfo cinfo, final String coverageName) throws Exception {
    	CoverageStoreInfo csinfo = (CoverageStoreInfo) store;
        GridCoverage2DReader reader = (GridCoverage2DReader) catalog
            	.getResourcePool().getGridCoverageReader(cinfo, GeoTools.getDefaultHints());
        if(coverageName != null) {
            reader = SingleGridCoverage2DReader.wrap(reader, coverageName);
        }
        
        initResourceInfo(cinfo);

        if (reader == null)
            throw new Exception("Unable to acquire a reader for this coverage with format: "
                    + csinfo.getFormat().getName());

        if (cinfo.getNativeCRS() == null) {
        	cinfo.setNativeCRS(reader.getCoordinateReferenceSystem());
        }

        CoordinateReferenceSystem nativeCRS = cinfo.getNativeCRS();

        if (cinfo.getSRS() == null) {
        	cinfo.setSRS(nativeCRS.getIdentifiers().toArray()[0].toString());
        }

        if (cinfo.getProjectionPolicy() == null) {
            if (nativeCRS != null && !nativeCRS.getIdentifiers().isEmpty()) {
                cinfo.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
            }
            if (nativeCRS == null) {
                cinfo.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
            }
        }

    	if (cinfo.getLatLonBoundingBox() == null
    			&& cinfo.getNativeBoundingBox() == null) {
    		GeneralEnvelope envelope = reader.getOriginalEnvelope();

    		cinfo.setNativeBoundingBox(new ReferencedEnvelope(envelope));
    		cinfo.setLatLonBoundingBox(new ReferencedEnvelope(CoverageStoreUtils.getWGS84LonLatEnvelope(envelope)));
    	} else if (cinfo.getLatLonBoundingBox() == null) {
    		setupBounds(cinfo);
    	} else if (cinfo.getNativeBoundingBox() == null && cinfo.getNativeCRS() != null) {
    		ReferencedEnvelope boundsLatLon = cinfo.getLatLonBoundingBox();
    		cinfo.setNativeBoundingBox(boundsLatLon.transform(cinfo.getNativeCRS(), true));
    	}

        if (cinfo.getGrid() == null) {
            GridEnvelope originalRange = reader.getOriginalGridRange();
            cinfo.setGrid(new GridGeometry2D(originalRange, reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER), nativeCRS));
        }
    }
    
    /**
     * Builds the default coverage contained in the current store
     * 
     *
     */
    public CoverageInfo buildCoverage() throws Exception {
        return buildCoverage(null);
    }

    /**
     * Builds the default coverage contained in the current store
     * 
     *
     */
    public CoverageInfo buildCoverage(String coverageName) throws Exception {
        if (store == null || !(store instanceof CoverageStoreInfo)) {
            throw new IllegalStateException("Coverage store not set.");
        }

        CoverageStoreInfo csinfo = (CoverageStoreInfo) store;
        GridCoverage2DReader reader = (GridCoverage2DReader) catalog
                .getResourcePool().getGridCoverageReader(csinfo, GeoTools.getDefaultHints());

        if (reader == null)
            throw new Exception("Unable to acquire a reader for this coverage with format: "
                    + csinfo.getFormat().getName());

        return buildCoverage(reader, coverageName, null);
    }

    /**
     * Builds the default coverage contained in the current store
     *
     * @param nativeCoverageName the native name for the coverage
     * @param specifiedName the published name for the coverage. If null, the name will be determined from the coverage store.
     * @return coverage for the specified name
     * @throws Exception if the coverage store was not found or could not be read, or if the coverage could not be created.
     */
    public CoverageInfo buildCoverageByName(String nativeCoverageName, String specifiedName) throws Exception {
        if (store == null || !(store instanceof CoverageStoreInfo)) {
            throw new IllegalStateException("Coverage store not set.");
        }

        CoverageStoreInfo csinfo = (CoverageStoreInfo) store;
        GridCoverage2DReader reader = (GridCoverage2DReader) catalog
                .getResourcePool().getGridCoverageReader(csinfo, GeoTools.getDefaultHints());

        if (reader == null)
            throw new Exception("Unable to acquire a reader for this coverage with format: "
                    + csinfo.getFormat().getName());

        return buildCoverageInternal(reader, nativeCoverageName, null, specifiedName);
    }

    /**
     * Builds a coverage from a geotools grid coverage reader.
     * @param customParameters 
     */
    public CoverageInfo buildCoverage(GridCoverage2DReader reader, Map customParameters) throws Exception {
        return buildCoverage(reader, null, customParameters);
    }

    /**
     * Builds a coverage from a geotools grid coverage reader.
     * @param customParameters 
     */
    public CoverageInfo buildCoverage(GridCoverage2DReader reader, String coverageName, Map customParameters) throws Exception {
        return buildCoverageInternal(reader, coverageName, customParameters, null);
    }

    private CoverageInfo buildCoverageInternal(GridCoverage2DReader reader, String nativeCoverageName, Map customParameters, String specifiedName) throws Exception {
        if (store == null || !(store instanceof CoverageStoreInfo)) {
            throw new IllegalStateException("Coverage store not set.");
        }
        
        // if we are dealing with a multicoverage reader, wrap to simplify code
        if (nativeCoverageName != null) {
            reader = SingleGridCoverage2DReader.wrap(reader, nativeCoverageName);
        }

        CoverageStoreInfo csinfo = (CoverageStoreInfo) store;
        CoverageInfo cinfo = catalog.getFactory().createCoverage();

        cinfo.setStore(csinfo);
        cinfo.setEnabled(true);

        WorkspaceInfo wspace = store.getWorkspace();
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(wspace.getName());
        if (namespace == null) {
            namespace = catalog.getDefaultNamespace();
        }
        cinfo.setNamespace(namespace);

        GeneralEnvelope envelope = reader.getOriginalEnvelope();
        CoordinateReferenceSystem nativeCRS = envelope.getCoordinateReferenceSystem();
        cinfo.setNativeCRS(nativeCRS);

        // mind the default projection policy, Coverages do not have a flexible
        // handling as feature types, they do reproject if the native srs is set,
        // force if missing
        if (nativeCRS != null) {
            try {
                Integer code = CRS.lookupEpsgCode(nativeCRS, false);
                if (code != null) {
                    cinfo.setSRS("EPSG:" + code);
                    cinfo.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
                }
            } catch (FactoryException e) {
                LOGGER.log(Level.WARNING, "SRS lookup failed", e);
            }
        }
        if (nativeCRS == null) {
            cinfo.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        }

        
        cinfo.setNativeBoundingBox(new ReferencedEnvelope(envelope));
        cinfo.setLatLonBoundingBox(new ReferencedEnvelope(CoverageStoreUtils.getWGS84LonLatEnvelope(envelope)));

        GridEnvelope originalRange = reader.getOriginalGridRange();
        cinfo.setGrid(new GridGeometry2D(originalRange, reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER), nativeCRS));

        // /////////////////////////////////////////////////////////////////////
        //
        // Now reading a fake small GridCoverage just to retrieve meta
        // information about bands:
        //
        // - calculating a new envelope which is just 5x5 pixels
        // - if it's a mosaic, limit the number of tiles we're going to read to one 
        //   (with time and elevation there might be hundreds of superimposed tiles)
        // - reading the GridCoverage subset
        //
        // /////////////////////////////////////////////////////////////////////
        Format format = csinfo.getFormat();
        final ParameterValueGroup readParams = format.getReadParameters();

        GridSampleDimension[] sampleDimensions = getCoverageSampleDimensions(reader, customParameters);
        List<CoverageDimensionInfo> coverageDimensions = getCoverageDimensions(sampleDimensions);

        cinfo.getDimensions().addAll(coverageDimensions);
        if (specifiedName != null) {
            cinfo.setName(specifiedName);
            cinfo.setTitle(specifiedName);
            cinfo.getKeywords().add(new Keyword(specifiedName));
        } else {
            String name = reader.getGridCoverageNames()[0];
            cinfo.setName(name);
            cinfo.setTitle(name);
            cinfo.getKeywords().add(new Keyword(name));
        }
        cinfo.setNativeCoverageName(nativeCoverageName);

        cinfo.setDescription(new StringBuilder("Generated from ").append(format.getName()).toString());

        // keywords
        cinfo.getKeywords().add(new Keyword("WCS"));
        cinfo.getKeywords().add(new Keyword(format.getName()));

        // native format name
        cinfo.setNativeFormat(format.getName());
        cinfo.getMetadata().put("dirName", new StringBuilder(store.getName()).append("_").append(nativeCoverageName).toString());

        // request and response SRS's
        if ((nativeCRS.getIdentifiers() != null)
                && !nativeCRS.getIdentifiers().isEmpty()) {
            cinfo.getRequestSRS().add(((Identifier) nativeCRS.getIdentifiers().toArray()[0]).toString());
            cinfo.getResponseSRS().add(((Identifier) nativeCRS.getIdentifiers().toArray()[0]).toString());
        }

        // supported formats
        final List formats = CoverageStoreUtils.listDataFormats();
        for (Iterator i = formats.iterator(); i.hasNext();) {
            final Format fTmp = (Format) i.next();
            final String fName = fTmp.getName();

            if (fName.equalsIgnoreCase("WorldImage")) {
                // TODO check if coverage can encode Format
                cinfo.getSupportedFormats().add("GIF");
                cinfo.getSupportedFormats().add("PNG");
                cinfo.getSupportedFormats().add("JPEG");
                cinfo.getSupportedFormats().add("TIFF");
            } else if (fName.toLowerCase().startsWith("geotiff")) {
                // TODO check if coverage can encode Format
                cinfo.getSupportedFormats().add("GEOTIFF");
            } else {
                // TODO check if coverage can encode Format
                cinfo.getSupportedFormats().add(fName);
            }
        }

        // interpolation methods
        cinfo.setDefaultInterpolationMethod("nearest neighbor");
        cinfo.getInterpolationMethods().add("nearest neighbor");
        cinfo.getInterpolationMethods().add("bilinear");
        cinfo.getInterpolationMethods().add("bicubic");

        // read parameters (get the params again since we altered the map to optimize the 
        // coverage read)
        cinfo.getParameters().putAll(CoverageUtils.getParametersKVP(readParams));

        return cinfo;
    }

    private GridSampleDimension[] getCoverageSampleDimensions(GridCoverage2DReader reader, Map customParameters)
            throws TransformException, IOException, Exception {
        GridEnvelope originalRange = reader.getOriginalGridRange();
        Format format = reader.getFormat();
        final ParameterValueGroup readParams = format.getReadParameters();
        final Map parameters = CoverageUtils.getParametersKVP(readParams);
        final int minX = originalRange.getLow(0);
        final int minY = originalRange.getLow(1);
        final int width = originalRange.getSpan(0);
        final int height = originalRange.getSpan(1);
        final int maxX = minX + (width <= 5 ? width : 5);
        final int maxY = minY + (height <= 5 ? height : 5);

        // we have to be sure that we are working against a valid grid range.
        final GridEnvelope2D testRange = new GridEnvelope2D(minX, minY, maxX, maxY);

        // build the corresponding envelope
        final MathTransform gridToWorldCorner = reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER);
       
        final GeneralEnvelope testEnvelope = CRS.transform(gridToWorldCorner, new GeneralEnvelope(testRange.getBounds()));
        testEnvelope.setCoordinateReferenceSystem(reader.getCoordinateReferenceSystem());

        if (customParameters != null) {
        	parameters.putAll(customParameters);
        }

        // make sure mosaics with many superimposed tiles won't blow up with 
        // a "too many open files" exception
        String maxAllowedTiles = ImageMosaicFormat.MAX_ALLOWED_TILES.getName().toString();
        if (parameters.keySet().contains(maxAllowedTiles)) {
            parameters.put(maxAllowedTiles, 1);
        }

        // Since the read sample image won't be greater than 5x5 pixels and we are limiting the
        // number of granules to 1, we may do direct read instead of using JAI
        String useJaiImageRead = ImageMosaicFormat.USE_JAI_IMAGEREAD.getName().toString();
        if (parameters.keySet().contains(useJaiImageRead)) {
            parameters.put(useJaiImageRead, false);
        }

        parameters.put(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString(), new GridGeometry2D(testRange, testEnvelope));

        // try to read this coverage
        final GridCoverage2D gc = reader.read(CoverageUtils.getParameters(readParams, parameters, true));
        final GridSampleDimension[] sampleDimensions;
        if (gc != null) {
            // remove read grid geometry since it is request specific
            parameters.remove(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString());
            sampleDimensions = gc.getSampleDimensions();
            /// dispose coverage 
            gc.dispose(true);
            if(gc.getRenderedImage() instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) gc.getRenderedImage());
            }
        } else {
            final ImageLayout imageLayout = reader.getImageLayout();
            if(imageLayout == null) {
                throw new Exception("Unable to acquire test coverage and image layout for format:" + format.getName());
            }
            ColorModel cm = imageLayout.getColorModel(null);
            if(cm == null) {
                throw new Exception("Unable to acquire test coverage and color model for format:" + format.getName());
            }
            SampleModel sm = imageLayout.getSampleModel(null);
            if(cm == null) {
                throw new Exception("Unable to acquire test coverage and sample model for format:" + format.getName());
            }
            final int numBands = sm.getNumBands();
            sampleDimensions = new GridSampleDimension[numBands];
            // setting bands names.
            for (int i = 0; i < numBands; i++) {
                final ColorInterpretation colorInterpretation = TypeMap.getColorInterpretation(cm, i);
                if (colorInterpretation == null)
                    throw new IOException("Unrecognized sample dimension type for band number " + (i + 1));
                sampleDimensions[i] = new GridSampleDimension(colorInterpretation.name());
            }

        }
        return sampleDimensions;
    }

    List<CoverageDimensionInfo> getCoverageDimensions(GridSampleDimension[] sampleDimensions) {

        final int length = sampleDimensions.length;
        List<CoverageDimensionInfo> dims = new ArrayList<CoverageDimensionInfo>();

        for (int i = 0; i < length; i++) {
            CoverageDimensionInfo dim = catalog.getFactory().createCoverageDimension();
            GridSampleDimension sd = sampleDimensions[i];
            String name = sd.getDescription().toString(Locale.getDefault());
            dim.setName(name);

            StringBuilder label = new StringBuilder("GridSampleDimension".intern());
            final Unit uom = sd.getUnits();

            String uName = name.toUpperCase();
            if (uom != null) {
                label.append("(".intern());
                parseUOM(label, uom);
                label.append(")".intern());
                dim.setUnit(uom.toString());
            } else if(uName.startsWith("RED") || uName.startsWith("GREEN") || uName.startsWith("BLUE")) {
                // radiance in SI
                dim.setUnit("W.m-2.Sr-1");
            }
            
            dim.setDimensionType(sd.getSampleDimensionType());

            double sdMin = sd.getMinimumValue();
            double sdMax = sd.getMaximumValue();
            label.append("[".intern());
            label.append(sdMin);
            label.append(",".intern());
            label.append(sdMax);
            label.append("]".intern());

            dim.setDescription(label.toString());
            // Since the nullValues element of the CoverageDimensionInfo reports
            // the nodata (if available), let's switch to use the 
            // sampleDimension's min and max as Dimension's Range
            // instead of the whole SampleDimension Range 
            // (the latter may include nodata categories).
            dim.setRange(NumberRange.create(sdMin, sdMax));

            final List<Category> categories = sd.getCategories();
            if (categories != null) {
                for (Category cat : categories) {

                    if ((cat != null) && cat.getName().toString(Locale.ENGLISH).equalsIgnoreCase("no data")) {
                        double min = cat.getRange().getMinimum();
                        double max = cat.getRange().getMaximum();

                        dim.getNullValues().add(min);
                        if (min != max) {
                            dim.getNullValues().add(max);
                        }
                    }
                }
            }
            
            dims.add(dim);
        }

        return dims;
    }
    
    public WMSLayerInfo buildWMSLayer(String layerName) throws IOException {
        return buildWMSLayer(this.store, layerName);
    }
    
    WMSLayerInfo buildWMSLayer(StoreInfo store, String layerName) throws IOException {
        if (store == null || !(store instanceof WMSStoreInfo)) {
            throw new IllegalStateException("WMS store not set.");
        }

        WMSLayerInfo wli = catalog.getFactory().createWMSLayer();

        wli.setName(layerName);
        wli.setNativeName(layerName);

        wli.setStore(store);
        wli.setEnabled(true);

        WorkspaceInfo workspace = store.getWorkspace();
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspace.getName());
        if (namespace == null) {
            namespace = catalog.getDefaultNamespace();
        }
        wli.setNamespace(namespace);

        Layer layer = wli.getWMSLayer(null);

        // try to get the native SRS -> we use the bounding boxes, GeoServer will publish all of the
        // supported SRS in the root, if we use getSRS() we'll get them all
        for (String srs : layer.getBoundingBoxes().keySet()) {
            try {
                CoordinateReferenceSystem crs = CRS.decode(srs);
                wli.setSRS(srs);
                wli.setNativeCRS(crs);
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Skipping " + srs
                        + " definition, it was not recognized by the referencing subsystem");
            }
        }
        
        // fall back on WGS84 if necessary, and handle well known WMS CRS codes
        String srs = wli.getSRS();
        try {
            if (srs == null || srs.equals("CRS:84")) {
                wli.setSRS("EPSG:4326");
                srs = "EPSG:4326";
                wli.setNativeCRS(CRS.decode("EPSG:4326"));
            } else if(srs.equals("CRS:83")) {
                wli.setSRS("EPSG:4269");
                srs = "EPSG:4269";
                wli.setNativeCRS(CRS.decode("EPSG:4269"));
            } else if(srs.equals("CRS:27")) {
                wli.setSRS("EPSG:4267");
                srs = "EPSG:4267";
                wli.setNativeCRS(CRS.decode("EPSG:4267"));
            }
        } catch(Exception e) {
            throw (IOException) new IOException("Failed to compute the layer declared SRS code").initCause(e);
        }
        wli.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);

        // try to grab the envelope
        GeneralEnvelope envelope = layer.getEnvelope(wli.getNativeCRS());
        if (envelope != null) {
            ReferencedEnvelope re = new ReferencedEnvelope(envelope.getMinimum(0), envelope
                    .getMaximum(0), envelope.getMinimum(1), envelope.getMaximum(1), wli
                    .getNativeCRS());
            wli.setNativeBoundingBox(re);
        }
        CRSEnvelope llbbox = layer.getLatLonBoundingBox();
        if (llbbox != null) {
            ReferencedEnvelope re = new ReferencedEnvelope(llbbox.getMinX(), llbbox.getMaxX(),
                    llbbox.getMinY(), llbbox.getMaxY(), DefaultGeographicCRS.WGS84);
            wli.setLatLonBoundingBox(re);
        } else if (wli.getNativeBoundingBox() != null) {
            try {
                wli.setLatLonBoundingBox(wli.getNativeBoundingBox().transform(
                        DefaultGeographicCRS.WGS84, true));
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Could not transform native bbox into a lat/lon one", e);
            }
        }

        // reflect all the metadata that we can grab
        wli.setAbstract(layer.get_abstract());
        wli.setDescription(layer.get_abstract());
        wli.setTitle(layer.getTitle());
        if (layer.getKeywords() != null) {
            for (String kw : layer.getKeywords()) {
                if(kw != null){
                    wli.getKeywords().add(new Keyword(kw));
                }
            }
        }

        // strip off the prefix if we're cascading from a server that does add them
        String published = wli.getName();
        if (published.contains(":")) {
            wli.setName(published.substring(published.lastIndexOf(':') + 1));
        }

        return wli;
    }
    
    public WMTSLayerInfo buildWMTSLayer(String layerName) throws IOException {
        return buildWMTSLayer(this.store, layerName);
    }

    WMTSLayerInfo buildWMTSLayer(StoreInfo store, String layerName) throws IOException {
        if (store == null || !(store instanceof WMTSStoreInfo)) {
            throw new IllegalStateException("WMTS store not set.");
        }

        WMTSLayerInfo wli = catalog.getFactory().createWMTSLayer();

        wli.setName(layerName);
        wli.setNativeName(layerName);

        wli.setStore(store);
        wli.setEnabled(true);

        WorkspaceInfo workspace = store.getWorkspace();
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspace.getName());
        if (namespace == null) {
            namespace = catalog.getDefaultNamespace();
        }
        wli.setNamespace(namespace);

        Layer layer = wli.getWMTSLayer(null);
        //TODO: handle axis order here ?
        // try to get the native SRS -> we use the bounding boxes, GeoServer will publish all of the
        // supported SRS in the root, if we use getSRS() we'll get them all
        for (String srs : layer.getBoundingBoxes().keySet()) {
            try {
                CoordinateReferenceSystem crs = CRS.decode(srs);
                wli.setSRS(srs);
                wli.setNativeCRS(crs);
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Skipping " + srs
                        + " definition, it was not recognized by the referencing subsystem");
            }
        }
        
        // fall back on WGS84 if necessary, and handle well known WMS CRS codes
        String srs = wli.getSRS();
        try {
            if (srs == null || srs.equals("CRS:84")) {
                wli.setSRS("EPSG:4326");
                srs = "EPSG:4326";
                wli.setNativeCRS(CRS.decode("EPSG:4326"));
            } else if(srs.equals("CRS:83")) {
                wli.setSRS("EPSG:4269");
                srs = "EPSG:4269";
                wli.setNativeCRS(CRS.decode("EPSG:4269"));
            } else if(srs.equals("CRS:27")) {
                wli.setSRS("EPSG:4267");
                srs = "EPSG:4267";
                wli.setNativeCRS(CRS.decode("EPSG:4267"));
            }
        } catch(Exception e) {
            throw (IOException) new IOException("Failed to compute the layer declared SRS code").initCause(e);
        }
        wli.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);

        // try to grab the envelope
        GeneralEnvelope envelope = layer.getEnvelope(wli.getNativeCRS());
        if (envelope != null) {
            ReferencedEnvelope re = new ReferencedEnvelope(envelope.getMinimum(0), envelope
                    .getMaximum(0), envelope.getMinimum(1), envelope.getMaximum(1), wli
                    .getNativeCRS());
            wli.setNativeBoundingBox(re);
        }
        CRSEnvelope llbbox = layer.getLatLonBoundingBox();
        if (llbbox != null) {
            ReferencedEnvelope re = new ReferencedEnvelope(llbbox.getMinX(), llbbox.getMaxX(),
                    llbbox.getMinY(), llbbox.getMaxY(), DefaultGeographicCRS.WGS84);
            wli.setLatLonBoundingBox(re);
        } else if (wli.getNativeBoundingBox() != null) {
            try {
                wli.setLatLonBoundingBox(wli.getNativeBoundingBox().transform(
                        DefaultGeographicCRS.WGS84, true));
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Could not transform native bbox into a lat/lon one", e);
            }
        }

        // reflect all the metadata that we can grab
        wli.setAbstract(layer.get_abstract());
        wli.setDescription(layer.get_abstract());
        wli.setTitle(layer.getTitle());
        if (layer.getKeywords() != null) {
            for (String kw : layer.getKeywords()) {
                if(kw != null){
                    wli.getKeywords().add(new Keyword(kw));
                }
            }
        }

        // strip off the prefix if we're cascading from a server that does add them
        String published = wli.getName();
        if (published.contains(":")) {
            wli.setName(published.substring(published.lastIndexOf(':') + 1));
        }

        return wli;
    }

    
    private boolean axisFlipped(Version version, String srsName) {
        if(version.compareTo(new Version("1.3.0")) < 0) {
            // aah, sheer simplicity
            return false;
        } else {
            // gah, hell gates breaking loose
            if(srsName.startsWith("EPSG:")) {
                try {
                    String epsgNative =  "urn:x-ogc:def:crs:EPSG:".concat(srsName.substring(5));
                    return CRS.getAxisOrder(CRS.decode(epsgNative)) == AxisOrder.NORTH_EAST;
                } catch(Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to determine axis order for " 
                            + srsName + ", assuming east/north", e);
                    return false;
                }
            } else {
                // CRS or AUTO, none of them is flipped so far
                return false;
            }
        }
    }

    void parseUOM(StringBuilder label, Unit uom) {
        String uomString = uom.toString();
        uomString = uomString.replaceAll("\u00B2", "^2");
        uomString = uomString.replaceAll("\u00B3", "^3");
        uomString = uomString.replaceAll("\u212B", "A");
        uomString = uomString.replaceAll("�", "");
        label.append(uomString);
    }

    /**
     * Builds a layer for a feature type.
     * <p>
     * The resulting object is not added to the catalog, it must be done by the calling code after
     * the fact.
     * </p>
     */
    public LayerInfo buildLayer(FeatureTypeInfo featureType) throws IOException {
        // also create a layer for the feautre type
        LayerInfo layer = buildLayer((ResourceInfo) featureType);

        StyleInfo style = getDefaultStyle(featureType);
        layer.setDefaultStyle(style);

        return layer;
    }

    /**
     * Builds a layer for a coverage.
     * <p>
     * The resulting object is not added to the catalog, it must be done by the calling code after
     * the fact.
     * </p>
     */
    public LayerInfo buildLayer(CoverageInfo coverage) throws IOException {
        LayerInfo layer = buildLayer((ResourceInfo) coverage);

        layer.setDefaultStyle(getDefaultStyle(coverage));

        return layer;
    }

    /**
     * Builds a layer wrapping a WMS layer resource
     * <p>
     * The resulting object is not added to the catalog, it must be done by the calling code after
     * the fact.
     * </p>
     */
    public LayerInfo buildLayer(WMSLayerInfo wms) throws IOException {
        LayerInfo layer = buildLayer((ResourceInfo) wms);
        
        layer.setDefaultStyle(getDefaultStyle(wms));
        
        return layer;
    }

    /**
     * Returns the default style for the specified resource, or null if the layer is vector and
     * geometryless
     * 
     * @param resource
     *
     * @throws IOException
     */
    public StyleInfo getDefaultStyle(ResourceInfo resource) throws IOException {
        // raster wise, only one style
        if (resource instanceof CoverageInfo || resource instanceof WMSLayerInfo || resource instanceof WMTSLayerInfo)
            return catalog.getStyleByName(StyleInfo.DEFAULT_RASTER);

        // for vectors we depend on the the nature of the default geometry
        String styleName;
        FeatureTypeInfo featureType = (FeatureTypeInfo) resource;
        if (featureType.getFeatureType() == null) {
            return null;
        }
        GeometryDescriptor gd = featureType.getFeatureType().getGeometryDescriptor();
        if (gd == null) {
            return null;
        }

        Class gtype = gd.getType().getBinding();
        if (Point.class.isAssignableFrom(gtype) || MultiPoint.class.isAssignableFrom(gtype)) {
            styleName = StyleInfo.DEFAULT_POINT;
        } else if (LineString.class.isAssignableFrom(gtype)
                || MultiLineString.class.isAssignableFrom(gtype)) {
            styleName = StyleInfo.DEFAULT_LINE;
        } else if (Polygon.class.isAssignableFrom(gtype)
                || MultiPolygon.class.isAssignableFrom(gtype)) {
            styleName = StyleInfo.DEFAULT_POLYGON;
        } else if (Point.class.isAssignableFrom(gtype) || MultiPoint.class.isAssignableFrom(gtype)) {
            styleName = StyleInfo.DEFAULT_POINT;
        } else {
            // fall back to the generic style
            styleName = StyleInfo.DEFAULT_GENERIC;
        }

        return catalog.getStyleByName(styleName);
    }

    public LayerInfo buildLayer(ResourceInfo resource) {
        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(resource);
        layer.setName(resource.getName());
        layer.setEnabled(true);

        // setup the layer type
        if (layer.getResource() instanceof FeatureTypeInfo) {
            layer.setType(PublishedType.VECTOR);
        } else if (layer.getResource() instanceof CoverageInfo) {
            layer.setType(PublishedType.RASTER);
        } else if (layer.getResource() instanceof WMTSLayerInfo) {
            layer.setType(PublishedType.WMTS);
        } else if (layer.getResource() instanceof WMSLayerInfo) {
            layer.setType(PublishedType.WMS);
        }

        return layer;
    }

    /**
     * Calculates the bounds of a layer group specifying a particular crs.
     */
    public void calculateLayerGroupBounds(LayerGroupInfo layerGroup, CoordinateReferenceSystem crs)
            throws Exception {
        LayerGroupHelper helper = new LayerGroupHelper(layerGroup);
        helper.calculateBounds(crs);
    }
    
    /**
     * Calculate the bounds of a layer group from the CRS defined bounds. 
     * Relies on the {@link LayerGroupHelper}
     * 
     * @param layerGroup
     * @param crs the CRS who's bounds should be used
     * @see LayerGroupHelper#calculateBoundsFromCRS(CoordinateReferenceSystem)
     */
    public void calculateLayerGroupBoundsFromCRS(
            LayerGroupInfo layerGroup, CoordinateReferenceSystem crs) {
        LayerGroupHelper helper = new LayerGroupHelper(layerGroup);
        helper.calculateBoundsFromCRS(crs);
    }

    /**
     * Calculates the bounds of a layer group by aggregating the bounds of each layer.
     */
    public void calculateLayerGroupBounds(LayerGroupInfo layerGroup) throws Exception {
        LayerGroupHelper helper = new LayerGroupHelper(layerGroup);
        helper.calculateBounds();
    }

    //
    // remove methods
    //

    /**
     * Removes a workspace from the catalog.
     * <p>
     * The <tt>recursive</tt> flag controls whether objects linked to the workspace such as stores
     * should also be deleted.
     * </p>
     */
    public void removeWorkspace(WorkspaceInfo workspace, boolean recursive) {
        if (recursive) {
            workspace.accept(new CascadeDeleteVisitor(catalog));
        } else {
            catalog.remove(workspace);
        }
    }

    /**
     * Removes a store from the catalog.
     * <p>
     * The <tt>recursive</tt> flag controls whether objects linked to the store such as resources
     * should also be deleted.
     * </p>
     */
    public void removeStore(StoreInfo store, boolean recursive) {
        if (recursive) {
            store.accept(new CascadeDeleteVisitor(catalog));
        } else {
            catalog.remove(store);
        }
    }

    /**
     * Removes a resource from the catalog.
     * <p>
     * The <tt>recursive</tt> flag controls whether objects linked to the resource such as layers
     * should also be deleted.
     * </p>
     */
    public void removeResource(ResourceInfo resource, boolean recursive) {
        if (recursive) {
            resource.accept(new CascadeDeleteVisitor(catalog));
        } else {
            catalog.remove(resource);
        }
    }

    /**
     * Reattaches a serialized {@link StoreInfo} to the catalog
     */
    public void attach(StoreInfo storeInfo) {
        storeInfo = ModificationProxy.unwrap(storeInfo);
        ((StoreInfoImpl) storeInfo).setCatalog(catalog);
    }

    /**
     * Reattaches a serialized {@link ResourceInfo} to the catalog
     */
    public void attach(ResourceInfo resourceInfo) {
        resourceInfo = ModificationProxy.unwrap(resourceInfo);
        ((ResourceInfoImpl) resourceInfo).setCatalog(catalog);
    }

    /**
     * Reattaches a serialized {@link LayerInfo} to the catalog
     */
    public void attach(LayerInfo layerInfo) {
        attach(layerInfo.getResource());
    }

    /**
     * Reattaches a serialized {@link MapInfo} to the catalog
     */
    public void attach(MapInfo mapInfo) {
        // hmmm... mapInfo has a list of layers inside? Not names?
        for (LayerInfo layer : mapInfo.getLayers()) {
            attach(layer);
        }
    }

    /**
     * Reattaches a serialized {@link LayerGroupInfo} to the catalog
     */
    public void attach(LayerGroupInfo groupInfo) {
        if (groupInfo.getRootLayer() != null) {
            attach(groupInfo.getRootLayer());
        }
        
        if (groupInfo.getRootLayerStyle() != null) {
            attach(groupInfo.getRootLayerStyle());            
        }
        
        for (PublishedInfo p : groupInfo.getLayers()) {
            if (p instanceof LayerInfo) {
                attach((LayerInfo) p);
            } else {
                attach((LayerGroupInfo) p);                
            }
        }
        
        for (StyleInfo style : groupInfo.getStyles()) {
            if (style != null)
                attach(style);
        }
    }

    /**
     * Reattaches a serialized {@link StyleInfo} to the catalog
     */
    public void attach(StyleInfo styleInfo) {
        styleInfo = ModificationProxy.unwrap(styleInfo);
        ((StyleInfoImpl) styleInfo).setCatalog(catalog);
    }

    /**
     * Reattaches a serialized {@link NamespaceInfo} to the catalog
     */
    public void attach(NamespaceInfo nsInfo) {
        // nothing to do
    }

    /**
     * Reattaches a serialized {@link WorkspaceInfo} to the catalog
     */
    public void attach(WorkspaceInfo wsInfo) {
        // nothing to do
    }
    
    /**
     * Extracts the AttributeTypeInfo by copying them from the specified feature type.
     * @param ft The schema to be harvested
     * @param info The optional feature type info from which all the attributes belong to
     *
     */
    public List<AttributeTypeInfo> getAttributes(FeatureType ft, FeatureTypeInfo info) {
        List<AttributeTypeInfo> attributes = new ArrayList<AttributeTypeInfo>();
        for (PropertyDescriptor pd : ft.getDescriptors()) {
            AttributeTypeInfo att = catalog.getFactory().createAttribute();
            att.setFeatureType(info);
            att.setName(pd.getName().getLocalPart());
            att.setMinOccurs(pd.getMinOccurs());
            att.setMaxOccurs(pd.getMaxOccurs());
            att.setNillable(pd.isNillable());
            att.setBinding(pd.getType().getBinding());
            int length = FeatureTypes.getFieldLength(pd);
            if(length > 0) {
                att.setLength(length);
            }
            attributes.add(att);
        }
        
        return attributes;
    }

    /**
     * Creates referenced envelope from resource based off the native or declared SRS. This bbox
     * depends on the projection policy.
     * 
     * <ul>
     *  <li>force declared, reproject native to declared: use the declared SRS bounding box </li>
     *  <li>keep native: use the native SRS bounding box</li>
     * <ul>
     * 
     * @param resource
     * @return the new referenced envelope or null if there is no bounding box associated with the 
     *         CRS
     */
    public ReferencedEnvelope getBoundsFromCRS(ResourceInfo resource) {
        ReferencedEnvelope crsReferencedEnvelope = null;
        
        ProjectionPolicy projPolicy = resource.getProjectionPolicy();
        CoordinateReferenceSystem crs = null;
        
        //find the right crs to use based on the projection policy
        if (projPolicy == ProjectionPolicy.NONE) {
            crs = resource.getNativeCRS();
        }
        else {
            crs = resource.getCRS();
        }
        
        if (crs != null) {
            Envelope crsEnvelope = CRS.getEnvelope(crs);
            if (crsEnvelope != null) {
                crsReferencedEnvelope = new ReferencedEnvelope(crsEnvelope);    
            } 
        }
        
        return crsReferencedEnvelope;
    }
}
