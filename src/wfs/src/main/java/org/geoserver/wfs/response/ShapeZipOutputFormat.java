/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.template.TemplateUtils;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDumper;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.wfs.v1_0.WFSConfiguration_1_0;
import org.geotools.wfs.v1_1.WFS;
import org.geotools.wfs.v1_1.WFSConfiguration;
import org.geotools.xsd.Encoder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * This class returns a shapefile encoded results of the users's query.
 *
 * <p>Based on ShapeFeatureResponseDelegate.java from geoserver 1.5.x
 *
 * @author originally authored by Chris Holmes, The Open Planning Project, cholmes@openplans.org
 * @author ported to gs 1.6.x by Saul Farber, MassGIS, saul.farber@state.ma.us
 */
public class ShapeZipOutputFormat extends WFSGetFeatureOutputFormat
        implements ApplicationContextAware {
    private static final Logger LOGGER = Logging.getLogger(ShapeZipOutputFormat.class);
    public static final String GS_SHAPEFILE_CHARSET = "GS-SHAPEFILE-CHARSET";
    public static final String SHAPE_ZIP_DEFAULT_PRJ_IS_ESRI = "SHAPE-ZIP_DEFAULT_PRJ_IS_ESRI";

    private static final Configuration templateConfig = TemplateUtils.getSafeConfiguration();

    private ApplicationContext applicationContext;
    private Catalog catalog;
    private GeoServerResourceLoader resourceLoader;
    private long maxShpSize = Long.getLong("GS_SHP_MAX_SIZE", Integer.MAX_VALUE);
    private long maxDbfSize = Long.getLong("GS_DBF_MAX_SIZE", Integer.MAX_VALUE);

    public ShapeZipOutputFormat(
            GeoServer gs, Catalog catalog, GeoServerResourceLoader resourceLoader) {
        super(gs, "SHAPE-ZIP");
        this.catalog = catalog;
        this.resourceLoader = resourceLoader;
    }

    /** @see WFSGetFeatureOutputFormat#getMimeType(Object, Operation) */
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/zip";
    }

    public String getCapabilitiesElementName() {
        return "SHAPE-ZIP";
    }

    /**
     * We abuse this method to pre-discover the query typenames so we know what to set in the
     * content-disposition header.
     */
    protected boolean canHandleInternal(Operation operation) {
        return true;
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    /**
     * Get this output format's file name for for the zipped shapefile.
     *
     * <p>The output file name is determined as follows:
     *
     * <ul>
     *   <li>If the {@code GetFeature} request indicated a desired file name, then that one is used
     *       as is. The request may have specified the output file name through the {@code FILENAME
     *       } format option. For example: {@code &format_options=FILENAME:roads.zip}
     *   <li>Otherwise a file name is inferred from the requested feature type(s) name.
     * </ul>
     *
     * @return the the file name for the zipped shapefile(s)
     */
    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        SimpleFeatureCollection fc =
                (SimpleFeatureCollection) ((FeatureCollectionResponse) value).getFeature().get(0);
        FeatureTypeInfo ftInfo = getFeatureTypeInfo(fc.getSchema());

        String filename = null;
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
        if (request != null) {
            Map<String, ?> formatOptions = request.getFormatOptions();
            filename = (String) formatOptions.get("FILENAME");
        }
        if (filename == null) {
            filename = new FileNameSource(getClass()).getZipName(ftInfo);
        }
        if (filename == null) {
            filename = ftInfo.getName();
        }
        return filename + (filename.endsWith(".zip") ? "" : ".zip");
    }

    public void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {
        List<SimpleFeatureCollection> collections = new ArrayList<SimpleFeatureCollection>();
        collections.addAll((List) featureCollection.getFeature());
        Charset charset = getShapefileCharset(getFeature);
        write(collections, charset, output, GetFeatureRequest.adapt(getFeature.getParameters()[0]));
    }

    /** @see WFSGetFeatureOutputFormat#write(Object, OutputStream, Operation) */
    public void write(
            List<SimpleFeatureCollection> collections,
            Charset charset,
            OutputStream output,
            final GetFeatureRequest request)
            throws IOException, ServiceException {
        // We might get multiple featurecollections in our response (multiple queries?) so we need
        // to
        // write out multiple shapefile sets, one for each query response.
        final File tempDir = IOUtils.createTempDirectory("shpziptemp");
        ShapefileDumper dumper =
                new ShapefileDumper(tempDir) {

                    @Override
                    protected String getShapeName(SimpleFeatureType schema, String geometryType) {
                        FeatureTypeInfo ftInfo = getFeatureTypeInfo(schema);
                        String fileName =
                                new FileNameSource(getClass()).getShapeName(ftInfo, geometryType);
                        return fileName;
                    }

                    @Override
                    protected void shapefileDumped(
                            String fileName, SimpleFeatureType remappedSchema) throws IOException {
                        try {
                            changeWKTFormatIfFileFormatIsESRI(
                                    tempDir, request, fileName, remappedSchema);
                        } catch (FactoryException e) {
                            throw new IOException("Failed to write out the ESRI style prj file", e);
                        }
                    }
                };
        dumper.setMaxDbfSize(maxDbfSize);
        dumper.setMaxShpSize(maxShpSize);
        dumper.setCharset(charset);

        // target charset

        try {
            // if an empty result out of feature type with unknown geometry is created, the
            // zip file will be empty and the zip output stream will break
            boolean shapefileCreated = false;
            for (SimpleFeatureCollection collection : collections) {
                shapefileCreated |= dumper.dump(collection);
            }

            // take care of the case the output is completely empty
            if (!shapefileCreated) {
                createEmptyZipWarning(tempDir);
            }

            // dump the request
            createRequestDump(tempDir, request, collections.get(0));

            // zip all the files produced
            final FilenameFilter filter =
                    new FilenameFilter() {

                        public boolean accept(File dir, String name) {
                            name = name.toLowerCase();
                            return name.endsWith(".shp")
                                    || name.endsWith(".shx")
                                    || name.endsWith(".dbf")
                                    || name.endsWith(".prj")
                                    || name.endsWith(".cst")
                                    || name.endsWith(".txt");
                        }
                    };
            ZipOutputStream zipOut = new ZipOutputStream(output);
            IOUtils.zipDirectory(tempDir, zipOut, filter);
            zipOut.finish();

            // This is an error, because this closes the output stream too... it's
            // not the right place to do so
            // zipOut.close();
        } finally {
            // make sure we remove the temp directory and its contents completely now
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch (IOException e) {
                LOGGER.warning(
                        "Could not delete temp directory: "
                                + tempDir.getAbsolutePath()
                                + " due to: "
                                + e.getMessage());
            }
        }
    }

    /** Dumps the request */
    private void createRequestDump(
            File tempDir, GetFeatureRequest gft, SimpleFeatureCollection fc) {
        final Request request = Dispatcher.REQUEST.get();
        if (request == null || gft == null) {
            // we're probably running in a unit test
            return;
        }

        // build the target file
        FeatureTypeInfo ftInfo = getFeatureTypeInfo(fc.getSchema());
        String fileName = new FileNameSource(getClass()).getRequestDumpName(ftInfo) + ".txt";
        File target = new File(tempDir, fileName);

        try {
            if (request.isGet()) {
                final HttpServletRequest httpRequest = request.getHttpRequest();
                String baseUrl = ResponseUtils.baseURL(httpRequest);
                String path = request.getPath();
                // encode proxy url if existing
                String mangledUrl = ResponseUtils.buildURL(baseUrl, path, null, URLType.SERVICE);
                StringBuilder url = new StringBuilder();
                String parameters = httpRequest.getQueryString();
                url.append(mangledUrl).append("?").append(parameters);
                FileUtils.writeStringToFile(target, url.toString(), "UTF-8");
            } else {
                org.geotools.xsd.Configuration cfg = null;
                QName elementName = null;
                if (gft.getVersion().equals("1.1.0")) {
                    cfg = new WFSConfiguration();
                    elementName = WFS.GetFeature;
                } else {
                    cfg = new WFSConfiguration_1_0();
                    elementName = org.geotools.wfs.v1_0.WFS.GetFeature;
                }
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(target);
                    Encoder encoder = new Encoder(cfg);
                    encoder.setIndenting(true);
                    encoder.setIndentSize(2);
                    encoder.encode(gft, elementName, fos);
                } finally {
                    if (fos != null) fos.close();
                }
            }
        } catch (IOException e) {
            throw new WFSException(gft, "Failed to dump the WFS request");
        }
    }

    private void createEmptyZipWarning(File tempDir) throws IOException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(tempDir, "README.TXT"));
            pw.print(
                    "The query result is empty, and the geometric type of the features is unknwon:"
                            + "an empty point shapefile has been created to fill the zip file");
        } finally {
            if (pw != null) pw.close();
        }
    }

    /**
     * Either retrieves the corresponding FeatureTypeInfo from the catalog or fakes one with the
     * necessary information
     */
    private FeatureTypeInfo getFeatureTypeInfo(SimpleFeatureType schema) {
        FeatureTypeInfo ftInfo = catalog.getFeatureTypeByName(schema.getName());
        if (ftInfo == null) {
            // SG the fc might have been generated by the WPS therefore there is no such a thing
            // inside the GeoServer catalogue
            final SimpleFeatureSource featureSource =
                    DataUtilities.source(new ListFeatureCollection(schema));
            final CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);
            catalogBuilder.setStore(catalogBuilder.buildDataStore(schema.getName().getLocalPart()));
            ftInfo = catalogBuilder.buildFeatureType(featureSource);
        }
        return ftInfo;
    }

    /**
     * If the {@code GetFeature} request indicated a desired ESRI WKT format or the
     * SHAPE-ZIP_DEFAULT_PRJ_IS_ESRI property in metadata component of wfs.xml is true and there is
     * an entrance for EPSG code in user_projections/esri.properties file, then the .prj file is
     * replaced with a new one in ESRI WKT format. The content of the new file is extracted from
     * user_projections/esri.properties using EPSG code as key. For example: {@code
     * &format_options=PRJFILEFORMAT:ESRI}. Otherwise, the output prj file format is OGC WKT format.
     */
    private void changeWKTFormatIfFileFormatIsESRI(
            File tempDir,
            GetFeatureRequest request,
            String fileName,
            SimpleFeatureType remappedSchema)
            throws FactoryException, IOException, FileNotFoundException {

        boolean useEsriFormat = false;

        // if the request originates from the WPS we won't actually have any GetFeatureType request
        if (request == null) {
            return;
        }

        Map<String, ?> formatOptions = request.getFormatOptions();
        final String requestedPrjFileFormat = (String) formatOptions.get("PRJFILEFORMAT");
        if (null == requestedPrjFileFormat) {
            WFSInfo bean = gs.getService(WFSInfo.class);
            MetadataMap metadata = bean.getMetadata();
            Boolean defaultIsEsri = metadata.get(SHAPE_ZIP_DEFAULT_PRJ_IS_ESRI, Boolean.class);
            useEsriFormat = defaultIsEsri != null && defaultIsEsri.booleanValue();
        } else {
            useEsriFormat = "ESRI".equalsIgnoreCase(requestedPrjFileFormat);
        }

        if (useEsriFormat) {
            replaceOGCPrjFileByESRIPrjFile(tempDir, fileName, remappedSchema);
        }
    }

    private void replaceOGCPrjFileByESRIPrjFile(
            File tempDir, String fileName, SimpleFeatureType remappedSchema)
            throws FactoryException, IOException, FileNotFoundException {
        final Integer epsgCode =
                CRS.lookupEpsgCode(
                        remappedSchema.getGeometryDescriptor().getCoordinateReferenceSystem(),
                        true);
        if (epsgCode == null) {
            LOGGER.info("Can't find the EPSG code for the shapefile CRS");
            return;
        }
        Resource file = resourceLoader.get("user_projections/esri.properties");

        if (file.getType() == Type.RESOURCE) {
            Properties properties = new Properties();
            try (InputStream fis = file.in()) {
                properties.load(fis);
            }

            String data = (String) properties.get(epsgCode.toString());

            if (data != null) {
                File prjShapeFile = new File(tempDir, fileName + ".prj");
                prjShapeFile.delete();

                BufferedWriter out = new BufferedWriter(new FileWriter(prjShapeFile));
                try {
                    out.write(data);
                } finally {
                    out.close();
                }
            } else {
                LOGGER.info(
                        "Requested shapefile with ESRI WKT .prj format but couldn't find an entry for ESPG code "
                                + epsgCode
                                + " in esri.properties");
            }
        } else {
            LOGGER.info(
                    "Requested shapefile with ESRI WKT .prj format but the esri.properties file does not exist in the user_projections directory");
        }
    }

    /**
     * Looks up the charset parameter, either in the GetFeature request or as a global parameter
     *
     * @return the found charset, or the platform's default one if none was specified
     */
    private Charset getShapefileCharset(Operation getFeature) {
        Charset result = null;

        GetFeatureRequest gft = GetFeatureRequest.adapt(getFeature.getParameters()[0]);
        if (gft.getFormatOptions() != null && gft.getFormatOptions().get("CHARSET") != null) {
            result = (Charset) gft.getFormatOptions().get("CHARSET");
        } else {
            final String charsetName =
                    GeoServerExtensions.getProperty(GS_SHAPEFILE_CHARSET, applicationContext);
            if (charsetName != null) result = Charset.forName(charsetName);
        }

        // if not specified let's use the shapefile default one
        return result != null ? result : Charset.forName("ISO-8859-1");
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public long getMaxShpSize() {
        return maxShpSize;
    }

    /** Sets the maximum shapefile size (2GB by default) */
    public void setMaxShpSize(long maxShapefileSize) {
        this.maxShpSize = maxShapefileSize;
    }

    public long getMaxDbfSize() {
        return maxDbfSize;
    }

    /** Sets the maximum shapefile size (2GB by default) */
    public void setMaxDbfSize(long maxDbfSize) {
        this.maxDbfSize = maxDbfSize;
    }

    class FileNameSource {

        private Class clazz;

        public FileNameSource(Class clazz) {
            this.clazz = clazz;
        }

        private Properties processTemplate(FeatureTypeInfo ftInfo, String geometryType) {
            try {
                // setup template subsystem
                GeoServerTemplateLoader templateLoader =
                        new GeoServerTemplateLoader(clazz, resourceLoader);
                templateLoader.setFeatureType(ftInfo);

                // load the template
                Template template = null;
                synchronized (templateConfig) {
                    templateConfig.setTemplateLoader(templateLoader);
                    template = templateConfig.getTemplate("shapezip.ftl");
                }

                // prepare the template context
                Date timestamp;
                if (Dispatcher.REQUEST.get() != null) {
                    timestamp = Dispatcher.REQUEST.get().getTimestamp();
                } else {
                    timestamp = new Date();
                }
                Map<String, Object> context = new HashMap<String, Object>();
                context.put("typename", getTypeName(ftInfo));
                context.put("workspace", ftInfo.getNamespace().getPrefix());
                context.put("geometryType", geometryType == null ? "" : geometryType);
                context.put("timestamp", timestamp);
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
                java.util.Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
                format.setCalendar(cal);
                context.put("iso_timestamp", format.format(timestamp));

                // process the template, write out and turn it into a property map
                StringWriter sw = new StringWriter();
                template.process(context, sw);

                Properties props = new Properties();
                props.load(new ByteArrayInputStream(sw.toString().getBytes()));

                return props;
            } catch (Exception e) {
                throw new WFSException("Failed to process the file name template", e);
            }
        }

        private String getTypeName(FeatureTypeInfo ftInfo) {
            return ftInfo.getName().replace(".", "_");
        }

        public String getZipName(FeatureTypeInfo ftInfo) {
            Properties props = processTemplate(ftInfo, null);
            String filename = props.getProperty("zip");
            if (filename == null) {
                filename = getTypeName(ftInfo);
            }

            return filename;
        }

        public String getShapeName(FeatureTypeInfo ftInfo, String geometryType) {
            Properties props = processTemplate(ftInfo, geometryType);
            String filename = props.getProperty("shp");
            if (filename == null) {
                filename = getTypeName(ftInfo) + geometryType;
            }

            return filename;
        }

        public String getRequestDumpName(FeatureTypeInfo ftInfo) {
            Properties props = processTemplate(ftInfo, null);
            String filename = props.getProperty("txt");
            if (filename == null) {
                filename = getTypeName(ftInfo);
            }

            return filename;
        }
    }
}
