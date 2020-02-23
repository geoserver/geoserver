/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.platform.ServiceException;
import org.geotools.data.*;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.crs.ForceCoordinateSystemFeatureReader;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.RemoteOWS;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.util.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.ProgressListener;

/**
 * Visitor for standalone {@link org.geotools.styling.StyledLayerDescriptor}s
 *
 * <p>Visits each {@link StyledLayer}s of an SLD. Resolves LayerGroups, Remote OWS references, and
 * inline features into individual layers Visits each {@link Style} in each{@link StyledLayer},
 * alongside each of these resolved layers
 *
 * <p>Intended to provide a definitive, extensible approach to parsing standalone {@link
 * org.geotools.styling.StyledLayerDescriptor}s, including style groups and external SLDs.
 */
public abstract class GeoServerSLDVisitor extends AbstractStyleVisitor {

    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.catalog");

    protected final Catalog catalog;
    protected final CoordinateReferenceSystem fallbackCrs;

    // Current state
    protected PublishedInfo info;
    protected StyledLayer layer;
    protected int styleCount = 0;

    /**
     * Constructs a new GeoServerSLDVisitor
     *
     * @param catalog GeoServer catalog to use for looking up catalog objects (Layers, LayerGroups,
     *     Styles)
     * @param fallbackCrs The CRS to use for inline features if it is not specified in the feature.
     *     Defaults to {@link DefaultGeographicCRS#WGS84}.
     */
    public GeoServerSLDVisitor(Catalog catalog, CoordinateReferenceSystem fallbackCrs) {
        this.catalog = catalog;
        this.fallbackCrs = fallbackCrs;
    }

    /**
     * Called on each named layer in the style
     *
     * <p>Implementations should resolve and return the corresponding PublishedInfo
     *
     * @param namedLayer The named layer
     * @return The resolved catalog layer
     */
    public abstract PublishedInfo visitNamedLayerInternal(StyledLayer namedLayer);

    /**
     * Called on each remote OWS user layer in the style
     *
     * @param userLayer The user layer
     */
    public abstract void visitUserLayerRemoteOWS(UserLayer userLayer);

    /**
     * Called on each inline feature user layer in the style
     *
     * @param userLayer The user layer
     */
    public abstract void visitUserLayerInlineFeature(UserLayer userLayer);

    /**
     * Called on each named style for each styled layer. In cases where the styled layer references
     * multiple layers, such as a remote OWS user layer exposing multiple layers, this will be
     * called for once of each of these layers, which will be passed in through the info argument.
     *
     * <p>Implementations may resolve and return the corresponding StyleInfo
     *
     * @param namedStyle The named style
     * @return The resolved catalog style
     */
    public abstract StyleInfo visitNamedStyleInternal(NamedStyle namedStyle);

    /**
     * Called on each named style for each styled layer. In cases where the styled layer references
     * multiple layers, such as a named layer referencing a layer group, or remote OWS user layer
     * exposing multiple layers, this will be called for once of each of these layers, which will be
     * passed in through the info argument.
     *
     * <p>Note: currently, in the case of a named layer referencing a layer group, any styles
     * defined in the SLD will be ignored, and the layer groups defined styles will be used instead,
     * being handled as user layers.
     *
     * @param userStyle The user style
     */
    public abstract void visitUserStyleInternal(Style userStyle);

    /**
     * Visit the SLD
     *
     * <p>Visit each layer. Construct temporary stores for inline features and remote OWS layers.
     * After visiting a layer, visit each style in that layer. In cases where the styled layer
     * references multiple layers, such as a named layer referencing a layer group, or remote OWS
     * user layer exposing multiple layers, visit each style for each of these sublayers instead.
     *
     * @param sld The sld the visitor is applied to
     * @throws UnsupportedOperationException, If the sld uses features not supported by GeoServer,
     *     such as if an OWS service other than WFS is specified for a UserLayer
     * @throws IllegalStateException If the sld is somehow invalid, such as if there is a NamedLayer
     *     without a name
     * @throws ServiceException if there was a problem accessing a remote OWS service
     * @throws UncheckedIOException if there is an underlying {@link IOException} when reading
     *     {@link Style} objects from the catalog
     */
    @Override
    public void visit(StyledLayerDescriptor sld) {
        if (sld.getStyledLayers().length == 0) {
            throw new IllegalStateException("SLD document contains no layers");
        }
        super.visit(sld);
    }

