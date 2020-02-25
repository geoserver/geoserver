/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import static org.geoserver.ows.URLMangler.URLType.SERVICE;
import static org.geoserver.wfs3.response.ConformanceDocument.CORE;
import static org.geoserver.wfs3.response.ConformanceDocument.GEOJSON;
import static org.geoserver.wfs3.response.ConformanceDocument.GMLSF0;
import static org.geoserver.wfs3.response.ConformanceDocument.OAS30;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.StoredQueryProvider;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs3.response.CollectionDocument;
import org.geoserver.wfs3.response.CollectionsDocument;
import org.geoserver.wfs3.response.ConformanceDocument;
import org.geoserver.wfs3.response.LandingPageDocument;
import org.geoserver.wfs3.response.Link;
import org.geoserver.wfs3.response.StyleDocument;
import org.geoserver.wfs3.response.StylesDocument;
import org.geoserver.wfs3.response.TilingSchemeDescriptionDocument;
import org.geoserver.wfs3.response.TilingSchemesDocument;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.DefaultGridsets;
import org.opengis.filter.FilterFactory2;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/** WFS 3.0 implementation */
public class DefaultWebFeatureService30 implements WebFeatureService30, ApplicationContextAware {
    private static final Logger LOGGER = Logging.getLogger(DefaultWebFeatureService30.class);

    private final GeoServerDataDirectory dataDirectory;
    private FilterFactory2 filterFactory;
    private final GeoServer geoServer;
    private final DefaultGridsets gridSets;
    private List<WFS3Extension> extensions;

    public DefaultWebFeatureService30(GeoServer geoServer, DefaultGridsets gridSets) {
        this.geoServer = geoServer;
        this.gridSets = gridSets;
        this.dataDirectory = new GeoServerDataDirectory(geoServer.getCatalog().getResourceLoader());
    }

    public FilterFactory2 getFilterFactory() {
        return filterFactory;
    }

    public void setFilterFactory(FilterFactory2 filterFactory) {
        this.filterFactory = filterFactory;
    }

    @Override
    public LandingPageDocument landingPage(LandingPageRequest request) {
        LandingPageDocument contents = new LandingPageDocument(request, getService(), getCatalog());
        return contents;
    }

    @Override
    public CollectionsDocument collections(CollectionsRequest request) {
        return new CollectionsDocument(request, geoServer, extensions);
    }

    @Override
    public CollectionDocument collection(CollectionRequest request) {
        // single collection
        QName typeName = request.getTypeName();
        NamespaceInfo ns = getCatalog().getNamespaceByURI(typeName.getNamespaceURI());
        FeatureTypeInfo featureType =
                getCatalog().getFeatureTypeByName(ns, typeName.getLocalPart());
        if (featureType == null) {
            throw new ServiceException(
                    "Unknown collection " + typeName,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "typeName");
        } else {
            CollectionsDocument collections =
                    new CollectionsDocument(request, geoServer, featureType, extensions);
            CollectionDocument collection = collections.getCollections().next();

            return collection;
        }
    }

    @Override
    public ConformanceDocument conformance(ConformanceRequest request) {
        List<String> classes = Arrays.asList(CORE, OAS30, GEOJSON, GMLSF0);
        return new ConformanceDocument(classes);
    }

    private Catalog getCatalog() {
        return geoServer.getCatalog();
    }

    private WFSInfo getService() {
        return geoServer.getService(WFSInfo.class);
    }

    @Override
    public FeatureCollectionResponse getFeature(org.geoserver.wfs3.GetFeatureType request) {
        // If the server has any more results available than it returns (the number it returns is
        // less than or equal to the requested/default/maximum limit) then the server will include a
        // link to the next set of results.
        // This will make paging in WFS3 slower, as it always introduces sorting
        if (request.getStartIndex() == null && request.getCount() != null) {
            request.setStartIndex(BigInteger.ZERO);
        }

        WFS3GetFeature gf = new WFS3GetFeature(getService(), getCatalog());
        gf.setFilterFactory(filterFactory);
        gf.setStoredQueryProvider(getStoredQueryProvider());
        FeatureCollectionResponse response = gf.run(new GetFeatureRequest.WFS20(request));

        return response;
    }

    private StoredQueryProvider getStoredQueryProvider() {
        return new StoredQueryProvider(getCatalog());
    }

