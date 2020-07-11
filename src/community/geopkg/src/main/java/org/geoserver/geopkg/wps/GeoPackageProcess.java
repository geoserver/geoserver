/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.QueryType;
import net.opengis.wfs20.Wfs20Factory;
import net.opengis.wps10.ExecuteType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.geopkg.GeoPackageGetMapOutputFormat;
import org.geoserver.geopkg.GeoPkg;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wfs.GetFeature;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.Entry;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.wps.GeoPackageProcessRequest;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.FeaturesLayer;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.Layer;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.LayerType;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.TilesLayer;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.geotools.styling.DefaultResourceLocator;
import org.geotools.styling.Description;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;

@DescribeProcess(title = "GeoPackage", description = "Geopackage Process")
public class GeoPackageProcess implements GeoServerProcess {

    static final Logger LOGGER = Logging.getLogger(GeoPackageProcess.class);
    private final GeoServerDataDirectory dataDirectory;
    private final EntityResolverProvider resolverProvider;

    private Catalog catalog;

    private WPSResourceManager resources;

    private GetFeature getFeatureDelegate;

    private GeoPackageGetMapOutputFormat mapOutput;

    private FilterFactory2 filterFactory;

    public GeoPackageProcess(
            GeoServer geoServer,
            GeoPackageGetMapOutputFormat mapOutput,
            WPSResourceManager resources,
            FilterFactory2 filterFactory,
            GeoServerDataDirectory dataDirectory,
            EntityResolverProvider resolverProvider) {
        this.resources = resources;
        this.mapOutput = mapOutput;
        this.filterFactory = filterFactory;
        this.dataDirectory = dataDirectory;
        this.resolverProvider = resolverProvider;
        catalog = geoServer.getCatalog();

        getFeatureDelegate = new GetFeature(geoServer.getService(WFSInfo.class), catalog);
        getFeatureDelegate.setFilterFactory(filterFactory);
    }

    private static final int TEMP_DIR_ATTEMPTS = 10000;

