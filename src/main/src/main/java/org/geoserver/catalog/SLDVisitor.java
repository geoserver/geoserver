package org.geoserver.catalog;

import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.platform.ServiceException;
import org.geotools.data.*;
import org.geotools.data.crs.ForceCoordinateSystemFeatureReader;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.factory.Hints;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.*;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * Visitor for standalone {@link org.geotools.styling.StyledLayerDescriptor}s
 *
 * Visits each {@link StyledLayer}s of an SLD.
 * Resolves LayerGroups, Remote OWS references, and inline features into individual layers
 * Visits each {@link Style} in each{@link StyledLayer}, alongside each of these resolved layers
 *
 * Intended to provide a definitive, extensible approach to parsing standalone
 * {@link org.geotools.styling.StyledLayerDescriptor}s, including style groups and external SLDs.
 */
public abstract class SLDVisitor {

    protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.catalog");

    protected final Catalog catalog;
    protected final CoordinateReferenceSystem fallbackCrs;

    public SLDVisitor(Catalog catalog, CoordinateReferenceSystem fallbackCrs) {
        this.catalog = catalog;
        this.fallbackCrs = fallbackCrs;
    }

    /**
     * Called on each named layer in the style
     *
     * Implementations should resolve and return the corresponding PublishedInfo
     *
     * @param namedLayer The named layer
     * @return The resolved catalog layer
     */
    public abstract PublishedInfo visitNamedLayer(StyledLayer namedLayer);

    /**
     * Called on each remote OWS user layer in the style
     *
     * @param userLayer The user layer
     * @param layerInfos The layers representing the feature sources in the remote OWS
     */
    public abstract void visitUserLayerRemoteOWS(UserLayer userLayer, List<LayerInfo> layerInfos);

    /**
     * Called on each inline feature user layer in the style
     *
     * @param userLayer The user layer
     * @param info The layer representing the inline feature
     */
    public abstract void visitUserLayerInlineFeature(UserLayer userLayer, LayerInfo info);

    /**
     * Called on each named style for each styled layer. In cases where the styled layer references multiple layers,
     * such as a remote OWS user layer exposing multiple layers, this will be called for once of each of these layers,
     * which will be passed in through the info argument.
     *
     * Implementations may resolve and return the corresponding Style
     *
     * @param layer The styled layer containing this style
     * @param namedStyle The named style
     * @param info Layer that the style is paired with
     * @return The resolved catalog style
     */
    public abstract Style visitNamedStyle(StyledLayer layer, NamedStyle namedStyle, LayerInfo info);

    /**
     * Called on each named style for each styled layer. In cases where the styled layer references multiple layers,
     * such as a named layer referencing a layer group, or remote OWS user layer exposing multiple layers, this will
     * be called for once of each of these layers, which will be passed in through the info argument.
     *
     * Note: currently, in the case of a named layer referencing a layer group, any styles defined in the SLD will be
     * ignored, and the layer groups defined styles will be used instead, being handled as user layers.
     *
     * @param layer The styled layer containing this style
     * @param userStyle The user style
     * @param info Layer that the style is paired with
     * @throws IOException
     */
    public abstract void visitUserStyle(StyledLayer layer, Style userStyle, LayerInfo info) throws IOException;