    /**
     * Returns a selection of supported formats for a given response object
     *
     * <p>TODO: this should be moved in a more central place, as it's of general utility (maybe the
     * filtering part could be made customizable via a lambda)
     *
     * @return A list of MIME types
     */
    public static List<String> getAvailableFormats(Class responseType) {
        Set<String> formatNames = new LinkedHashSet<>();
        Collection responses = GeoServerExtensions.extensions(Response.class);
        for (Iterator i = responses.iterator(); i.hasNext(); ) {
            Response format = (Response) i.next();
            if (!responseType.isAssignableFrom(format.getBinding())) {
                continue;
            }
            if (format instanceof WFSGetFeatureOutputFormat
                    && !((WFSGetFeatureOutputFormat) format).canHandle(WebFeatureService30.V3)) {
                continue;
            }
            // TODO: get better collaboration from content
            Set<String> formats = format.getOutputFormats();
            if (formats.isEmpty()) {
                continue;
            }
            // try to get a MIME type, otherwise pick the first available
            formats.stream().filter(f -> f.contains("/")).forEach(f -> formatNames.add(f));
        }
        return new ArrayList<>(formatNames);
    }

    @Override
    public OpenAPI api(APIRequest request) {
        return new OpenAPIBuilder().build(request, getService(), extensions);
    }

    @Override
    public TilingSchemesDocument tilingSchemes(TilingSchemesRequest request) {
        return new TilingSchemesDocument(gridSets);
    }

    @Override
    public FeatureCollectionResponse getTile(org.geoserver.wfs3.GetFeatureType request) {

        WFS3GetFeature gf = new WFS3GetFeature(getService(), getCatalog());
        gf.setFilterFactory(filterFactory);
        gf.setStoredQueryProvider(getStoredQueryProvider());
        FeatureCollectionResponse response = gf.run(new GetFeatureRequest.WFS20(request));

        return response;
    }

    @Override
    public TilingSchemeDescriptionDocument describeTilingScheme(
            TilingSchemeDescriptionRequest request) {
        return new TilingSchemeDescriptionDocument(request.getGridSet());
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        extensions = GeoServerExtensions.extensions(WFS3Extension.class, context);
    }

    @Override
    public void postStyles(
            HttpServletRequest request, HttpServletResponse response, PostStyleRequest post)
            throws IOException {
        final String mimeType = request.getContentType();
        final StyleHandler handler = Styles.handler(mimeType);
        if (handler == null) {
            throw new HttpErrorCodeException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                    "Cannot handle a style of type " + mimeType);
        }
        final Catalog catalog = getCatalog();
        final String styleBody = IOUtils.toString(request.getReader());
        final StyledLayerDescriptor sld =
                handler.parse(
                        styleBody,
                        handler.versionForMimeType(mimeType),
                        dataDirectory.getResourceLocator(),
                        catalog.getResourcePool().getEntityResolver());
        String name = getStyleName(sld);
        if (name == null) {
            throw new HttpErrorCodeException(
                    HttpStatus.BAD_REQUEST.value(), "Style does not have a name!");
        }

        final WorkspaceInfo wsInfo = LocalWorkspace.get();
        if (catalog.getStyleByName(wsInfo, name) != null) {
            throw new HttpErrorCodeException(
                    HttpStatus.BAD_REQUEST.value(), "Style already exists!");
        }

        StyleInfo sinfo = catalog.getFactory().createStyle();
        sinfo.setName(name);
        sinfo.setFilename(name + "." + handler.getFileExtension());
        sinfo.setFormat(handler.getFormat());
        sinfo.setFormatVersion(handler.versionForMimeType(mimeType));
        if (wsInfo != null) {
            sinfo.setWorkspace(wsInfo);
        }

        try {
            catalog.getResourcePool()
                    .writeStyle(sinfo, new ByteArrayInputStream(styleBody.getBytes()));
        } catch (Exception e) {
            throw new HttpErrorCodeException(INTERNAL_SERVER_ERROR.value(), "Error writing style");
        }

        catalog.add(sinfo);

        // do we need to associate with a layer?
        LayerInfo layer = null;
        if (post.getLayerName() != null) {
            // layer existence already checked in WFS3Filter
            layer = getCatalog().getLayerByName(post.getLayerName());
            layer.getStyles().add(sinfo);
            getCatalog().save(layer);
        }

        // build and return the new path
        ResponseUtils.appendPath(request.getContextPath());
        final String baseURL = ResponseUtils.baseURL(request);
        final String path =
                layer == null
                        ? "wfs3/styles/" + name
                        : "wfs3/collections/"
                                + NCNameResourceCodec.encode(layer.getResource())
                                + "/styles/"
                                + name;
        // URLType.SERVICE is important here, otherwise no ws localization
        final String url = ResponseUtils.buildURL(baseURL, path, null, SERVICE);