    @Override
    public void visit(NamedLayer layer) {
        if (null == layer.getName()) {
            throw new ServiceException("A UserLayer or NamedLayer without layer name was passed");
        }
        setLayerState(visitNamedLayerInternal(layer), layer);
        if (!handleLayerGroup()) {
            super.visit(layer);
        }
        handleNoStyles();
        clearLayerState();
    }

    @Override
    public void visit(UserLayer layer) {
        if (null == layer.getName()) {
            throw new ServiceException("A UserLayer or NamedLayer without layer name was passed");
        }
        if (layer.getRemoteOWS() != null) {
            List<LayerInfo> layers = getRemoteLayersFromUserLayer(layer);
            visitUserLayerRemoteOWS(layer);

            // UserLayer - Remote OWS: Apply each style to each layer
            for (LayerInfo layerInfo : layers) {
                setLayerState(layerInfo, layer);
                super.visit(layer);
                clearLayerState();
            }
            // We've already handled the styles, don't need to do anything more
            return;

        } else if (layer.getInlineFeatureDatastore() != null) {
            try {
                setLayerState(getInlineFeatureLayer(layer, fallbackCrs), layer);
            } catch (SchemaException e) {
                throw new IllegalStateException(e);
            } catch (IOException io) {
                throw new UncheckedIOException(io);
            }
            visitUserLayerInlineFeature(layer);
        } else {
            // TODO: By the SLD spec, we shouldn't be supporting UserLayers as NamedLayers, but we
            // do anyways.
            setLayerState(visitNamedLayerInternal(layer), layer);
        }
        super.visit(layer);
        handleNoStyles();
        clearLayerState();
    }

    protected void setLayerState(PublishedInfo info, StyledLayer layer) {
        this.info = info;
        this.layer = layer;
    }

    protected void clearLayerState() {
        this.info = null;
        this.layer = null;
    }

