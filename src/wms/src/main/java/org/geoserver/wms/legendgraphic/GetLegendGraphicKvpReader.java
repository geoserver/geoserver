/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wms.CascadedLegendRequest;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.util.NullProgressListener;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.util.URLs;
import org.geotools.util.factory.FactoryRegistryException;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.geotools.xml.styling.SLDParser;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.operation.TransformException;
import org.xml.sax.EntityResolver;

/**
 * Key/Value pair set parsed for a GetLegendGraphic request. When calling <code>getRequest</code>
 * produces a {@linkPlain org.vfny.geoserver.requests.wms.GetLegendGraphicRequest}
 *
 * <p>See {@linkplain org.org.geoserver.wms.GetLegendGraphicRequest} for a complete list of expected
 * request parameters.
 *
 * <p>This class is responsible for looking up all the required information for {@link
 * BufferedImageLegendGraphicBuilder} (titles, styles and legend graphics used). If requested
 * information (such as a legend graphic) is unavailable as described a warning will be issued
 * rather than outright failure. ALl parsed/gathered information is recorded in {@link
 * GetLegendGraphicRequest} for use by BufferedImageLegendGraphicBuilder, RasterLayerLegendHelper
 * and similar.
 *
 * @author Gabriel Roldan
 * @version $Id$
 * @see org.org.geoserver.wms.GetLegendGraphicRequest
 */
public class GetLegendGraphicKvpReader extends KvpRequestReader {

    private static final Logger LOGGER = Logging.getLogger(GetLegendGraphicKvpReader.class);