    /**
     * Apply the visitor to a SLD
     *
     * Visit each layer.
     * Construct temporary stores for inline features and remote OWS layers.
     * After visiting a layer, visit each style in that layer. In cases where the styled layer references multiple
     * layers, such as a named layer referencing a layer group, or remote OWS user layer exposing multiple layers, visit
     * each style for each of these sublayers instead.
     *
     * @param sld The sld the visitor is applied to
     * @throws ServiceException If the SLD document is invalid
     */
    public void apply(StyledLayerDescriptor sld) throws IOException {
        final StyledLayer[] styledLayers = sld.getStyledLayers();
        final int slCount = styledLayers.length;

        if (slCount == 0) {
            throw new ServiceException("SLD document contains no layers");
        }

        String layerName;
        Style[] layerStyles = null;

        for (StyledLayer sl : styledLayers) {
            layerName = sl.getName();
            PublishedInfo info = null;

            if (null == layerName) {
                throw new ServiceException("A UserLayer or NamedLayer without layer name was passed");
            }

            if (sl instanceof NamedLayer) {
                NamedLayer nl = (NamedLayer) sl;
                info = visitNamedLayer(nl);
                layerStyles = nl.getStyles();

            } else if (sl instanceof UserLayer) {
                UserLayer ul = (UserLayer) sl;
                if (ul.getRemoteOWS() != null) {
                    List<LayerInfo> layers = getRemoteLayersFromUserLayer((UserLayer)sl);
                    visitUserLayerRemoteOWS(ul, layers);

                    //UserLayer - Remote OWS: Apply each style to each layer
                    for (LayerInfo layer : layers) {
                        for (Style s : layerStyles) {
                            if (s instanceof NamedStyle) {
                                visitNamedStyle(sl, (NamedStyle) s, layer);
                            } else {
                                visitUserStyle(sl, s, layer);
                            }
                        }
                    }
                    //We've already handled the styles, don't need to do anything more
                    continue;

                } else if (ul.getInlineFeatureDatastore() != null) {
                    try {
                        info = getInlineFeatureLayer(ul, fallbackCrs);
                    } catch (SchemaException e) {
                        throw new ServiceException(e);
                    }
                    visitUserLayerInlineFeature(ul, (LayerInfo) info);
                } else {
                    //TODO: By the SLD spec, we shouldn't be supporting UserLayers as NamedLayers, but we do anyways.
                    info = visitNamedLayer(ul);
                }
                layerStyles = ul.getUserStyles();
            }
            // handle no styles -- use default
            if ((layerStyles == null) || (layerStyles.length == 0)) {
                if (info != null && info instanceof LayerInfo) {
                    StyleInfo styleInfo = ((LayerInfo) info).getDefaultStyle();
                    if (styleInfo != null) {
                        layerStyles = new Style[]{styleInfo.getStyle()};
                    }
                }
            }
            //NamedLayer - LayerGroup: ignore any defined styles and use the layer group instead
            if (info != null && info instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) info;

                LayerGroupHelper layerGroupHelper = new LayerGroupHelper(lg);
                List<LayerInfo> layers = lg.layers();
                List<StyleInfo> styles = lg.styles();

                for (int i = 0; i < layers.size(); i++) {
                    StyleInfo style = styles.get(i);
                    if (style == null) {
                        visitUserStyle(sl, layers.get(i).getDefaultStyle().getStyle(), layers.get(i));
                    } else {
                        visitUserStyle(sl, styles.get(i).getStyle(), layers.get(i));
                    }
                }
            //Otherwise, just a single layer; apply each style
            } else {
                for (Style s : layerStyles) {
                    if (s instanceof NamedStyle) {
                        visitNamedStyle(sl, (NamedStyle) s, (LayerInfo) info);
                    } else {
                        visitUserStyle(sl, s, (LayerInfo) info);
                    }
                }
            }
        }
    }

    /**
     * Constructs a {@link DataStore} from a remote OWS specified in a {@link UserLayer},
     * and wraps each feature matching one of the supplied {@link FeatureTypeConstraint}s in a {@link LayerInfo}
     *
     * @param ul
     * @return The list of layers wrapping the exposed features
     * @throws ServiceException
     */
    protected List<LayerInfo> getRemoteLayersFromUserLayer(UserLayer ul) throws ServiceException {
        try {
            RemoteOWS service = ul.getRemoteOWS();
            if (!service.getService().equalsIgnoreCase("WFS"))
                throw new ServiceException("GeoServer only supports WFS as remoteOWS service");
            if (service.getOnlineResource() == null)
                throw new ServiceException("OnlineResource for remote WFS not specified in SLD");
            final FeatureTypeConstraint[] featureConstraints = ul.getLayerFeatureConstraints();
            if (featureConstraints == null || featureConstraints.length == 0)
                throw new ServiceException(
                        "No FeatureTypeConstraint specified, no layer can be loaded for this UserStyle");

            DataStore remoteWFS = null;
            List remoteTypeNames = null;
            try {
                URL url = new URL(service.getOnlineResource());
                remoteWFS = connectRemoteWFS(url);
                remoteTypeNames = new ArrayList(Arrays.asList(remoteWFS.getTypeNames()));
                Collections.sort(remoteTypeNames);


            } catch (MalformedURLException e) {
                throw new ServiceException("Invalid online resource url: '"
                        + service.getOnlineResource() + "'");
            }

            List<LayerInfo> layers = new ArrayList<>();
            Style[] layerStyles = ul.getUserStyles();

            for (int i = 0; i < featureConstraints.length; i++) {
                // make sure the layer is there
                String name = featureConstraints[i].getFeatureTypeName();
                if (Collections.binarySearch(remoteTypeNames, name) < 0) {
                    throw new ServiceException("Could not find layer feature type '" + name
                            + "' on remote WFS '" + service.getOnlineResource());
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
     * @param remoteOwsUrl
     * @return
     * @throws ServiceException
     */
    protected static DataStore connectRemoteWFS(URL remoteOwsUrl) throws ServiceException {
        try {
            WFSDataStoreFactory storeFactory = new WFSDataStoreFactory();
            Map params = new HashMap(storeFactory.getImplementationHints());
            params.put(WFSDataStoreFactory.URL.key, remoteOwsUrl
                    + "&request=GetCapabilities&service=WFS");
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
     * @param ul
     * @param fallbackCrs {@link CoordinateReferenceSystem} to fall back to in case one is not specified in the inline
     *                    feature definition.
     * @return The layer
     * @throws SchemaException
     * @throws IOException
     */
    protected LayerInfo getInlineFeatureLayer(UserLayer ul, CoordinateReferenceSystem fallbackCrs) throws SchemaException, IOException {

        SimpleFeatureSource featureSource;

        //TODO: Move back to WFS
        // what if they didn't put an "srsName" on their geometry in their
        // inlinefeature?
        // I guess we should assume they mean their geometry to exist in the
        // output SRS of the
        // request they're making.
        if (ul.getInlineFeatureType().getCoordinateReferenceSystem() == null) {
            LOGGER.warning("No CRS set on inline features default geometry.  Assuming the requestor has their inlinefeatures in the boundingbox CRS.");

            SimpleFeatureType currFt = ul.getInlineFeatureType();
            Query q = new Query(currFt.getTypeName(), Filter.INCLUDE);
            FeatureReader<SimpleFeatureType, SimpleFeature> ilReader;
            DataStore inlineFeatureDatastore = ul.getInlineFeatureDatastore();
            ilReader = inlineFeatureDatastore.getFeatureReader(q, Transaction.AUTO_COMMIT);
            CoordinateReferenceSystem crs = (fallbackCrs == null) ? DefaultGeographicCRS.WGS84 : fallbackCrs;
            String typeName = inlineFeatureDatastore.getTypeNames()[0];
            MemoryDataStore reTypedDS = new MemoryDataStore(new ForceCoordinateSystemFeatureReader(
                    ilReader, crs));

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
        //TODO: Wrap info from GeoTools {@link FeatureSource#getInfo()} for GetFeatureInfo, etc.
        FeatureTypeInfoImpl featureTypeInfo = new FeatureTypeInfoImpl(catalog) {
            /**
             * Override to avoid going down to the catalog and geoserver resource loader etc
             */
            @Override
            public FeatureSource getFeatureSource(ProgressListener listener, Hints hints) {
                return featureSource;
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
                        return featureSource.getDataStore();
                    }
                };
            }
        };
        featureTypeInfo.setName(featureSource.getName().getLocalPart());
        featureTypeInfo.setEnabled(true);
        LayerInfo layerInfo = catalog.getFactory().createLayer();
        layerInfo.setResource(featureTypeInfo);
        layerInfo.setEnabled(true);
        if (featureSource.getDataStore() instanceof WFSDataStore) {
            layerInfo.setType(PublishedType.REMOTE);
        } else {
            layerInfo.setType(PublishedType.VECTOR);
        }

        return layerInfo;
    }
}
