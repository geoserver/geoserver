/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin.output;

import freemarker.template.Template;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.resource.Files;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wps.process.ResourceRawData;
import org.geoserver.wps.process.StreamRawData;
import org.geoserver.wps.process.StringRawData;
import org.geoserver.wps.remote.WmcFeature;
import org.geoserver.wps.remote.plugin.XMPPClient;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;

/**
 * Actual implementation of a RAW DATA Output Type
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class XMPPRawDataOutput implements XMPPOutputType {

    /** The LOGGER */
    public static final Logger LOGGER =
            Logging.getLogger(XMPPRawDataOutput.class.getPackage().getName());

    @Override
    public Object accept(
            XMPPOutputVisitor visitor,
            Object value,
            String type,
            String pID,
            String baseURL,
            XMPPClient xmppClient,
            boolean publish,
            String name,
            String title,
            String description,
            String defaultStyle,
            String targetWorkspace,
            String metadata)
            throws Exception {
        return visitor.visit(
                this,
                value,
                type,
                pID,
                baseURL,
                xmppClient,
                publish,
                name,
                title,
                description,
                defaultStyle,
                targetWorkspace,
                metadata);
    }

    @Override
    public Object produceOutput(
            Object value,
            String type,
            String pID,
            String baseURL,
            XMPPClient xmppClient,
            boolean publish,
            String name,
            String title,
            String description,
            String defaultStyle,
            String targetWorkspace,
            String metadata)
            throws Exception {

        LOGGER.finest(
                "[XMPP Raw Data Output - ProduceOutput] "
                        + value
                        + " - type:"
                        + type
                        + " - pID:"
                        + pID
                        + " - name:"
                        + name
                        + " - title:"
                        + title
                        + " - description:"
                        + description
                        + " - style:"
                        + defaultStyle
                        + " - workspace:"
                        + targetWorkspace
                        + " - metadata:"
                        + metadata);

        if (XMPPClient.PRIMITIVE_NAME_TYPE_MAP.get(type) != null) {
            Object sample = ((Object[]) XMPPClient.PRIMITIVE_NAME_TYPE_MAP.get(type))[2];

            if (sample instanceof StreamRawData) {
                final String fileName =
                        FilenameUtils.getBaseName(((String) value))
                                + "_"
                                + pID
                                + "."
                                + ((StreamRawData) sample).getFileExtension();

                LOGGER.finest("[XMPP Raw Data Output - ProduceOutput] StreamRawData:" + fileName);

                value =
                        new StreamRawData(
                                ((StreamRawData) sample).getMimeType(),
                                new FileInputStream(((String) value)),
                                ((StreamRawData) sample).getFileExtension());
                if (publish) {
                    final File tempFile = new File(FileUtils.getTempDirectory(), fileName);
                    FileUtils.copyInputStreamToFile(
                            ((StreamRawData) value).getInputStream(), tempFile);

                    try {
                        xmppClient.importLayer(
                                tempFile,
                                type,
                                null,
                                name + "_" + pID,
                                title,
                                description,
                                defaultStyle,
                                targetWorkspace,
                                metadata);
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.WARNING,
                                "There was an issue while trying to automatically publish the Layer into the Catalog!",
                                e);
                    }

                    // need to re-open the stream
                    value =
                            new StreamRawData(
                                    ((StreamRawData) sample).getMimeType(),
                                    new FileInputStream(tempFile),
                                    ((StreamRawData) sample).getFileExtension());
                }

                LOGGER.finest("[XMPP Raw Data Output - ProduceOutput] Value:" + value);

                return value;
            } else if (sample instanceof ResourceRawData) {
                final String fileName =
                        FilenameUtils.getBaseName(((String) value))
                                + "_"
                                + pID
                                + "."
                                + ((ResourceRawData) sample).getFileExtension();

                LOGGER.finest("[XMPP Raw Data Output - ProduceOutput] FileRawData:" + fileName);

                File outputFile = getOutputFile(xmppClient, (String) value);
                value =
                        new ResourceRawData(
                                Files.asResource(outputFile),
                                ((ResourceRawData) sample).getMimeType(),
                                ((ResourceRawData) sample).getFileExtension());
                if (publish) {
                    try {
                        LayerInfo layer =
                                xmppClient.importLayer(
                                        outputFile,
                                        type,
                                        null,
                                        name + "_" + pID,
                                        title,
                                        description,
                                        defaultStyle,
                                        targetWorkspace,
                                        metadata);
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.WARNING,
                                "There was an issue while trying to automatically publish the Layer into the Catalog!",
                                e);
                    }
                }

                LOGGER.finest("[XMPP Raw Data Output - ProduceOutput] Value:" + value);

                return value;
            } else if (sample instanceof StringRawData) {

                LOGGER.finest("[XMPP Raw Data Output - ProduceOutput] StringRawData:" + type);

                if (type.equals("application/owc")) {
                    value =
                            encodeAsPlainOWCMapContext(
                                    value,
                                    type,
                                    pID,
                                    baseURL,
                                    xmppClient,
                                    publish,
                                    name + "_" + pID,
                                    title,
                                    description,
                                    defaultStyle,
                                    targetWorkspace,
                                    metadata);
                } else {
                    value =
                            encodeAsPlainRawData(
                                    value,
                                    type,
                                    pID,
                                    baseURL,
                                    xmppClient,
                                    publish,
                                    name + "_" + pID,
                                    title,
                                    description,
                                    defaultStyle,
                                    targetWorkspace,
                                    metadata);
                }

                LOGGER.finest("[XMPP Raw Data Output - ProduceOutput] Value:" + value);

                return value;
            }
        }

        return null;
    }

    private File getOutputFile(XMPPClient xmppClient, String value) throws IOException {
        final File file = new File(value);
        if (file != null && file.exists() && file.canRead() && file.isFile()) {
            return file;
        } else {
            try {
                // 1. try first if they are present on "uploadedFilesBasePath"
                if (xmppClient.getConfiguration().get("uploadedFilesBasePath") != null) {
                    final String uploadedFilesBasePath =
                            xmppClient.getConfiguration().get("uploadedFilesBasePath");
                    final File uploadedFile =
                            new File(
                                    new File(uploadedFilesBasePath).getCanonicalPath(),
                                    com.google.common.io.Files.simplifyPath(value));

                    if (uploadedFile != null
                            &&
                            /*uploadedFile.isAbsolute() &&*/ uploadedFile.exists()
                            && uploadedFile.canRead()
                            && uploadedFile.isFile()) {
                        return uploadedFile;
                    }
                }

                // 2. check if the file has stored on the GeoServer Data Dir
                File uploadedFile = new File(value);
                if (uploadedFile != null
                        &&
                        /*uploadedFile.isAbsolute() &&*/ uploadedFile.exists()
                        && uploadedFile.canRead()
                        && uploadedFile.isFile()) {
                    return uploadedFile;
                }

                uploadedFile =
                        xmppClient.getGeoServer().getCatalog().getResourceLoader().find(value);
                if (uploadedFile != null
                        &&
                        /*uploadedFile.isAbsolute() &&*/ uploadedFile.exists()
                        && uploadedFile.canRead()
                        && uploadedFile.isFile()) {
                    return uploadedFile;
                }
            } catch (Exception e) {
                throw new IOException("Produced Output file is not reachable!", e);
            }
        }

        throw new IOException("Produced Output file is not accessible!");
    }

    /** */
    private Object encodeAsPlainRawData(
            Object value,
            String type,
            String pID,
            String baseURL,
            XMPPClient xmppClient,
            boolean publish,
            String name,
            String title,
            String description,
            String defaultStyle,
            String targetWorkspace,
            String metadata)
            throws IOException {
        final String extension =
                ((String) ((Object[]) XMPPClient.PRIMITIVE_NAME_TYPE_MAP.get(type))[4]);
        final String fileName = "wps-remote-str-rawdata_" + pID + extension;
        final File outputFile = getOutputFile(xmppClient, (String) value);
        final String content = FileUtils.readFileToString(outputFile);

        LOGGER.finest("[XMPP Raw Data Output - ProduceOutput] encodeAsPlainRawData:" + fileName);

        value =
                new StringRawData(
                        content,
                        ((String) ((Object[]) XMPPClient.PRIMITIVE_NAME_TYPE_MAP.get(type))[3])
                                .split(",")[0]);
        if (publish) {
            final File tempFile = new File(FileUtils.getTempDirectory(), fileName);

            LOGGER.finest(
                    "[XMPP Raw Data Output - ProduceOutput] encodeAsPlainRawData:"
                            + tempFile.getAbsolutePath());

            FileUtils.writeStringToFile(tempFile, ((StringRawData) value).getData());
            FileUtils.waitFor(tempFile, 5);

            String wsName = xmppClient.getGeoServer().getCatalog().getDefaultWorkspace().getName();
            DataStoreInfo h2DataStore =
                    xmppClient.createH2DataStore(wsName, FilenameUtils.getBaseName(fileName));

            try {
                xmppClient.importLayer(
                        tempFile,
                        type,
                        h2DataStore,
                        name + "_" + pID,
                        title,
                        description,
                        defaultStyle,
                        targetWorkspace,
                        metadata);
            } catch (Exception e) {
                LOGGER.log(
                        Level.WARNING,
                        "There was an issue while trying to automatically publish the Layer into the Catalog!",
                        e);
            }
        }
        return value;
    }

    /** */
    private Object encodeAsPlainOWCMapContext(
            Object value,
            String type,
            String pID,
            String baseURL,
            XMPPClient xmppClient,
            boolean publish,
            String name,
            String title,
            String description,
            String defaultStyle,
            String targetWorkspace,
            String metadata)
            throws IOException {
        String[] layerToPublish = ((String) value).split(";");
        String[] styles = (defaultStyle != null ? defaultStyle.split(";") : null);
        String[] workspaces = (targetWorkspace != null ? targetWorkspace.split(";") : null);

        LOGGER.finest("[XMPP Raw Data Output - ProduceOutput] encodeAsPlainOWCMapContext:" + value);

        List<LayerInfo> wmc = new ArrayList<LayerInfo>();

        if (layerToPublish != null && layerToPublish.length > 0) {
            final GeoServer geoServer = xmppClient.getGeoServer();
            final Catalog catalog = geoServer.getCatalog();

            for (int fi = 0; fi < layerToPublish.length; fi++) {
                final String layerBaseName = layerToPublish[fi];
                final String layerStyle = styles[fi];
                final String layerWorkspace = workspaces[fi];

                LOGGER.finest(
                        "[XMPP Raw Data Output - ProduceOutput] looking for LayerInfo:"
                                + layerBaseName
                                + "_"
                                + pID);

                LayerInfo layerInfo =
                        (catalog.getLayerByName(layerBaseName + "_" + pID) != null
                                ? catalog.getLayerByName(layerBaseName + "_" + pID)
                                : catalog.getLayerByName(
                                        new NameImpl(layerWorkspace, layerBaseName + "_" + pID)));

                if (layerInfo == null) {
                    LOGGER.warning(
                            "[XMPP Raw Data Output - ProduceOutput] cuold not find LayerInfo ["
                                    + layerBaseName
                                    + "_"
                                    + pID
                                    + "]... going to scan the whole Catalog!");
                    for (LayerInfo layer : catalog.getLayers()) {
                        LOGGER.info(
                                "[XMPP Raw Data Output - ProduceOutput] looking for LayerInfo:"
                                        + layer.getName());
                        if (layer.getName().contains(layerBaseName)
                                && layer.getName().contains(pID)) {
                            LOGGER.info(
                                    "[XMPP Raw Data Output - ProduceOutput] found candidate LayerInfo:"
                                            + layer.getName());
                            layerInfo = layer;
                            break;
                        }
                    }
                }

                if (layerInfo != null) {
                    LOGGER.finest(
                            "[XMPP Raw Data Output - ProduceOutput] found LayerInfo:"
                                    + layerBaseName
                                    + "_"
                                    + pID);

                    if (layerStyle.trim().length() > 0) {
                        StyleInfo style = catalog.getStyleByName(layerStyle);

                        if (style != null) {
                            layerInfo.setDefaultStyle(style);
                        }
                    }

                    if (layerWorkspace.trim().length() > 0) {
                        WorkspaceInfo workspace = catalog.getWorkspaceByName(layerWorkspace);

                        NamespaceInfo namespace =
                                catalog.getNamespace(
                                        layerInfo.getResource().getNamespace().getId());
                        if (workspace != null
                                && (!workspace.getName().equals(namespace.getPrefix())
                                        || namespace == null)) {
                            namespace = catalog.getNamespaceByPrefix(workspace.getName());
                            if (namespace != null) {
                                layerInfo.getResource().setNamespace(namespace);
                            }
                        }
                    }

                    wmc.add(layerInfo);
                    LOGGER.finest(
                            "[XMPP Raw Data Output - ProduceOutput] added LayerInfo:" + layerInfo);
                } else {
                    LOGGER.finest(
                            "[XMPP Raw Data Output - ProduceOutput] could not find any LayerInfo:"
                                    + layerBaseName
                                    + "_"
                                    + pID);
                }
            }
        }

        return getWmc(xmppClient, wmc, type, pID, baseURL, metadata);
    }

    /** */
    private Object getWmc(
            XMPPClient xmppClient,
            List<LayerInfo> wmc,
            String type,
            String pID,
            String baseURL,
            String metadata)
            throws IOException {
        final String wmcTemplatePath = xmppClient.getConfiguration().get("owc_wms_json_template");

        LOGGER.finest("[XMPP Raw Data Output - ProduceOutput] wmcTemplatePath:" + wmcTemplatePath);

        String content = "";
        if (wmcTemplatePath != null) {
            try {
                // filling the model
                final HashMap<String, Object> map = new HashMap<String, Object>();

                Map<String, Object> featureList = new HashMap<String, Object>();
                ReferencedEnvelope renderingArea = null;
                for (LayerInfo layer : wmc) {
                    WmcFeature feature = wrapFeature(xmppClient, baseURL, layer);
                    featureList.put(layer.getId(), feature);

                    if (renderingArea == null) {
                        renderingArea =
                                new ReferencedEnvelope(layer.getResource().getLatLonBoundingBox());
                    } else {
                        renderingArea.expandToInclude(layer.getResource().getLatLonBoundingBox());
                    }
                }
                map.put("owcProperties", metadata);
                map.put("featureList", featureList);
                map.put("renderingArea", bboxToJSON(renderingArea));

                // process the template and stream out the result
                content = FileUtils.readFileToString(new File(wmcTemplatePath));
                Template template =
                        new Template(
                                "name",
                                new StringReader(content),
                                TemplateUtils.getSafeConfiguration());

                template.setOutputEncoding("UTF-8");
                ByteArrayOutputStream buff = new ByteArrayOutputStream();

                template.process(map, new OutputStreamWriter(buff, Charset.forName("UTF-8")));

                content = buff.toString();

                LOGGER.finest(
                        "[XMPP Raw Data Output - ProduceOutput] wmcTemplate Processed:" + content);

            } catch (Exception e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                content = errors.toString();
            }
        }

        StringRawData value =
                new StringRawData(
                        content,
                        ((String) ((Object[]) XMPPClient.PRIMITIVE_NAME_TYPE_MAP.get(type))[3])
                                .split(",")[0]);
        return value;
    }

    /** */
    private static WmcFeature wrapFeature(XMPPClient xmppClient, String baseURL, LayerInfo layer) {
        GeoServer geoserver = xmppClient.getGeoServer();
        Catalog catalog = geoserver.getCatalog();

        WmcFeature ft = new WmcFeature();

        ft.setType(layer.getType().toString());

        ft.setName(layer.getName());
        ft.setTitle(layer.getTitle());
        ft.setDescription(layer.getAbstract());

        ft.setLayers(layer.prefixedName());
        ft.setStyles(layer.getDefaultStyle().prefixedName());
        ft.setSrs(layer.getResource().getSRS());
        ft.setBbox(bboxToString(layer.getResource().getNativeBoundingBox()));
        ft.setLatLonBbox(bboxToString(layer.getResource().getLatLonBoundingBox()));

        ft.setGeometryCoords(bboxToJSON(layer.getResource().getLatLonBoundingBox()));

        NamespaceInfo namespace = catalog.getNamespace(layer.getResource().getNamespace().getId());
        WorkspaceInfo workspace = catalog.getWorkspaceByName(namespace.getPrefix());
        ft.setWorkspace(workspace.getName());

        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        ft.setLastUpdated(sdf.format(new Date()));

        Map<String, String> kvps = new HashMap<String, String>();
        String baseUrl =
                ResponseUtils.buildURL(
                        baseURL, "/" + ft.getWorkspace() + "/ows/", kvps, URLType.SERVICE);
        ft.setGetMapBaseUrl(canonicUrl(baseUrl));

        MetadataMap layerMetadata = layer.getMetadata();
        String owcProperties =
                owcTemplate(
                        ft,
                        (String)
                                (layerMetadata.containsKey("owc_properties")
                                        ? layerMetadata.get("owc_properties")
                                        : ""));
        ft.setOwcProperties(owcProperties);

        // TODO Handle dimensions
        String test1 = layer.getResource().getStore().getType();
        MetadataMap test2 = layer.getResource().getMetadata();

        return ft;
    }

    /** */
    private static String owcTemplate(WmcFeature ft, String srcMetadata) {
        String trgMetadata = srcMetadata;

        trgMetadata = trgMetadata.replaceAll("\\$\\{type\\}", ft.getType());
        trgMetadata = trgMetadata.replaceAll("\\$\\{name\\}", ft.getName());
        trgMetadata = trgMetadata.replaceAll("\\$\\{title\\}", ft.getTitle());
        trgMetadata = trgMetadata.replaceAll("\\$\\{description\\}", ft.getDescription());
        trgMetadata = trgMetadata.replaceAll("\\$\\{lastUpdated\\}", ft.getLastUpdated());
        trgMetadata = trgMetadata.replaceAll("\\$\\{getMapBaseUrl\\}", ft.getGetMapBaseUrl());
        trgMetadata = trgMetadata.replaceAll("\\$\\{srs\\}", ft.getSrs());
        trgMetadata = trgMetadata.replaceAll("\\$\\{bbox\\}", ft.getBbox());
        trgMetadata = trgMetadata.replaceAll("\\$\\{workspace\\}", ft.getWorkspace());
        trgMetadata = trgMetadata.replaceAll("\\$\\{layers\\}", ft.getLayers());
        trgMetadata = trgMetadata.replaceAll("\\$\\{styles\\}", ft.getStyles());

        return trgMetadata;
    }

    /** @param refEnvelope */
    private static String bboxToJSON(ReferencedEnvelope refEnvelope) {
        if (refEnvelope == null) return "[[]]";
        // "[[[-2,45],[8,45],[8,55],[-2,55],[-2,45]]]"
        double minx = refEnvelope.getLowerCorner().getOrdinate(0);
        double miny = refEnvelope.getLowerCorner().getOrdinate(1);

        double maxx = refEnvelope.getUpperCorner().getOrdinate(0);
        double maxy = refEnvelope.getUpperCorner().getOrdinate(1);

        return "[[[" + minx + "," + miny + "],[" + maxx + "," + miny + "],[" + maxx + "," + maxy
                + "],[" + minx + "," + maxy + "],[" + minx + "," + miny + "]]]";
    }

    /** @param refEnvelope */
    private static String bboxToString(ReferencedEnvelope refEnvelope) {
        double minx = refEnvelope.getLowerCorner().getOrdinate(0);
        double miny = refEnvelope.getLowerCorner().getOrdinate(1);

        double maxx = refEnvelope.getUpperCorner().getOrdinate(0);
        double maxy = refEnvelope.getUpperCorner().getOrdinate(1);

        return minx + "," + miny + "," + maxx + "," + maxy;
    }

    /**
     * Makes sure the url does not end with "/", otherwise we would have URL lik
     * "http://localhost:8080/geoserver//wms?LAYERS=..." and Jetty 6.1 won't digest them...
     */
    private static String canonicUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            return baseUrl;
        }
    }
}