    /**
     * Factory to create styles from inline or remote SLD documents (aka, from SLD_BODY or SLD
     * parameters).
     */
    private static final StyleFactory styleFactory =
            CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());

    private WMS wms;

    /**
     * Creates a new GetLegendGraphicKvpReader object.
     *
     * @param wms WMS config object.
     */
    public GetLegendGraphicKvpReader(WMS wms) {
        super(GetLegendGraphicRequest.class);
        this.wms = wms;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public GetLegendGraphicRequest read(Object req, Map kvp, Map rawKvp) throws Exception {

        GetLegendGraphicRequest request = (GetLegendGraphicRequest) super.read(req, kvp, rawKvp);
        request.setRawKvp(rawKvp);
        request.setKvp(kvp);
        request.setWms(wms);

        if (request.getVersion() == null || request.getVersion().length() == 0) {
            String version = (String) rawKvp.get("WMTVER");
            if (version == null) {
                version = wms.getVersion();
            }
            request.setVersion(version);
        }

        final String language = (String) rawKvp.get("LANGUAGE");
        if (language != null) {
            request.setLocale(new Locale(language));
        }

        // Fix for https://osgeo-org.atlassian.net/browse/GEOS-710
        // Since at the moment none of the other request do check the version
        // numbers, we
        // disable this check for the moment, and wait for a proper fix once the
        // we support more than one version of WMS/WFS specs
        // if (!GetLegendGraphicRequest.SLD_VERSION.equals(version)) {
        // throw new WmsException("Invalid SLD version number \"" + version
        // + "\"");
        // }
        final String layer = (String) rawKvp.get("LAYER");
        final boolean strict =
                rawKvp.containsKey("STRICT")
                        ? Boolean.valueOf((String) rawKvp.get("STRICT"))
                        : request.isStrict();
        request.setStrict(strict);
        if (strict && layer == null) {
            throw new ServiceException(
                    "LAYER parameter not present for GetLegendGraphic", "LayerNotDefined");
        }
        if (strict && request.getFormat() == null) {
            throw new ServiceException(
                    "Missing FORMAT parameter for GetLegendGraphic", "MissingFormat");
        }

        // object representing the layer or layer group requested
        Object infoObject = null;

        // list of layers to render in the legend (we can have more
        // than one if a layergroup is requested)
        List<LegendRequest> layers = new ArrayList<LegendRequest>();
        if (layer != null) {
            try {
                LayerInfo layerInfo = wms.getLayerByName(layer);
                if (layerInfo != null) {
                    LegendRequest legend = null;
                    // for WMS cascaded layer
                    if (layerInfo.getResource() instanceof WMSLayerInfo)
                        legend = getCascadeLegendRequest(layerInfo, request);
                    else {
                        // layer found, fill in LegendRequest details
                        legend = addLayer(layerInfo, request);
                    }
                    legend.setLayer(layer);
                    layers.add(legend);
                    infoObject = layerInfo;
                } else {
                    // check for layer group, and add each layer
                    LayerGroupInfo layerGroupInfo = wms.getLayerGroupByName(layer);
                    if (layerGroupInfo != null) {
                        // add all single layers of the group
                        for (LayerInfo singleLayer : layerGroupInfo.layers()) {
                            LegendRequest legend = null;
                            // for WMS cascaded layer
                            if (singleLayer.getResource() instanceof WMSLayerInfo)
                                legend = getCascadeLegendRequest(singleLayer, request);
                            else legend = addLayer(singleLayer, request);

                            legend.setLayerGroupInfo(layerGroupInfo);
                            layers.add(legend);
                        }
                        infoObject = layerGroupInfo;
                    } else {
                        throw new ServiceException(layer + " layer does not exist.");
                    }
                }
            } catch (IOException e) {
                throw new ServiceException(e);
            } catch (NoSuchElementException ne) {
                throw new ServiceException(
                        new StringBuffer(layer).append(" layer does not exists.").toString(), ne);
            } catch (Exception te) {
                throw new ServiceException("Can't obtain the schema for the required layer.", te);
            }
        } else {
            // Assume this is "just" a request for a legend graphic representing for a style (no
            // infoObject for context)
            LegendRequest styleLegend = new LegendRequest();
            layers.add(styleLegend);
        }
        request.getLegends().addAll(layers);

        if (request.getFormat() == null) {
            request.setFormat(GetLegendGraphicRequest.DEFAULT_FORMAT);
        }
        if (null == wms.getLegendGraphicOutputFormat(request.getFormat())) {
            throw new ServiceException(
                    new StringBuffer("Invalid graphic format: ")
                            .append(request.getFormat())
                            .toString(),
                    "InvalidFormat");
        }

        try {
            // Parse optional parameters into legend data structure created above
            parseOptionalParameters(request, infoObject, rawKvp);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        return request;
    }

    /**
     * Create a Legend request to fetch Legend from cascaded WMS service
     *
     * @param layerInfo of Cascaded Layer, should WMSLayerInfo
     * @param request should be instance of GetLegendGraphicRequest
     * @return GetCascadedLegendGraphicRequest
     */
    private LegendRequest getCascadeLegendRequest(
            LayerInfo layerInfo, GetLegendGraphicRequest request) throws IOException {
        WMSLayerInfo wmsLayerInfo = (WMSLayerInfo) layerInfo.getResource();
        WMSStoreInfo wmsStoreInfo = wmsLayerInfo.getStore();
        WebMapServer wmsServer = wmsStoreInfo.getWebMapServer(null);

        org.geotools.ows.wms.request.GetLegendGraphicRequest remoteLegendGraphicRequest;

        if (wmsServer.getCapabilities().getVersion().equalsIgnoreCase("1.3.0")) {
            // WebMapServer under 1.3.0 version does not create GetLegendGraphicRequest
            // since the XML tag <GetLegendGraphic> is not present under the <Capability>
            // HACK HACK HACK
            // taking a 1.3.0 GetMap request and forcefully turning it into a GetLegend Request
            remoteLegendGraphicRequest =
                    new CascadedLegendRequest.GetLegendGraphicRequestV1_3_0(
                            wmsServer.createGetMapRequest().getFinalURL(), "1.3.0");
            remoteLegendGraphicRequest.toString();

        } else {
            // other than 1.3.0
            remoteLegendGraphicRequest = wmsServer.createGetLegendGraphicRequest();
        }

        // setting up remote request
        remoteLegendGraphicRequest.setLayer(wmsLayerInfo.getNativeName());

        CascadedLegendRequest legend = new CascadedLegendRequest(request);

        legend.setRemoteLegendGraphicRequest(remoteLegendGraphicRequest);
        legend.setLayer(layerInfo.getName());
        legend.setTitle(layerInfo.getTitle());
        legend.setLayerInfo(layerInfo);

        return legend;
    }

    /**
     * Creates a new layer for the current list of layers to be drawn on the legend.
     *
     * <p>Additional LayerInfo details such as title and legend are filled in if available.
     *
     * @param layerInfo The layer description
     * @param request The GetLegendGrapicRequest used for context
     * @return created LegendRequest
     */
    private LegendRequest addLayer(LayerInfo layerInfo, GetLegendGraphicRequest request)
            throws FactoryRegistryException, IOException, TransformException, SchemaException {
        FeatureType featureType = getLayerFeatureType(layerInfo);
        if (featureType != null) {
            LegendRequest legend =
                    new LegendRequest(featureType, layerInfo.getResource().getQualifiedName());
            legend.setLayerInfo(layerInfo);

            MapLayerInfo mli = new MapLayerInfo(layerInfo);
            // Temporary MapLayerInfo used to map a title, if label is defined on layer
            if (mli.getLabel() != null) {
                legend.setTitle(mli.getLabel());
            }
            LegendInfo legendInfo = resolveLegendInfo(layerInfo.getLegend(), request, null);
            if (legendInfo != null) {
                configureLegendInfo(request, legend, legendInfo);
            }
            return legend;
        } else {
            throw new ServiceException("Cannot get FeatureType for Layer", "MissingFeatureType");
        }
    }

    /**
     * Ensures the online resource is stored on the GetLegendGraphicRequest and native dimensions
     * are configured if not specified on the original request.
     *
     * @param request GetLegendGraphicRequest original KWP Request
     * @param legend LegendRequest internal Class containing references to Resources
     * @param legendInfo LegendInfo used to document use external graphic
     */
    private void configureLegendInfo(
            GetLegendGraphicRequest request, LegendRequest legend, LegendInfo legendInfo) {
        legend.setLegendInfo(legendInfo);
        if (legendInfo.getHeight() > 0 && !request.getKvp().containsKey("HEIGHT")) {
            request.setHeight(legendInfo.getHeight());
        }
        if (legendInfo.getWidth() > 0 && !request.getKvp().containsKey("WIDTH")) {
            request.setWidth(legendInfo.getWidth());
        }
    }
    /**
     * Makes a copy of the provided LegendInfo resolving ExternalGraphics reference to local file
     * system.
     *
     * <p>If external graphic reference cannot be resolved locally null is returned.
     *
     * @param legendInfo LegendInfo used to document use external graphic
     * @return Copy of provided legend info resolved to local file references.
     */
    public LegendInfo resolveLegendInfo(
            LegendInfo legendInfo, GetLegendGraphicRequest request, StyleInfo context) {
        if (legendInfo == null) {
            return null; // not available
        }
        String onlineResource = legendInfo.getOnlineResource();
        String baseUrl = request.getBaseUrl();
        if (onlineResource == null) {
            return null;
        }
        URL url = null;
        try {
            URI uri = new URI(onlineResource);
            GeoServerResourceLoader resources = wms.getCatalog().getResourceLoader();
            if (uri.isAbsolute()) {
                if (baseUrl != null && onlineResource.startsWith(baseUrl + "styles/")) {
                    // convert relative to styles durectory
                    onlineResource = onlineResource.substring(baseUrl.length() + 7);
                } else {
                    return legendInfo; // an actual external graphic reference
                }
            } else {
                // not absolute, try relative to the style if available, otherwise search
                // in the styles directory
                if (context != null) {
                    GeoServerDataDirectory dd = new GeoServerDataDirectory(resources);
                    Resource styleParentResource = dd.get(context);
                    if (styleParentResource != null
                            && styleParentResource.getType() == Resource.Type.DIRECTORY) {
                        url = URLs.fileToUrl(new File(styleParentResource.dir(), onlineResource));
                    }
                }
            }
            if (url == null) {
                File styles = resources.findOrCreateDirectory("styles");
                URL base = URLs.fileToUrl(styles);
                url = new URL(base, onlineResource);
            }
        } catch (MalformedURLException invalid) {
            LOGGER.log(Level.FINER, "Unable to resolve " + onlineResource + " locally", invalid);
            return null; // Do not try this online resource

        } catch (IOException access) {
            LOGGER.log(Level.FINER, "Unable to resolve " + onlineResource + " locally", access);
            return null; // Do not try this online resource
        } catch (URISyntaxException syntax) {
            LOGGER.log(Level.FINER, "Unable to resolve " + onlineResource + " locally", syntax);
            return null; // Do not try this online resource
        }
        LegendInfoImpl resolved = new LegendInfoImpl();
        resolved.setOnlineResource(url.toExternalForm());
        resolved.setFormat(legendInfo.getFormat());
        resolved.setHeight(legendInfo.getHeight());
        resolved.setWidth(legendInfo.getWidth());

        return resolved;
    }
    /**
     * Extracts a FeatureType for a given layer.
     *
     * <p>FeatureType obtained from catalog
     *
     * @param layerInfo vector or raster layer
     * @return the FeatureType for the given layer
     */
    private FeatureType getLayerFeatureType(LayerInfo layerInfo)
            throws IOException, FactoryRegistryException, TransformException, SchemaException {
        MapLayerInfo mli = new MapLayerInfo(layerInfo);
        if (layerInfo.getType() == PublishedType.VECTOR) {
            FeatureType featureType = mli.getFeature().getFeatureType();
            return featureType;
        } else if (layerInfo.getType() == PublishedType.RASTER) {
            CoverageInfo coverageInfo = mli.getCoverage();
            // it much safer to wrap a reader rather than a coverage in most cases, OOM can
            // occur otherwise
            final GridCoverage2DReader reader;
            reader =
                    (GridCoverage2DReader)
                            coverageInfo.getGridCoverageReader(
                                    new NullProgressListener(), GeoTools.getDefaultHints());
            final SimpleFeatureCollection feature;
            feature = FeatureUtilities.wrapGridCoverageReader(reader, null);
            return feature.getSchema();
        }
        return null;
    }

    /**
     * Parses the GetLegendGraphic optional parameters.
     *
     * <p>The parameters parsed by this method are:
     *
     * <ul>
     *   <li>FEATURETYPE for the {@link GetLegendGraphicRequest#getFeatureType() featureType}
     *       property.
     *   <li>SCALE for the {@link GetLegendGraphicRequest#getScale() scale} property.
     *   <li>WIDTH for the {@link GetLegendGraphicRequest#getWidth() width} property.
     *   <li>HEIGHT for the {@link GetLegendGraphicRequest#getHeight() height} property.
     *   <li>EXCEPTIONS for the {@link GetLegendGraphicRequest#getExceptions() exceptions} property.
     *   <li>TRANSPARENT for the {@link GetLegendGraphicRequest#isTransparent() transparent}
     *       property.
     *   <li>LEGEND_OPTIONS for the {@link GetLegendGraphicRequest#getLegendOptions() legendOptions}
     *       property.
     * </ul>
     *
     * @param req The request to set the properties to.
     * @param infoObj a {@link LayerInfo layer} or a {@link LayerGroupInfo layerGroup} for which the
     *     legend graphic is to be produced, from where to extract the style information.
     * @task TODO: validate EXCEPTIONS parameter
     */
    private void parseOptionalParameters(GetLegendGraphicRequest req, Object infoObj, Map rawKvp)
            throws IOException {
        parseStyleAndRule(req, infoObj, rawKvp);
    }

    /**
     * Parses the STYLE, SLD and SLD_BODY parameters, as well as RULE.
     *
     * <p>STYLE, SLD and SLD_BODY are mutually exclusive. STYLE refers to a named style known by the
     * server and applicable to the requested layer (i.e., it is exposed as one of the layer's
     * styles in the Capabilities document). SLD is a URL to an externally available SLD document,
     * and SLD_BODY is a string containing the SLD document itself.
     *
     * <p>As I don't completely understand which takes priority over which from the spec, I assume
     * the precedence order as follow: SLD, SLD_BODY, STYLE, in decrecent order of precedence.
     */
    private void parseStyleAndRule(GetLegendGraphicRequest req, Object infoObj, Map rawKvp)
            throws IOException {
        // gets the list of styles requested
        String listOfStyles = (String) rawKvp.get("STYLE");
        if (listOfStyles == null) {
            listOfStyles = "";
        }
        List<String> styleNames = KvpUtils.readFlat(listOfStyles);

        String sldUrl = (String) rawKvp.get("SLD");
        String sldBody = (String) rawKvp.get("SLD_BODY");

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(new StringBuffer("looking for styles ").append(listOfStyles).toString());
        }

        List<Style> sldStyles = new ArrayList<Style>();

        if (sldUrl != null) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("taking style from SLD parameter");
            }
            addStylesFrom(sldStyles, styleNames, loadRemoteStyle(sldUrl));

        } else if (sldBody != null) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("taking style from SLD_BODY parameter");
            }
            addStylesFrom(sldStyles, styleNames, parseSldBody(sldBody));

        } else if (styleNames.size() > 0) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("taking style from STYLE parameter");
            }
            int pos = 0;
            for (String styleName : styleNames) {
                // if we have a layer group and no style is specified
                // use the default one for the layer in the current position
                if (styleName.equals("") && infoObj instanceof LayerGroupInfo) {
                    LayerGroupInfo layerGroupInfo = (LayerGroupInfo) infoObj;
                    List<LayerInfo> groupLayers = layerGroupInfo.layers();
                    if (pos < groupLayers.size()) {
                        sldStyles.add(getStyleFromLayer(groupLayers.get(pos)));
                    }
                } else {
                    sldStyles.add(wms.getStyleByName(styleName));
                    if (infoObj instanceof LayerInfo) {
                        StyleInfo styleInfo = wms.getCatalog().getStyleByName(styleName);
                        if (styleInfo != null) {
                            LegendInfo legend =
                                    resolveLegendInfo(styleInfo.getLegend(), req, styleInfo);
                            if (legend != null) {
                                LayerInfo layerInfo = (LayerInfo) infoObj;
                                Name name = layerInfo.getResource().getQualifiedName();
                                LegendRequest legendRequest = req.getLegend(name);
                                if (legendRequest != null) {
                                    configureLegendInfo(req, legendRequest, legend);
                                } else {
                                    LOGGER.log(Level.FINE, "Unable to set LegendInfo for " + name);
                                }
                            }
                        }
                    }
                }
                pos++;
            }

        } else {
            if (infoObj instanceof LayerInfo) {
                LayerInfo layerInfo = (LayerInfo) infoObj;
                sldStyles.add(getStyleFromLayer(layerInfo));

                StyleInfo defaultStyle = layerInfo.getDefaultStyle();
                LegendInfo legend = resolveLegendInfo(defaultStyle.getLegend(), req, defaultStyle);
                if (legend != null) {
                    Name name = layerInfo.getResource().getQualifiedName();
                    LegendRequest legendRequest = req.getLegend(name);
                    if (legendRequest != null) {
                        configureLegendInfo(req, legendRequest, legend);
                    } else {
                        LOGGER.log(Level.FINE, "Unable to set LegendInfo for " + name);
                    }
                }
            } else if (infoObj instanceof LayerGroupInfo) {
                LayerGroupInfo layerGroupInfo = (LayerGroupInfo) infoObj;
                List<LayerInfo> groupLayers = layerGroupInfo.layers();
                List<StyleInfo> groupStyles = layerGroupInfo.styles();
                for (int count = 0; count < groupLayers.size(); count++) {
                    LayerInfo layerInfo = groupLayers.get(count);
                    StyleInfo styleInfo = null;
                    if (count < groupStyles.size() && groupStyles.get(count) != null) {
                        styleInfo = groupStyles.get(count);
                        sldStyles.add(styleInfo.getStyle());
                    } else {
                        sldStyles.add(getStyleFromLayer(layerInfo));
                        styleInfo = layerInfo.getDefaultStyle();
                    }
                    LegendInfo legend = resolveLegendInfo(styleInfo.getLegend(), req, styleInfo);
                    if (legend != null) {
                        Name name = layerInfo.getResource().getQualifiedName();
                        LegendRequest legendRequest = req.getLegend(name);
                        if (legendRequest != null) {
                            configureLegendInfo(req, legendRequest, legend);
                        } else {
                            LOGGER.log(Level.FINE, "Unable to set LegendInfo for " + name);
                        }
                    }
                }
            }
        }

        Iterator<Style> stylesIterator = sldStyles.iterator();
        for (LegendRequest legend1 : req.getLegends()) {
            if (!stylesIterator.hasNext()) {
                break; // no more styles
            }
            legend1.setStyle(stylesIterator.next());
        }

        String rule = (String) rawKvp.get("RULE");

        if (rule != null) {
            List<String> ruleNames = KvpUtils.readFlat(rule);
            Iterator<String> s = ruleNames.iterator();
            for (LegendRequest legend : req.getLegends()) {
                if (!s.hasNext()) {
                    break; // no more styles
                }
                legend.setRule(s.next());
            }
        }
    }

    /**
     * Gets the default style for the given layer
     *
     * @param layerInfo layer requested
     * @return default style of the layer
     */
    private Style getStyleFromLayer(LayerInfo layerInfo) {
        MapLayerInfo mli = new MapLayerInfo(layerInfo);
        return mli.getDefaultStyle();
    }

    /**
     * Adds styles whose name matches names from a given source of styles.
     *
     * @param sldStyles final styles container
     * @param styleNames names of styles to find in the given source
     * @param source list of styles from a given source
     */
    private void addStylesFrom(List<Style> sldStyles, List<String> styleNames, Style[] source) {
        if (styleNames.size() == 0) {
            sldStyles.add(findStyle(null, source));
        } else {
            for (String styleName : styleNames) {
                sldStyles.add(findStyle(styleName, source));
            }
        }
    }

    /**
     * Finds the Style named <code>styleName</code> in <code>styles</code>.
     *
     * @param styleName name of style to search for in the list of styles. If <code>null</code>, it
     *     is assumed the request is made in literal mode and the user has requested the first
     *     style.
     * @param styles non null, non empty, list of styles
     * @throws NoSuchElementException if no style named <code>styleName</code> is found in <code>
     *     styles</code>
     */
    private Style findStyle(String styleName, Style[] styles) throws NoSuchElementException {
        if ((styles == null) || (styles.length == 0)) {
            throw new NoSuchElementException(
                    "No styles have been provided to search for " + styleName);
        }

        if (styleName == null) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("styleName is null, request in literal mode, returning first style");
            }

            return styles[0];
        }

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer(
                    new StringBuffer("request in library mode, looking for style ")
                            .append(styleName)
                            .toString());
        }

        StringBuffer noMatchNames = new StringBuffer();

        for (int i = 0; i < styles.length; i++) {
            if ((styles[i] != null) && styleName.equals(styles[i].getName())) {
                return styles[i];
            }

            noMatchNames.append(styles[i].getName());

            if (i < styles.length) {
                noMatchNames.append(", ");
            }
        }

        throw new NoSuchElementException(
                styleName + " not found. Provided style names: " + noMatchNames);
    }

    /**
     * Loads a remote SLD document and parses it to a Style object
     *
     * @param sldUrl an URL to a SLD document
     * @return the document parsed to a Style object
     * @throws WmsException if <code>sldUrl</code> is not a valid URL, a stream can't be opened or a
     *     parsing error occurs
     */
    private Style[] loadRemoteStyle(String sldUrl) throws ServiceException {
        InputStream in;

        try {
            URL url = new URL(sldUrl);
            in = url.openStream();
        } catch (MalformedURLException e) {
            throw new ServiceException(
                    e, "Not a valid URL to an SLD document " + sldUrl, "loadRemoteStyle");
        } catch (IOException e) {
            throw new ServiceException(e, "Can't open the SLD URL " + sldUrl, "loadRemoteStyle");
        }

        return parseSld(new InputStreamReader(in));
    }

    /**
     * Parses a SLD Style from a xml string
     *
     * @param sldBody the string containing the SLD document
     * @return the SLD document string parsed to a Style object
     * @throws WmsException if a parsing error occurs.
     */
    private Style[] parseSldBody(String sldBody) throws ServiceException {
        // return parseSld(new StringBufferInputStream(sldBody));
        return parseSld(new StringReader(sldBody));
    }

    /**
     * Parses the content of the given input stream to an SLD Style, provided that a valid SLD
     * document can be read from <code>xmlIn</code>.
     *
     * @param xmlIn where to read the SLD document from.
     * @return the parsed Style
     * @throws WmsException if a parsing error occurs
     */
    private Style[] parseSld(Reader xmlIn) throws ServiceException {
        SLDParser parser = new SLDParser(styleFactory, xmlIn);
        EntityResolver entityResolver = wms.getCatalog().getResourcePool().getEntityResolver();
        if (entityResolver != null) {
            parser.setEntityResolver(entityResolver);
        }
        Style[] styles = null;

        try {
            styles = parser.readXML();
        } catch (RuntimeException e) {
            throw new ServiceException(e);
        }

        if ((styles == null) || (styles.length == 0)) {
            throw new ServiceException("Document contains no styles");
        }

        return styles;
    }
}
