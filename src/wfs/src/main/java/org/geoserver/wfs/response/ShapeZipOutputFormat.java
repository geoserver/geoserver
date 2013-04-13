/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;
 
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.logging.Level;
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
import org.geoserver.data.util.IOUtils;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.wfs.v1_1.WFS;
import org.geotools.wfs.v1_1.WFSConfiguration;
import org.geotools.xml.Encoder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import freemarker.template.Configuration;
import freemarker.template.Template;


/**
 *
 * This class returns a shapefile encoded results of the users's query.
 *
 * Based on ShapeFeatureResponseDelegate.java from geoserver 1.5.x
 *
 * @author originally authored by Chris Holmes, The Open Planning Project, cholmes@openplans.org
 * @author ported to gs 1.6.x by Saul Farber, MassGIS, saul.farber@state.ma.us
 *
 */
public class ShapeZipOutputFormat extends WFSGetFeatureOutputFormat implements ApplicationContextAware {
    private static final Logger LOGGER = Logging.getLogger(ShapeZipOutputFormat.class);
    public static final String GS_SHAPEFILE_CHARSET = "GS-SHAPEFILE-CHARSET";
    public static final String SHAPE_ZIP_DEFAULT_PRJ_IS_ESRI = "SHAPE-ZIP_DEFAULT_PRJ_IS_ESRI";
    
    private static final Configuration templateConfig = new Configuration();
    
    private ApplicationContext applicationContext;
    private Catalog catalog;
	private GeoServerResourceLoader resourceLoader;
    
    /**
     * Tuple used when fanning out a collection with generic geometry types to multiple outputs 
     * @author Administrator
     *
     */
    private static class StoreWriter {
        DataStore dstore;
        FeatureWriter<SimpleFeatureType, SimpleFeature> writer;
    }

    /**
     * @deprecated use {@link #ShapeZipOutputFormat(GeoServer)}
     */
    public ShapeZipOutputFormat() {
        this(GeoServerExtensions.bean(GeoServer.class), 
                (Catalog) GeoServerExtensions.bean("catalog"), 
                (GeoServerResourceLoader) GeoServerExtensions.bean("resourceLoader"));
    }
    
    public ShapeZipOutputFormat(GeoServer gs, Catalog catalog, GeoServerResourceLoader resourceLoader) {
        super(gs, "SHAPE-ZIP");
        this.catalog = catalog;
        this.resourceLoader = resourceLoader;
    }

    /**
     * @see WFSGetFeatureOutputFormat#getMimeType(Object, Operation)
     */
    public String getMimeType(Object value, Operation operation)
        throws ServiceException {
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
     * <p>
     * The output file name is determined as follows:
     * <ul>
     * <li>If the {@code GetFeature} request indicated a desired file name, then that one is used as
     * is. The request may have specified the output file name through the {@code FILENAME } format
     * option. For example: {@code &format_options=FILENAME:roads.zip}
     * <li>Otherwise a file name is inferred from the requested feature type(s) name.
     * </ul>
     * 
     * @return the the file name for the zipped shapefile(s)
     * 
     */
    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        SimpleFeatureCollection fc = (SimpleFeatureCollection) ((FeatureCollectionResponse) value).getFeature().get(0);
        FeatureTypeInfo ftInfo = getFeatureTypeInfo(fc);
        
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
    
    protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
            Operation getFeature) throws IOException, ServiceException {
    	List<SimpleFeatureCollection> collections = new ArrayList<SimpleFeatureCollection>();
        collections.addAll((List)featureCollection.getFeature());
        Charset charset = getShapefileCharset(getFeature);
        write(collections, charset, output, GetFeatureRequest.adapt(getFeature.getParameters()[0]));
    }