        response.setStatus(HttpStatus.CREATED.value());
        response.addHeader(HttpHeaders.LOCATION, url);
    }

    private String getStyleName(StyledLayerDescriptor sld) {
        String name = sld.getName();
        if (name == null) {
            Style style = Styles.style(sld);
            name = style.getName();
        }
        return name;
    }

    @Override
    public StylesDocument getStyles(GetStylesRequest request) throws IOException {
        List<StyleDocument> styles = new ArrayList<>();

        if (request.getLayerName() == null) {
            // return only styles that are not associated to a layer, those will show up
            // in the layer association instead
            final Set<StyleInfo> layerAssociatedStyles = getLayerAssociatedStyles();
            addBuiltInStyles(layerAssociatedStyles);
            for (StyleInfo style : getCatalog().getStyles()) {
                if (layerAssociatedStyles.contains(style)) {
                    continue;
                }
                addStyleDocument(request, styles, style);
            }
        } else {
            final LayerInfo layer = getCatalog().getLayerByName(request.getLayerName());
            if (layer.getDefaultStyle() != null) {
                addStyleDocument(request, styles, layer.getDefaultStyle());
            }
            if (layer.getStyles() != null) {
                for (StyleInfo style : layer.getStyles()) {
                    addStyleDocument(request, styles, style);
                }
            }
        }

        return new StylesDocument(styles);
    }

    private void addStyleDocument(
            GetStylesRequest request, List<StyleDocument> styles, StyleInfo style) {
        try {
            StyleDocument sd = buildStyleDocument(request, style);
            styles.add(sd);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Failed to process style " + style.getName(), e);
        }
    }

    private StyleDocument buildStyleDocument(GetStylesRequest request, StyleInfo style) {
        StyleDocument sd = StyleDocument.build(style);
        String styleFormat = style.getFormat();
        if (styleFormat == null) {
            styleFormat = "SLD";
        }
        // canonicalize
        styleFormat = Styles.handler(styleFormat).mimeType(null);
        // add link for native format
        sd.addLink(buildLink(request, sd, styleFormat));
        if (!SLDHandler.MIMETYPE_10.equalsIgnoreCase(styleFormat)) {
            // add link for SLD 1.0 translation
            sd.addLink(buildLink(request, sd, SLDHandler.MIMETYPE_10));
        }
        return sd;
    }

    private void addBuiltInStyles(Set<StyleInfo> blacklist) {
        accumulateStyle(blacklist, getCatalog().getStyleByName(StyleInfo.DEFAULT_POINT));
        accumulateStyle(blacklist, getCatalog().getStyleByName(StyleInfo.DEFAULT_LINE));
        accumulateStyle(blacklist, getCatalog().getStyleByName(StyleInfo.DEFAULT_POLYGON));
        accumulateStyle(blacklist, getCatalog().getStyleByName(StyleInfo.DEFAULT_GENERIC));
        accumulateStyle(blacklist, getCatalog().getStyleByName(StyleInfo.DEFAULT_RASTER));
    }

    private Link buildLink(GetStylesRequest request, StyleDocument sd, String styleFormat) {
        String path;
        if (request.getLayerName() != null) {
            FeatureTypeInfo featureType = getCatalog().getFeatureTypeByName(request.getLayerName());
            String collectionId = NCNameResourceCodec.encode(featureType);
            path = "wfs3/collections/" + collectionId + "/styles/" + sd.getId();
        } else {
            path = "wfs3/styles/" + sd.getId();
        }
        String href =
                ResponseUtils.buildURL(
                        request.getBaseUrl(),
                        path,
                        Collections.singletonMap("f", styleFormat),
                        SERVICE);
        return new Link(href, "style", styleFormat, null);
    }

    /** Returns a list of styles that are not associated with any layer */
    private Set<StyleInfo> getLayerAssociatedStyles() {
        Set<StyleInfo> result = new HashSet<>();
        for (LayerInfo layer : getCatalog().getLayers()) {
            accumulateStyle(result, layer.getDefaultStyle());
            if (layer.getStyles() != null) {
                for (StyleInfo style : layer.getStyles()) {
                    accumulateStyle(result, style);
                }
            }
        }

        return result;
    }

    private void accumulateStyle(Set<StyleInfo> result, StyleInfo style) {
        if (style != null) {
            result.add(style);
        }
    }

    @Override
    public StyleInfo getStyle(GetStyleRequest request) throws IOException {
        StyleInfo style = null;
        LayerInfo layer = null;
        if (request.getLayerName() != null) {
            // layer existence already checked in WFSFilter
            layer = getCatalog().getLayerByName(request.getLayerName());
            if (layer.getDefaultStyle() != null
                    && layer.getDefaultStyle().getName().equals(request.getStyleId())) {
                style = layer.getDefaultStyle();
            } else {
                Predicate<StyleInfo> styleFilter =
                        s -> s != null && s.getName().equalsIgnoreCase(request.getStyleId());
                Optional<StyleInfo> first =
                        layer.getStyles().stream().filter(styleFilter).findFirst();
                if (first.isPresent()) {
                    style = first.get();
                }
            }
        } else {
            style = getCatalog().getStyleByName(request.getStyleId());
        }
        if (style == null) {
            String message = "Style " + request.getStyleId() + " could not be found";
            if (layer != null) {
                message += " in collection " + NCNameResourceCodec.encode(layer.getResource());
            }
            throw new HttpErrorCodeException(NOT_FOUND.value(), message);
        }

        return style;
    }

    @Override
    public void putStyle(
            HttpServletRequest request, HttpServletResponse response, PutStyleRequest putStyle)
            throws IOException {
        final String mimeType = request.getContentType();
        final StyleHandler handler = Styles.handler(mimeType);
        if (handler == null) {
            throw new HttpErrorCodeException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                    "Cannot handle a style of type " + mimeType);
        }
        final Catalog catalog = getCatalog();
        final String styleBody = IOUtils.toString(request.getReader());

        final WorkspaceInfo wsInfo = LocalWorkspace.get();
        String styleName = putStyle.getStyleId();
        StyleInfo sinfo = catalog.getStyleByName(wsInfo, styleName);
        boolean newStyle = sinfo == null;
        if (newStyle) {
            sinfo = catalog.getFactory().createStyle();
            sinfo.setName(styleName);
            sinfo.setFilename(styleName + "." + handler.getFileExtension());
        }

        sinfo.setFormat(handler.getFormat());
        sinfo.setFormatVersion(handler.versionForMimeType(mimeType));
        if (wsInfo != null) {
            sinfo.setWorkspace(wsInfo);
        }

        try {
            catalog.getResourcePool()
                    .writeStyle(sinfo, new ByteArrayInputStream(styleBody.getBytes()));
        } catch (Exception e) {
            throw new HttpErrorCodeException(INTERNAL_SERVER_ERROR.value(), "Error writing style");
        }

        if (newStyle) {
            catalog.add(sinfo);
        } else {
            catalog.save(sinfo);
        }

        final String layerName = putStyle.getLayerName();
        if (layerName != null) {
            final LayerInfo layer = catalog.getLayerByName(layerName);
            if (!layer.getStyles()
                    .stream()
                    .anyMatch(s -> s != null && s.getName().equalsIgnoreCase(styleName))) {
                layer.getStyles().add(sinfo);
                catalog.save(layer);
            }
        }

        response.setStatus(NO_CONTENT.value());
    }

    @Override
    public void deleteStyle(DeleteStyleRequest request, HttpServletResponse response)
            throws IOException {
        String name = request.getStyleId();
        WorkspaceInfo ws = LocalWorkspace.get();
        final Catalog catalog = getCatalog();
        StyleInfo sinfo = catalog.getStyleByName(ws, name);
        if (sinfo == null) {
            throw new HttpErrorCodeException(
                    NOT_FOUND.value(), "Could not find style with id: " + name);
        }

        if (request.getLayerName() != null) {
            LayerInfo layer = catalog.getLayerByName(request.getLayerName());
            if (sinfo.equals(layer.getDefaultStyle())) {
                StyleInfo newDefault =
                        new CatalogBuilder(catalog).getDefaultStyle(layer.getResource());
                layer.setDefaultStyle(newDefault);
            } else {
                if (!(layer.getStyles().remove(sinfo))) {
                    throw new HttpErrorCodeException(
                            NOT_FOUND.value(),
                            "Style with id: "
                                    + name
                                    + " is not associated to collection "
                                    + NCNameResourceCodec.encode(layer.getResource()));
                }
            }
            catalog.save(layer);
        }

        catalog.remove(sinfo);

        response.setStatus(NO_CONTENT.value());
    }
}