    protected boolean handleLayerGroup() {
        // NamedLayer - LayerGroup: ignore any defined styles and use the layer group instead
        if (info != null && info instanceof LayerGroupInfo) {
            LayerGroupInfo lg = (LayerGroupInfo) info;

            List<LayerInfo> layers = lg.layers();
            List<StyleInfo> styles = lg.styles();

            try {
                for (int i = 0; i < layers.size(); i++) {
                    info = layers.get(i);
                    StyleInfo style = styles.get(i);
                    if (style == null) {
                        visit(layers.get(i).getDefaultStyle().getStyle());
                    } else {
                        visit(styles.get(i).getStyle());
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return true;
        }
        return false;
    }

    protected void handleNoStyles() {
        // handle no styles -- use default
        try {
            if (styleCount == 0 && info != null && info instanceof LayerInfo) {
                StyleInfo styleInfo = ((LayerInfo) info).getDefaultStyle();

                if (styleInfo != null) {
                    visit(styleInfo.getStyle());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            // clean up state
            styleCount = 0;
        }
    }

    @Override
    public void visit(Style style) {
        styleCount++;
        if (style instanceof NamedStyle) {
            visitNamedStyleInternal((NamedStyle) style);
        } else {
            visitUserStyleInternal(style);
        }
        super.visit(style);
    }

    /**
     * Constructs a {@link DataStore} from a remote OWS specified in a {@link UserLayer}, and wraps
     * each feature matching one of the supplied {@link FeatureTypeConstraint}s in a {@link
     * LayerInfo}
     *
     * @return The list of layers wrapping the exposed features
     * @throws UnsupportedOperationException, if an OWS service other than WFS is specified
     * @throws ServiceException if there was a problem accessing the remote service
     */
    protected List<LayerInfo> getRemoteLayersFromUserLayer(UserLayer ul) throws ServiceException {
        try {
            RemoteOWS service = ul.getRemoteOWS();
            if (!service.getService().equalsIgnoreCase("WFS"))
                throw new UnsupportedOperationException(
                        "GeoServer only supports WFS as remoteOWS service");
            if (service.getOnlineResource() == null)
                throw new IllegalStateException(
                        "OnlineResource for remote WFS not specified in SLD");
            final FeatureTypeConstraint[] featureConstraints = ul.getLayerFeatureConstraints();
            if (featureConstraints == null || featureConstraints.length == 0)
                throw new IllegalStateException(
                        "No FeatureTypeConstraint specified, no layer can be loaded for this UserStyle");

            DataStore remoteWFS = null;
            List remoteTypeNames = null;
            try {
                URL url = new URL(service.getOnlineResource());
                remoteWFS = connectRemoteWFS(url);
                remoteTypeNames = new ArrayList(Arrays.asList(remoteWFS.getTypeNames()));
                Collections.sort(remoteTypeNames);

            } catch (MalformedURLException e) {
                throw new IllegalStateException(
                        "Invalid online resource url: '" + service.getOnlineResource() + "'");
            }

            List<LayerInfo> layers = new ArrayList<>();
            Style[] layerStyles = ul.getUserStyles();

            for (int i = 0; i < featureConstraints.length; i++) {
                // make sure the layer is there
                String name = featureConstraints[i].getFeatureTypeName();
                if (Collections.binarySearch(remoteTypeNames, name) < 0) {
                    throw new IllegalStateException(
                            "Could not find layer feature type '"
                                    + name
                                    + "' on remote WFS '"
                                    + service.getOnlineResource());
                }
                layers.add(getLayerFromFeatureSource(remoteWFS.getFeatureSource(name)));
            }
            return layers;
        } catch (IOException e) {
            throw new ServiceException("Error accessing remote layers", e, "RemoteAccessFailed");
        }
    }

    /**
     * Constructs a {@link WFSDataStore} from an OWS URL.
     *
     * @throws ServiceException if there was a problem accessing the remote service
     */
    protected static DataStore connectRemoteWFS(URL remoteOwsUrl) {
        try {
            WFSDataStoreFactory storeFactory = new WFSDataStoreFactory();
            Map params = new HashMap(storeFactory.getImplementationHints());
            params.put(
                    WFSDataStoreFactory.URL.key,
                    remoteOwsUrl + "&request=GetCapabilities&service=WFS");
            params.put(WFSDataStoreFactory.TRY_GZIP.key, Boolean.TRUE);
            DataStore dataStore = storeFactory.createDataStore(params);

            return dataStore;
        } catch (Exception e) {
            throw new ServiceException("Could not connect to remote OWS", e, "RemoteOWSFailure");
        }
    }

    /**
     * Constructs a {@link MemoryDataStore} from an inline feature specifies in a {@link UserLayer},
     * and wraps it in a {@link LayerInfo}
     *
     * @param fallbackCrs {@link CoordinateReferenceSystem} to fall back to in case one is not
     *     specified in the inline feature definition.
     * @return The layer
     */
    protected LayerInfo getInlineFeatureLayer(UserLayer ul, CoordinateReferenceSystem fallbackCrs)
            throws SchemaException, IOException {

        SimpleFeatureSource featureSource;

        // TODO: Move back to WFS
        // what if they didn't put an "srsName" on their geometry in their
        // inlinefeature?
        // I guess we should assume they mean their geometry to exist in the
        // output SRS of the
        // request they're making.
        if (ul.getInlineFeatureType().getCoordinateReferenceSystem() == null) {
            LOGGER.warning(
                    "No CRS set on inline features default geometry.  Assuming the requestor has their inlinefeatures in the boundingbox CRS.");

            SimpleFeatureType currFt = ul.getInlineFeatureType();
            Query q = new Query(currFt.getTypeName(), Filter.INCLUDE);
            FeatureReader<SimpleFeatureType, SimpleFeature> ilReader;
            DataStore inlineFeatureDatastore = ul.getInlineFeatureDatastore();
            ilReader = inlineFeatureDatastore.getFeatureReader(q, Transaction.AUTO_COMMIT);
            CoordinateReferenceSystem crs =
                    (fallbackCrs == null) ? DefaultGeographicCRS.WGS84 : fallbackCrs;
            String typeName = inlineFeatureDatastore.getTypeNames()[0];
            MemoryDataStore reTypedDS =
                    new MemoryDataStore(new ForceCoordinateSystemFeatureReader(ilReader, crs));

            featureSource = reTypedDS.getFeatureSource(typeName);
        } else {
            DataStore inlineFeatureDatastore = ul.getInlineFeatureDatastore();
            String typeName = inlineFeatureDatastore.getTypeNames()[0];
            featureSource = inlineFeatureDatastore.getFeatureSource(typeName);
        }
        return getLayerFromFeatureSource(featureSource);
    }

    /**
     * Wraps a {@link FeatureSource} in a {@link LayerInfo} containing a {@link FeatureTypeInfo}.
     *
     * @param featureSource the feature source to be wrapped
     * @return The wrapping layer
     */
    protected LayerInfo getLayerFromFeatureSource(final FeatureSource featureSource) {
        // TODO: Wrap info from GeoTools {@link FeatureSource#getInfo()} for GetFeatureInfo, etc.
        FeatureTypeInfoImpl featureTypeInfo = null;
        try {
            featureTypeInfo = new FeatureSourceWrappingFeatureTypeInfoImpl(featureSource);
        } catch (IOException | TransformException | FactoryException e) {
            throw new IllegalStateException("Error constructing wrapping feature source", e);
        }
        featureTypeInfo.setName(featureSource.getName().getLocalPart());
        featureTypeInfo.setEnabled(true);
        featureTypeInfo.setCatalog(catalog);
        LayerInfo layerInfo = catalog.getFactory().createLayer();
        layerInfo.setResource(featureTypeInfo);
        layerInfo.setEnabled(true);
        // CollectionFeatureSource doesn't support getDataStore
        if (!(featureSource instanceof CollectionFeatureSource)
                && featureSource.getDataStore() instanceof WFSDataStore) {
            layerInfo.setType(PublishedType.REMOTE);
        } else {
            layerInfo.setType(PublishedType.VECTOR);
        }

        return layerInfo;
    }

    protected static class FeatureSourceWrappingFeatureTypeInfoImpl extends FeatureTypeInfoImpl {
        FeatureSource featureSource;

        public FeatureSourceWrappingFeatureTypeInfoImpl(FeatureSource featureSource)
                throws IOException, TransformException, FactoryException {
            super();
            this.featureSource = featureSource;
            setName(featureSource.getName().getLocalPart());
            setEnabled(true);
            setLatLonBoundingBox(
                    featureSource.getBounds().transform(DefaultGeographicCRS.WGS84, true));
        }

        @Override
        public FeatureSource getFeatureSource(ProgressListener listener, Hints hints) {
            return featureSource;
        }

        @Override
        public FeatureType getFeatureType() throws IOException {
            return featureSource.getSchema();
        }

        @Override
        public Name getQualifiedName() {
            return featureSource.getName();
        }

        @Override
        public String prefixedName() {
            return featureSource.getName().getNamespaceURI() + ":" + getName();
        }

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public DataStoreInfo getStore() {
            return new DataStoreInfoImpl() {
                @Override
                public DataAccess<? extends FeatureType, ? extends Feature> getDataStore(
                        ProgressListener listener) throws IOException {
                    return DataUtilities.dataStore((SimpleFeatureSource) featureSource);
                }
            };
        }
    }

    protected static class StyleWrappingStyleInfoImpl extends StyleInfoImpl {
        Style style;

        public StyleWrappingStyleInfoImpl(Style style) {
            super();
            this.style = style;
            setName(style.getName());
        }

        @Override
        public Style getStyle() {
            return style;
        }
    }
}