    /**
     * @see WFSGetFeatureOutputFormat#write(Object, OutputStream, Operation)
     */
    public void write(List<SimpleFeatureCollection> collections, Charset charset, OutputStream output, 
        GetFeatureRequest request) throws IOException, ServiceException {
        //We might get multiple featurecollections in our response (multiple queries?) so we need to
        //write out multiple shapefile sets, one for each query response.
        File tempDir = IOUtils.createTempDirectory("shpziptemp");
        
        // target charset
        
        try {
           // if an empty result out of feature type with unknown geometry is created, the
            // zip file will be empty and the zip output stream will break
            boolean shapefileCreated = false;
            for (SimpleFeatureCollection curCollection : collections) {
                
                if(curCollection.getSchema().getGeometryDescriptor() == null) {
                    throw new WFSException(request, "Cannot write geometryless shapefiles, yet " 
                            + curCollection.getSchema() + " has no geometry field");
                } 
                Class geomType = curCollection.getSchema().getGeometryDescriptor().getType().getBinding();
                if(GeometryCollection.class.equals(geomType) || Geometry.class.equals(geomType)) {
                    // in this case we fan out the output to multiple shapefiles
                    shapefileCreated |= writeCollectionToShapefiles(curCollection, tempDir, charset, request);
                } else {
                    // simple case, only one and supported type
                    writeCollectionToShapefile(curCollection, tempDir, charset, request);
                    shapefileCreated = true;
                }

            }
            
            // take care of the case the output is completely empty
            if(!shapefileCreated) {
                SimpleFeatureCollection fc;
                fc = (SimpleFeatureCollection) collections.get(0);
                fc = remapCollectionSchema(fc, Point.class);
                writeCollectionToShapefile(fc, tempDir, charset, request);
                createEmptyZipWarning(tempDir);
            }
            
            // dump the request
            createRequestDump(tempDir, request, collections.get(0));
            
            // zip all the files produced
            final FilenameFilter filter = new FilenameFilter() {
            
                public boolean accept(File dir, String name) {
                    return name.endsWith(".shp") || name.endsWith(".shx") || name.endsWith(".dbf")
                           || name.endsWith(".prj") || name.endsWith(".cst") || name.endsWith(".txt");
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
            } catch(IOException e) {
                LOGGER.warning("Could not delete temp directory: " + tempDir.getAbsolutePath() + " due to: " + e.getMessage());
            }
        }
    }

    /**
     * Dumps the request
     * @param simpleFeatureCollection
     */
    private void createRequestDump(File tempDir, GetFeatureRequest gft, SimpleFeatureCollection fc) {
        final Request request = Dispatcher.REQUEST.get();
        if(request == null || gft == null) {
            // we're probably running in a unit test
            return;
        }
        
        // build the target file
        FeatureTypeInfo ftInfo = getFeatureTypeInfo(fc);
        String fileName = new FileNameSource(getClass()).getRequestDumpName(ftInfo) + ".txt";
        File target = new File(tempDir, fileName);
        
        try {
            if(request.isGet()) {
                final HttpServletRequest httpRequest = request.getHttpRequest();
                String baseUrl = ResponseUtils.baseURL(httpRequest);
                String path = request.getPath();
                //encode proxy url if existing
                String mangledUrl = ResponseUtils.buildURL(baseUrl, path, null, URLType.SERVICE);
                StringBuilder url = new StringBuilder();
                String parameters = httpRequest.getQueryString();
				url.append(mangledUrl).append("?").append(parameters);
                FileUtils.writeStringToFile(target, url.toString());
            } else {
                org.geotools.xml.Configuration cfg = null;
                QName elementName = null;
                if(gft.getVersion().equals("1.1.0")) {
                    cfg = new WFSConfiguration();
                    elementName = WFS.GetFeature;
                } else {
                    cfg = new org.geotools.wfs.v1_0.WFSConfiguration();
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
                    if(fos != null)
                        fos.close();
                }
            }
        } catch(IOException e) {
            throw new WFSException(gft, "Failed to dump the WFS request");
        }
        
    }

    private void createEmptyZipWarning(File tempDir) throws IOException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(tempDir, "README.TXT"));
            pw.print("The query result is empty, and the geometric type of the features is unknwon:" +
            		"an empty point shapefile has been created to fill the zip file");
        } finally {
            pw.close();
        }
    }   

    /**
     * Write one featurecollection to an appropriately named shapefile.
     * @param c the featurecollection to write
     * @param tempDir the temp directory into which it should be written
     */
    private void writeCollectionToShapefile(SimpleFeatureCollection c, File tempDir, Charset charset, 
        GetFeatureRequest request) {
        FeatureTypeInfo ftInfo = getFeatureTypeInfo(c);

        c = remapCollectionSchema(c, null);
        
        SimpleFeatureType schema = c.getSchema();
        String fileName = new FileNameSource(getClass()).getShapeName(ftInfo, null);
        if(!fileName.equals(schema.getTypeName())) {
        	// rename the schema to have the proper output file name
        	SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        	tb.init(c.getSchema());
        	tb.setName(fileName);
        	SimpleFeatureType renamed = tb.buildFeatureType();
        	c = new RetypingFeatureCollection(c, renamed);
        }

        SimpleFeatureStore fstore = null;
        ShapefileDataStore dstore = null;
        try {
            // create attribute name mappings, to be compatible 
            // with shapefile constraints:
            //  - geometry field is always named the_geom
            //  - field names have a max length of 10
            Map<String,String> attributeMappings=createAttributeMappings(c.getSchema());
            // wraps the original collection in a remapping wrapper
            SimpleFeatureCollection remapped = new RemappingFeatureCollection(c,attributeMappings);
            SimpleFeatureType remappedSchema=(SimpleFeatureType)remapped.getSchema();
            dstore = buildStore(tempDir, charset,  remappedSchema); 
            fstore = (SimpleFeatureStore) dstore.getFeatureSource();
            // we need retyping too, because the shapefile datastore
            // could have sorted fields in a different order
            SimpleFeatureCollection retyped = new RetypingFeatureCollection(remapped, fstore.getSchema());
            fstore.addFeatures(retyped);
            
            changeWKTFormatIfFileFormatIsESRI(tempDir, request, fileName,
					remappedSchema);
          
        } catch (FactoryException fe) {
        	LOGGER.log(Level.WARNING,
        			"Error while getting EPSG code from FeatureType", fe);
        	throw new ServiceException(fe);
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING,
                "Error while writing featuretype '" + schema.getTypeName() + "' to shapefile.", ioe);
            throw new ServiceException(ioe);
        } finally {
            if(dstore != null) {
                dstore.dispose();
            }
        }
    }

    /**
     * Either retrieves the corresponding FeatureTypeInfo from the catalog or fakes one
     * with the necessary information 
     * @param c
     * @return
     */
    private FeatureTypeInfo getFeatureTypeInfo(SimpleFeatureCollection c) {
        FeatureTypeInfo ftInfo = catalog.getFeatureTypeByName(c.getSchema().getName());
        if (ftInfo == null) {
            // SG the fc might have been generated by the WPS therefore there is no such a thing
            // inside the GeoServer catalogue
            final SimpleFeatureSource featureSource = DataUtilities.source(c);
            final CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);
            catalogBuilder.setStore(catalogBuilder.buildDataStore(c.getSchema().getName()
                    .getLocalPart()));
            ftInfo = catalogBuilder.buildFeatureType(featureSource);

        }
        return ftInfo;
    }

    /**
     * <p>
     * If the {@code GetFeature} request indicated a desired ESRI WKT format or the
     * SHAPE-ZIP_DEFAULT_PRJ_IS_ESRI property in metadata component of wfs.xml is true and there is
     * an entrance for EPSG code in user_projections/esri.properties file, then the .prj file is
     * replaced with a new one in ESRI WKT format. The content of the new file is extracted from
     * user_projections/esri.properties using EPSG code as key. For example:
     * {@code &format_options=PRJFILEFORMAT:ESRI}. Otherwise, the output prj file format is OGC WKT
     * format.
     * </p>
     */
    private void changeWKTFormatIfFileFormatIsESRI(File tempDir, GetFeatureRequest request,
            String fileName, SimpleFeatureType remappedSchema) throws FactoryException,
            IOException, FileNotFoundException {
        
        boolean useEsriFormat = false;
        
        // if the request originates from the WPS we won't actually have any GetFeatureType request
        if(request == null) {
            return;
        }
        
        Map<String, ?> formatOptions = request.getFormatOptions();
        final String requestedPrjFileFormat = (String) formatOptions.get("PRJFILEFORMAT");
        if (null == requestedPrjFileFormat) {
            WFSInfo bean = gs.getService(WFSInfo.class);
            MetadataMap metadata = bean.getMetadata();
            Boolean defaultIsEsri = metadata.get(SHAPE_ZIP_DEFAULT_PRJ_IS_ESRI, Boolean.class);
            useEsriFormat = defaultIsEsri != null && defaultIsEsri.booleanValue();
        }else{
            useEsriFormat = "ESRI".equalsIgnoreCase(requestedPrjFileFormat);
        }
        
        if (useEsriFormat) {
            replaceOGCPrjFileByESRIPrjFile(tempDir, fileName, remappedSchema);
        }
    }

    private void replaceOGCPrjFileByESRIPrjFile(File tempDir, String fileName,
            SimpleFeatureType remappedSchema) throws FactoryException, IOException,
            FileNotFoundException {
        final Integer epsgCode = CRS.lookupEpsgCode(remappedSchema.getGeometryDescriptor()
                .getCoordinateReferenceSystem(), true);
        if(epsgCode == null){
            LOGGER.info("Can't find the EPSG code for the shapefile CRS");
            return;
        }
        File file = resourceLoader.find("user_projections", "esri.properties");

        if (file != null && file.exists()) {
            Properties properties = new Properties();
            FileInputStream fis = null;
            try {
            	fis = new FileInputStream(file);
            	properties.load(fis);
            } finally {
            	org.apache.commons.io.IOUtils.closeQuietly(fis);
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
                LOGGER.info("Requested shapefile with ESRI WKT .prj format but couldn't find an entry for ESPG code "
                        + epsgCode + " in esri.properties");
            }
        } else {
            LOGGER.info("Requested shapefile with ESRI WKT .prj format but the esri.properties file does not exist in the user_projections directory");
        }
    }
    
    /**
     * Takes a feature collection with a generic schema and remaps it to one whose schema
     * respects the limitations of the shapefile format
     * @param fc
     * @param targetGeometry
     * @return
     */
    SimpleFeatureCollection remapCollectionSchema(SimpleFeatureCollection fc, Class targetGeometry) {
        SimpleFeatureType schema = fc.getSchema();
        
        // force the proper output type if necessary
        if(targetGeometry != null) {
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            // force generic geometric attributes to the desired geometry type
            for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
                if(!(ad instanceof GeometryDescriptor)) {
                    tb.add(ad);
                } else {
                    Class geomType = ad.getType().getBinding();
                    if(geomType.equals(Geometry.class) || geomType.equals(GeometryCollection.class)) {
                        tb.add(ad.getName().getLocalPart(), targetGeometry, 
                                ((GeometryDescriptor) ad).getCoordinateReferenceSystem());
                    } else {
                        tb.add(ad);
                    }
                }
            } 
            tb.setName(fc.getSchema().getName());
            SimpleFeatureType retyped = tb.buildFeatureType();
            fc = new RetypingFeatureCollection(fc, retyped);
        }
        
        // create attribute name mappings, to be compatible 
        // with shapefile constraints:
        //  - geometry field is always named the_geom
        //  - field names have a max length of 10
        Map<String,String> attributeMappings = createAttributeMappings(schema);
        return new RemappingFeatureCollection(fc,attributeMappings);
    }
    
    /**
     * Maps schema attributes to shapefile-compatible attributes.
     * 
     * @param schema
     * @return
     */
    private Map<String, String> createAttributeMappings(SimpleFeatureType schema) {
        Map<String, String> result = new HashMap<String,String>();
        
        // track the field names used and reserve "the_geom" for the geometry
        Set<String> usedFieldNames = new HashSet<String>(); 
        usedFieldNames.add("the_geom");
        
        // scan and remap
        for(AttributeDescriptor attDesc : schema.getAttributeDescriptors()) {
            if(attDesc instanceof GeometryDescriptor) {
                result.put(attDesc.getLocalName(), "the_geom");
            } else {
                String name = attDesc.getLocalName();
                result.put(attDesc.getLocalName(), getShapeCompatibleName(usedFieldNames, name));
            }
        }
        return result;
    }
    
    /**
     * If necessary remaps the name so that it's less than 10 chars long and 
     * @param usedFieldNames
     * @param name
     * @return
     */
    String getShapeCompatibleName(Set<String> usedFieldNames, String name) {
        // 10 chars limit
        if(name.length() > 10)
            name = name.substring(0,10);
        
        // don't use an already assigned name, create a new unique name (it might
        // conflict even if we did not cut it to 10 chars due to remaps of previous long attributes)
        int counter=0;
        while(usedFieldNames.contains(name)) {
            String postfix=(counter++)+"";
            name = name.substring(0, name.length() - postfix.length()) + postfix;
        }
        usedFieldNames.add(name);
        
        return name;
    }
       
    /**
     * Write one featurecollection to a group of appropriately named shapefiles, one per geometry
     * type. This method assume the features will have a Geometry type and the actual type of each
     * feature will be discovered during the scan. Each feature will be routed to a shapefile that
     * contains only a specific geometry type chosen among point, multipoint, polygons and lines.
     * @param c the featurecollection to write
     * @param tempDir the temp directory into which it should be written
     * @param request 
     * @return true if a shapefile has been created, false otherwise
     */
    private boolean writeCollectionToShapefiles(SimpleFeatureCollection c, File tempDir, Charset charset, 
        GetFeatureRequest request) {
        FeatureTypeInfo ftInfo = getFeatureTypeInfo(c);
        c = remapCollectionSchema(c, null);
        SimpleFeatureType schema = c.getSchema();
        
        boolean shapefileCreated = false;
        
        Map<Class, StoreWriter> writers = new HashMap<Class, StoreWriter>();
        SimpleFeatureIterator it;
        try {
            it = c.features(); 
            while(it.hasNext()) {
                SimpleFeature f = it.next();
                
                if(f.getDefaultGeometry() == null) {
                    LOGGER.warning("Skipping " + f.getID() + " as its geometry is null");
                    continue;
                }
                
                FeatureWriter<SimpleFeatureType, SimpleFeature> writer = getFeatureWriter(ftInfo, f, writers, tempDir, charset);
                SimpleFeature fw = writer.next();
                
                // we cannot trust attribute order, shapefile changes the location and name of the geometry
                for (AttributeDescriptor d : fw.getFeatureType().getAttributeDescriptors()) {
                    fw.setAttribute(d.getLocalName(), f.getAttribute(d.getLocalName()));
                }
                fw.setDefaultGeometry(f.getDefaultGeometry());
                writer.write();
                shapefileCreated = true;
                writer.getFeatureType().getName().getLocalPart();
                String geometryType = (String) getGeometryType((Geometry)f.getDefaultGeometry()).get("geometryType");
				String fileName = new FileNameSource(getClass()).getShapeName(ftInfo, geometryType);
                changeWKTFormatIfFileFormatIsESRI(tempDir, request, fileName,
                		schema);
            }
            
        } catch (FactoryException fe) {
        	LOGGER.log(Level.WARNING,
        			"Error while getting EPSG code from FeatureType", fe);
        	throw new ServiceException(fe);    
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING,
                "Error while writing featuretype '" + schema.getTypeName() + "' to shapefile.", ioe);
            throw new ServiceException(ioe);
        } finally {
            // close all writers, dispose all datastores, even if an exception occurs
            // during closeup (shapefile datastore will have to copy the shapefiles, that migh
            // fail in many ways)
            IOException stored = null;
            for (StoreWriter sw : writers.values()) {
                try {
                    sw.writer.close();
                    sw.dstore.dispose();
                } catch(IOException e) {
                    stored = e;
                }
            }
            // if an exception occurred make the world aware of it
            if(stored != null)
                throw new ServiceException(stored);
        }
        
        return shapefileCreated;
    }
    
    /**
     * Returns the feature writer for a specific geometry type, creates a new datastore
     * and a new writer if there are none so far
     */
    private FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(FeatureTypeInfo ftInfo, SimpleFeature f, 
            Map<Class, StoreWriter> writers, File tempDir, Charset charset) throws IOException {
        // get the target class
    	Map<String, Object> map = getGeometryType((Geometry) f.getDefaultGeometry());
        Class<?> target = (Class<?>) map.get("target");
        String geometryType = (String) map.get("geometryType");
        
        // see if we already have a cached writer
        StoreWriter storeWriter = writers.get(target);
        if(storeWriter == null) {
            // retype the schema
            SimpleFeatureType original = f.getFeatureType();
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            for (AttributeDescriptor d : original.getAttributeDescriptors()) {
                if(Geometry.class.isAssignableFrom(d.getType().getBinding())) {
                    GeometryDescriptor gd = (GeometryDescriptor) d;
                    builder.add(gd.getLocalName(), target, gd.getCoordinateReferenceSystem());
                    builder.setDefaultGeometry(gd.getLocalName());
                } else {
                    builder.add(d);
                }
            }
            builder.setNamespaceURI(original.getName().getURI());
            String fileName = new FileNameSource(getClass()).getShapeName(ftInfo, geometryType);
            builder.setName(fileName);
            SimpleFeatureType retyped = builder.buildFeatureType();
            
            // create the datastore for the current geom type
            DataStore dstore = buildStore(tempDir, charset, retyped);
            
            // cache it
            storeWriter = new StoreWriter();
            storeWriter.dstore = dstore;
            storeWriter.writer = dstore.getFeatureWriter(retyped.getTypeName(), Transaction.AUTO_COMMIT);
            writers.put(target, storeWriter);
        }
        return storeWriter.writer;
    }
    
    private Map<String, Object> getGeometryType(Geometry g) {
    	Class<?> target;
        String geometryType = null;
    	
        if(g instanceof Point) {
            target = Point.class;
            geometryType = "Point";
        } else if(g instanceof MultiPoint) {
            target = MultiPoint.class;
            geometryType = "MPoint";
        } else if(g instanceof MultiPolygon || g instanceof Polygon) {
            target = MultiPolygon.class;
            geometryType = "Polygon";
        } else if(g instanceof LineString || g instanceof MultiLineString) {
            target = MultiLineString.class;
            geometryType = "Line";
        } else {
            throw new RuntimeException("This should never happen, " +
            		"there's a bug in the SHAPE-ZIP output format. I got a geometry of type " + g.getClass());
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("target", target);
        map.put("geometryType", geometryType);
        return map;
    }


    /**
     * Looks up the charset parameter, either in the GetFeature request or as a global parameter
     * @param getFeature
     * @return the found charset, or the platform's default one if none was specified
     */
    private Charset getShapefileCharset(Operation getFeature) {
        Charset result = null;
        
        GetFeatureRequest gft = GetFeatureRequest.adapt(getFeature.getParameters()[0]);
        if(gft.getFormatOptions() != null && gft.getFormatOptions().get("CHARSET") != null) {
           result = (Charset) gft.getFormatOptions().get("CHARSET");
        } else {
            final String charsetName = GeoServerExtensions.getProperty(GS_SHAPEFILE_CHARSET, applicationContext);
            if(charsetName != null)
                result = Charset.forName(charsetName);
        }
        
        // if not specified let's use the shapefile default one
        return result != null ? result : Charset.forName("ISO-8859-1");
    }

    /**
     * Creates a shapefile data store for the specified schema 
     * @param tempDir
     * @param charset
     * @param schema
     * @return
     * @throws MalformedURLException
     * @throws FileNotFoundException
     * @throws IOException
     */
    private ShapefileDataStore buildStore(File tempDir,
            Charset charset, SimpleFeatureType schema) throws MalformedURLException,
            FileNotFoundException, IOException {
        File file = new File(tempDir, schema.getTypeName() + ".shp");
        ShapefileDataStore sfds = new ShapefileDataStore(file.toURL());
        
        // handle shapefile encoding
        // and dump the charset into a .cst file, for debugging and control purposes
        // (.cst is not a standard extension)
        sfds.setCharset(charset);
        File charsetFile = new File(tempDir, schema.getTypeName()+ ".cst");
        PrintWriter pw = null;
        try {
            pw  = new PrintWriter(charsetFile);
            pw.write(charset.name());
        } finally {
            if(pw != null) pw.close();
        }

        try {
            sfds.createSchema(schema);
        } catch (NullPointerException e) {
            LOGGER.warning(
                "Error in shapefile schema. It is possible you don't have a geometry set in the output. \n"
                + "Please specify a <wfs:PropertyName>geom_column_name</wfs:PropertyName> in the request");
            throw new ServiceException(
                "Error in shapefile schema. It is possible you don't have a geometry set in the output.");
        }
        
        try {
            if(schema.getCoordinateReferenceSystem() != null)
                sfds.forceSchemaCRS(schema.getCoordinateReferenceSystem());
        } catch(Exception e) {
            LOGGER.log(Level.WARNING, "Could not properly create the .prj file", e);
        }

        return sfds;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    
    static class FileNameSource {
        
        private Class clazz;

        public FileNameSource(Class clazz) {
            this.clazz = clazz;
        }
        
        private Properties processTemplate(FeatureTypeInfo ftInfo, String geometryType) {
            try {
                // setup template subsystem
                GeoServerTemplateLoader templateLoader = new GeoServerTemplateLoader(clazz);
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
            } catch(Exception e) {
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