    public static File createTempDir(File baseDir) {
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                return tempDir;
            }
        }
        throw new IllegalStateException(
                "Failed to create directory within "
                        + TEMP_DIR_ATTEMPTS
                        + " attempts (tried "
                        + baseName
                        + "0 to "
                        + baseName
                        + (TEMP_DIR_ATTEMPTS - 1)
                        + ')');
    }

    @DescribeResult(name = "geopackage", description = "Link to Compiled Geopackage File")
    public URL execute(
            @DescribeParameter(
                        name = "contents",
                        description = "xml scheme describing geopackage contents"
                    )
                    GeoPackageProcessRequest contents)
            throws IOException {

        final File file;

        URL path = contents.getPath();
        boolean remove = contents.getRemove() != null ? contents.getRemove() : true;

        String outputName = contents.getName() + ".gpkg";
        if (!remove && path != null) {
            File urlToFile = URLs.urlToFile(path);
            urlToFile.mkdirs();
            file = new File(urlToFile, contents.getName() + ".gpkg");
        } else {
            file = resources.getOutputResource(null, outputName).file();
        }

        GeoPackage gpkg = GeoPkg.getGeoPackage(file);
        // Initialize the GeoPackage file in order to avoid exceptions when accessing the geoPackage
        // file
        gpkg.init();

        for (int i = 0; i < contents.getLayerCount(); i++) {
            Layer layer = contents.getLayer(i);

            if (layer.getType() == LayerType.FEATURES) {
                FeaturesLayer features = (FeaturesLayer) layer;
                QName ftName = features.getFeatureType();

                QueryType query = Wfs20Factory.eINSTANCE.createQueryType();
                query.getTypeNames().add(ftName);

                String ns =
                        ftName.getNamespaceURI() != null
                                ? ftName.getNamespaceURI()
                                : ftName.getPrefix();
                FeatureTypeInfo ft = catalog.getFeatureTypeByName(ns, ftName.getLocalPart());

                if (features.getSrs() == null) {
                    if (ft != null) {
                        try {
                            query.setSrsName(new URI(ft.getSRS()));
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    query.setSrsName(features.getSrs());
                }

                if (features.getPropertyNames() != null) {
                    query.getPropertyNames().addAll(features.getPropertyNames());
                }
                Filter filter = features.getFilter();

                // add bbox to filter if there is one
                if (features.getBbox() != null) {
                    String defaultGeometry =
                            catalog.getFeatureTypeByName(features.getFeatureType().getLocalPart())
                                    .getFeatureType()
                                    .getGeometryDescriptor()
                                    .getLocalName();

                    Envelope e = features.getBbox();
                    // HACK: because we are going through wfs 2.0, flip the coordinates (specified
                    // in xy)
                    // which will then be later flipped back to xy
                    if (query.getSrsName() != null) {
                        try {
                            CoordinateReferenceSystem crs =
                                    CRS.decode(query.getSrsName().toString());
                            if (crs instanceof GeographicCRS) {
                                // flip the bbox
                                e =
                                        new Envelope(
                                                e.getMinY(), e.getMaxY(), e.getMinX(), e.getMaxX());
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    Filter bboxFilter =
                            filterFactory.bbox(
                                    filterFactory.property(defaultGeometry),
                                    ReferencedEnvelope.reference(e));
                    if (filter == null) {
                        filter = bboxFilter;
                    } else {
                        filter = filterFactory.and(filter, bboxFilter);
                    }
                }
                query.setFilter(filter);

                GetFeatureType getFeature = Wfs20Factory.eINSTANCE.createGetFeatureType();
                getFeature.getAbstractQueryExpression().add(query);

                FeatureCollectionResponse fc =
                        getFeatureDelegate.run(GetFeatureRequest.adapt(getFeature));

                for (FeatureCollection collection : fc.getFeatures()) {
                    if (!(collection instanceof SimpleFeatureCollection)) {
                        throw new ServiceException(
                                "GeoPackage OutputFormat does not support Complex Features.");
                    }

                    FeatureEntry e = new FeatureEntry();
                    e.setTableName(layer.getName());
                    addLayerMetadata(e, features);
                    ReferencedEnvelope bounds = collection.getBounds();
                    if (features.getBbox() != null) {
                        bounds =
                                ReferencedEnvelope.reference(
                                        bounds.intersection(features.getBbox()));
                    }

                    e.setBounds(bounds);

                    gpkg.add(e, (SimpleFeatureCollection) collection);

                    if (features.isIndexed()) {
                        gpkg.createSpatialIndex(e);
                    }
                }

                List<LayerInfo> layers = catalog.getLayers(ft);
                if (features.isStyles() && layers != null && !layers.isEmpty()) {
                    LayerInfo layerInfo = layers.get(0);
                    if (layerInfo != null) {
                        addLayerStyles(gpkg, layerInfo);
                    }
                }

            } else if (layer.getType() == LayerType.TILES) {
                TilesLayer tiles = (TilesLayer) layer;
                GetMapRequest request = new GetMapRequest();

                request.setLayers(new ArrayList<MapLayerInfo>());
                for (QName layerQName : tiles.getLayers()) {
                    LayerInfo layerInfo = null;
                    if ("".equals(layerQName.getNamespaceURI())) {
                        layerInfo = catalog.getLayerByName(layerQName.getLocalPart());
                    } else {
                        layerInfo =
                                catalog.getLayerByName(
                                        new NameImpl(
                                                layerQName.getNamespaceURI(),
                                                layerQName.getLocalPart()));
                    }
                    if (layerInfo == null) {
                        throw new ServiceException("Layer not found: " + layerQName);
                    }
                    request.getLayers().add(new MapLayerInfo(layerInfo));
                }

                if (tiles.getBbox() == null) {
                    try {
                        // generate one from requests layers
                        CoordinateReferenceSystem crs =
                                tiles.getSrs() != null
                                        ? CRS.decode(tiles.getSrs().toString())
                                        : null;

                        ReferencedEnvelope bbox = null;
                        for (MapLayerInfo l : request.getLayers()) {
                            ResourceInfo r = l.getResource();
                            ReferencedEnvelope b = null;
                            if (crs != null) {
                                // transform from lat lon bbox
                                b = r.getLatLonBoundingBox().transform(crs, true);
                            } else {
                                // use native bbox
                                b = r.getNativeBoundingBox();
                                if (bbox != null) {
                                    // transform
                                    b = b.transform(bbox.getCoordinateReferenceSystem(), true);
                                }
                            }

                            if (bbox != null) {
                                bbox.include(b);
                            } else {
                                bbox = b;
                            }
                        }

                        request.setBbox(bbox);
                    } catch (Exception e) {
                        String msg = "Must specify bbox, unable to derive from requested layers";
                        throw new RuntimeException(msg, e);
                    }
                } else {
                    request.setBbox(tiles.getBbox());
                }

                if (tiles.getSrs() == null) {
                    // use srs of first layer
                    ResourceInfo r = request.getLayers().iterator().next().getResource();
                    request.setSRS(r.getSRS());
                } else {
                    request.setSRS(tiles.getSrs().toString());
                }

                // Get the request SRS defined and set is as the request CRS
                String srs = request.getSRS();
                if (srs != null && !srs.isEmpty()) {
                    try {
                        request.setCrs(CRS.decode(srs));
                    } catch (FactoryException e) {
                        throw new RuntimeException(e);
                    }
                }

                request.setBgColor(tiles.getBgColor());
                request.setTransparent(tiles.isTransparent());
                request.setStyleBody(tiles.getSldBody());
                if (tiles.getSld() != null) {
                    request.setStyleUrl(tiles.getSld().toURL());
                } else if (tiles.getSldBody() != null) {
                    request.setStyleBody(tiles.getSldBody());
                } else {
                    request.setStyles(new ArrayList<Style>());
                    if (tiles.getStyles() != null) {
                        for (String styleName : tiles.getStyles()) {
                            StyleInfo info = catalog.getStyleByName(styleName);
                            if (info != null) {
                                request.getStyles().add(info.getStyle());
                            }
                        }
                    }
                    if (request.getStyles().isEmpty()) {
                        for (MapLayerInfo layerInfo : request.getLayers()) {
                            request.getStyles().add(layerInfo.getDefaultStyle());
                        }
                    }
                }
                request.setFormat("none");
                Map formatOptions = new HashMap();
                formatOptions.put("flipy", "true");
                if (tiles.getFormat() != null) {
                    formatOptions.put("format", tiles.getFormat());
                }
                if (tiles.getCoverage() != null) {
                    if (tiles.getCoverage().getMinZoom() != null) {
                        formatOptions.put("min_zoom", tiles.getCoverage().getMinZoom());
                    }
                    if (tiles.getCoverage().getMaxZoom() != null) {
                        formatOptions.put("max_zoom", tiles.getCoverage().getMaxZoom());
                    }
                    if (tiles.getCoverage().getMinColumn() != null) {
                        formatOptions.put("min_column", tiles.getCoverage().getMinColumn());
                    }
                    if (tiles.getCoverage().getMaxColumn() != null) {
                        formatOptions.put("max_column", tiles.getCoverage().getMaxColumn());
                    }
                    if (tiles.getCoverage().getMinRow() != null) {
                        formatOptions.put("min_row", tiles.getCoverage().getMinRow());
                    }
                    if (tiles.getCoverage().getMaxRow() != null) {
                        formatOptions.put("max_row", tiles.getCoverage().getMaxRow());
                    }
                }
                if (tiles.getGridSetName() != null) {
                    formatOptions.put("gridset", tiles.getGridSetName());
                }
                request.setFormatOptions(formatOptions);

                TileEntry e = new TileEntry();
                addLayerMetadata(e, tiles);

                if (tiles.getGrids() != null) {
                    mapOutput.addTiles(gpkg, e, request, tiles.getGrids(), layer.getName());
                } else {
                    mapOutput.addTiles(gpkg, e, request, layer.getName());
                }
            }
        }

        gpkg.close();
        // Add to storage only if it is a temporary file
        if (path != null && !remove) {
            return path;
        } else {
            return new URL(resources.getOutputResourceUrl(outputName, "application/x-gpkg"));
        }
    }

    private void addLayerStyles(GeoPackage gpkg, LayerInfo layerInfo) throws IOException {
        try {
            PortrayalExtension portrayal = gpkg.getExtension(PortrayalExtension.class);
            SemanticAnnotationsExtension annotations =
                    gpkg.getExtension(SemanticAnnotationsExtension.class);

            StyleInfo defaultStyle = layerInfo.getDefaultStyle();
            GeoPkgStyle defaultGeoPkgStyle = addStyle(portrayal, annotations, defaultStyle);
            linkStyle(annotations, layerInfo, defaultGeoPkgStyle);

            for (StyleInfo style : layerInfo.getStyles()) {
                GeoPkgStyle geoPkgStyle = addStyle(portrayal, annotations, style);
                linkStyle(annotations, layerInfo, geoPkgStyle);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    private void linkStyle(
            SemanticAnnotationsExtension annotations, LayerInfo layerInfo, GeoPkgStyle style)
            throws SQLException {
        // need to link style and layer (it's not done while adding the style, cause the
        // style could have been shared among different layers, and thus, created only once)
        List<GeoPkgSemanticAnnotation> styleAnnotations =
                annotations.getAnnotationsByURI(style.getUri());
        if (styleAnnotations.size() > 0) {
            GeoPkgSemanticAnnotation annotation = styleAnnotations.get(0);
            annotations.addReference(
                    new GeoPkgAnnotationReference(
                            layerInfo.getResource().getNativeName(), annotation));
            annotations.addReference(
                    new GeoPkgAnnotationReference(
                            PortrayalExtension.STYLES_TABLE, "id", style.getId(), annotation));
        }
    }

    private GeoPkgStyle addStyle(
            PortrayalExtension portrayal, SemanticAnnotationsExtension annotations, StyleInfo style)
            throws SQLException, IOException {
        String styleName = style.prefixedName();
        String styleURI = builStyledURI(style);
        // if it's missing yet, add it (multiple layers could be referring to the same style)
        GeoPkgStyle gs = portrayal.getStyle(styleName);
        if (gs == null) {
            // grab the sld
            Resource styleResource = dataDirectory.style(style);
            String format = style.getFormat();
            StyleHandler handler = Styles.handler(format);
            File file = styleResource.file();
            DefaultResourceLocator mockLocator = new DefaultResourceLocator();
            StyledLayerDescriptor sld =
                    handler.parse(
                            file,
                            style.getFormatVersion(),
                            mockLocator,
                            resolverProvider.getEntityResolver());

            // save the style
            String description = getDescription(sld);
            gs = new GeoPkgStyle(styleName, styleURI);
            gs.setDescription(description);
            portrayal.addStyle(gs);

            // the stylesheet (for now, in a single format, multi-format support can be added later
            // easily
            try (InputStream in = styleResource.in()) {
                String body = IOUtils.toString(in, "UTF-8");
                String mimeType = handler.mimeType(style.getFormatVersion());
                GeoPkgStyleSheet styleSheet = new GeoPkgStyleSheet(gs, mimeType, body);
                portrayal.addStylesheet(styleSheet);
            }

            // now go hunt for symbology
            WorkspaceInfo ws = style.getWorkspace();
            String symbolPrefix =
                    "symbols://" + (ws != null ? ws.getName() + "_" : "") + style.getName() + "/";
            StyleResourceCollector collector =
                    new StyleResourceCollector(dataDirectory.getResourceLocator(ws), symbolPrefix);
            sld.accept(collector);
            int symbolId = 0;
            for (Map.Entry<String, GeoPkgSymbolImage> entry : collector.getResources().entrySet()) {
                String name = entry.getKey();
                GeoPkgSymbolImage image = entry.getValue();
                GeoPkgSymbol symbol = image.getSymbol();

                if (portrayal.getSymbol(symbol.getSymbol()) == null) {
                    portrayal.addSymbol(symbol);
                    portrayal.addImage(image);
                }
            }

            // create a semantic annotation for the style
            GeoPkgSemanticAnnotation annotation =
                    new GeoPkgSemanticAnnotation(
                            PortrayalExtension.SA_TYPE_STYLE, styleName, styleURI);
            annotation.setDescription(description);
            annotations.addAnnotation(annotation);
        }

        return gs;
    }

    private String getDescription(StyledLayerDescriptor sld) {
        if (StringUtils.isNotBlank(sld.getTitle())) {
            return sld.getTitle();
        } else if (StringUtils.isNotBlank(sld.getAbstract())) {
            return sld.getAbstract();
        }
        Optional<Description> description =
                Optional.ofNullable(sld.getStyledLayers())
                        .filter(layers -> layers.length > 0)
                        .map(layers -> layers[0])
                        .map(
                                l ->
                                        l instanceof UserLayer
                                                ? ((UserLayer) l).getUserStyles()
                                                : ((NamedLayer) l).getStyles())
                        .filter(styles -> styles != null && styles.length > 0)
                        .map(styles -> styles[0])
                        .map(s -> s.getDescription());

        return description
                .map(d -> d.getTitle() != null ? d.getTitle() : d.getAbstract())
                .map(t -> t.toString())
                .orElse(null);
    }

    private String builStyledURI(StyleInfo style) {
        try {
            ExecuteType request =
                    (ExecuteType) Dispatcher.REQUEST.get().getOperation().getParameters()[0];
            WorkspaceInfo ws = style.getWorkspace();
            String path = "styles/" + (ws != null ? ws.getName() + "/" : "") + style.getFilename();
            return ResponseUtils.buildURL(
                    request.getBaseUrl(),
                    path,
                    Collections.emptyMap(),
                    URLMangler.URLType.RESOURCE);
        } catch (Exception e) {
            LOGGER.log(
                    Level.INFO,
                    "Failed to build back-reference to the style, using a unique URI",
                    e);
            return style.prefixedName();
        }
    }

    private void addLayerMetadata(Entry e, Layer layer) {
        e.setDescription(layer.getDescription());
        e.setIdentifier(layer.getIdentifier());
    }
}
