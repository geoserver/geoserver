/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.zip.ZipOutputStream;

import org.geoserver.config.GeoServer;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.store.EmptyFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gml.producer.FeatureTransformer;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Ogr2OgrOutputFormat extends WFSGetFeatureOutputFormat {
    
    /**
     * The types of geometries a shapefile can handle
     */
    private static final Set<Class> SHAPEFILE_GEOM_TYPES = new HashSet<Class>() {
        {
            add(Point.class);
            add(LineString.class);
            add(LinearRing.class);
            add(Polygon.class);
            add(MultiPoint.class);
            add(MultiLineString.class);
            add(MultiPolygon.class);
        }
    };
    
    /**
     * The fs path to ogr2ogr. If null, we'll assume ogr2ogr is in the PATH and
     * that we can execute it just by running ogr2ogr
     */
    String ogrPath = null;

    /**
     * The full path to ogr2ogr
     */
    String ogrExecutable = "ogr2ogr";
    
    /**
     * The GDAL_DATA folder
     */
    String gdalData = null;

    /**
     * The output formats we can generate using ogr2ogr. Using a concurrent
     * one so that it can be reconfigured while the output format is working
     */
    static Map<String, OgrFormat> formats = new ConcurrentHashMap<String, OgrFormat>();

    public Ogr2OgrOutputFormat(GeoServer gs) {
        // initialize with the key set of formats, so that it will change as
        // we register new formats
        super(gs, formats.keySet());
    }

    /**
     * Returns the ogr2ogr executable full path
     * 
     * @return
     */
    public String getOgrExecutable() {
        return ogrExecutable;
    }

    /**
     * Sets the ogr2ogr executable full path. The default value is simply
     * "ogr2ogr", which will work if ogr2ogr is in the path
     * 
     * @param ogrExecutable
     */
    public void setOgrExecutable(String ogrExecutable) {
        this.ogrExecutable = ogrExecutable;
    }
    
    /**
     * Returns the location of the gdal data folder (required to set the output srs)
     * @return
     */
    public String getGdalData() {
        return gdalData;
    }

    /**
     * Sets the location of the gdal data folder (requierd to set the output srs)
     * @param gdalData
     */
    public void setGdalData(String gdalData) {
        this.gdalData = gdalData;
    }

    /**
     * @see WFSGetFeatureOutputFormat#getMimeType(Object, Operation)
     */
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
        String outputFormat = request.getOutputFormat();
        String mimeType = "";
        OgrFormat format = formats.get(outputFormat);
        if (format == null) {
            throw new WFSException("Unknown output format " + outputFormat);
        } else if (format.singleFile && request.getQueries().size() <= 1) {
            if (format.mimeType != null) {
                mimeType = format.mimeType;
            } else {
                // use a default binary blob
                mimeType = "application/octet-stream";
            }
        } else {
            mimeType = "application/zip";
        }
        return mimeType;
    }
    
    @Override
    public boolean canHandle(Operation operation) {
        // we can't handle anything if the ogr2ogr configuration failed
        if(formats.size() == 0) {
            return false;
        } else {
            return super.canHandle(operation);
        }
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }
    
    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
        String outputFormat = request.getOutputFormat();
        
        OgrFormat format = formats.get(outputFormat);
        List<Query> queries = request.getQueries();
        if (format == null) {
            throw new WFSException("Unknown output format " + outputFormat);
        } else if (!format.singleFile || queries.size() > 1) {
            String outputFileName = queries.get(0).getTypeNames().get(0).getLocalPart();
            return outputFileName + ".zip";
        } else {
            return null;
        }
    }
    
    /**
     * Adds a ogr format among the supported ones
     * 
     * @param parameters
     */
    public void addFormat(OgrFormat parameters) {
        formats.put(parameters.formatName, parameters);
    }

    /**
     * Get a list of supported ogr format
     *
     * @return
     */
    public List<OgrFormat> getFormats() {
        return new ArrayList<OgrFormat>(formats.values());
    }

    /**
     * Programmatically removes all formats
     * 
     * @param parameters
     */
    public void clearFormats() {
        formats.clear();
    }

    /**
     * Writes out the data to an OGR known format (GML/shapefile) to disk and
     * then ogr2ogr each generated file into the destination format. Finally,
     * zips up all the resulting files.
     */
    @Override
    protected void write(FeatureCollectionResponse featureCollection, OutputStream output, 
        Operation getFeature) throws IOException ,ServiceException {

        // figure out which output format we're going to generate
        GetFeatureRequest request = GetFeatureRequest.adapt(getFeature.getParameters()[0]);
        String outputFormat = request.getOutputFormat();

        OgrFormat format = formats.get(outputFormat);
        if (format == null)
            throw new WFSException("Unknown output format " + outputFormat);

        // create the first temp directory, used for dumping gs generated
        // content
        File tempGS = org.geoserver.data.util.IOUtils.createTempDirectory("ogrtmpin");
        File tempOGR = org.geoserver.data.util.IOUtils.createTempDirectory("ogrtmpout");

        // build the ogr wrapper used to run the ogr2ogr commands
        OGRWrapper wrapper = new OGRWrapper(ogrExecutable, gdalData);

        // actually export each feature collection
        try {
            Iterator outputFeatureCollections = featureCollection.getFeature().iterator();
            SimpleFeatureCollection curCollection;

            File outputFile = null;
            while (outputFeatureCollections.hasNext()) {
                curCollection = (SimpleFeatureCollection) outputFeatureCollections
                        .next();
                
                // write out the gml
                File intermediate = writeToDisk(tempGS, curCollection);

                // convert with ogr2ogr
                final SimpleFeatureType schema = curCollection.getSchema();
                final CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
                outputFile = wrapper.convert(intermediate, tempOGR, schema.getTypeName(), format, crs);

                // wipe out the input dir contents
                IOUtils.emptyDirectory(tempGS);
            }
            
            // was is a single file output?
            if(format.singleFile && featureCollection.getFeature().size() == 1) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(outputFile);
                    org.apache.commons.io.IOUtils.copy(fis, output);
                } finally {
                    if(fis != null) {
                        fis.close();
                    }
                }
            } else {
                // scan the output directory and zip it all
                ZipOutputStream zipOut = null;
                try {
                    zipOut = new ZipOutputStream(output);
                    IOUtils.zipDirectory(tempOGR, zipOut, null);
                    zipOut.finish();
                } finally {
                    org.apache.commons.io.IOUtils.closeQuietly(zipOut);
                }
            }

            // delete the input and output directories
            IOUtils.delete(tempGS);
            IOUtils.delete(tempOGR);
        } catch (Exception e) {
            throw new ServiceException("Exception occurred during output generation", e);
        }
    }
    
    /**
     * Writes to disk using shapefile if the feature type allows for it, GML otherwise
     * @param tempDir
     * @param curCollection
     * @return
     */
    private File writeToDisk(File tempDir,
            SimpleFeatureCollection curCollection) throws Exception {
        // ogr2ogr cannot handle empty gml collections, but it can handle empty
        // shapefiles
        final SimpleFeatureType originalSchema = curCollection.getSchema();
        if(curCollection.isEmpty()) {
            if(isShapefileCompatible(originalSchema)) {
                return writeShapefile(tempDir, curCollection);
            } else {
                SimpleFeatureType simplifiedShema = buildShapefileCompatible(originalSchema);
                return writeShapefile(tempDir, new EmptyFeatureCollection(simplifiedShema));
            }
        }
        
        // create the temp file for this output
        File outFile = new File(tempDir, originalSchema.getTypeName() + ".gml");

        // write out
        OutputStream os = null;
        try {
            os = new FileOutputStream(outFile);

            // let's invoke the transformer
            FeatureTransformer ft = new FeatureTransformer();
            ft.setNumDecimals(16);
            ft.getFeatureNamespaces().declarePrefix("gs",
                    originalSchema.getName().getNamespaceURI());
            ft.transform(curCollection, os);
        } finally {
            os.close();
        }

        return outFile;
    }
    
    private SimpleFeatureType buildShapefileCompatible(SimpleFeatureType originalSchema) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(originalSchema.getName());
        // add the fake geometry
        tb.add("the_geom", Point.class, originalSchema.getCoordinateReferenceSystem());
        // preserve all othr attributes
        for (AttributeDescriptor at : originalSchema.getAttributeDescriptors()) {
            if(!(at instanceof GeometryDescriptor)) {
                tb.add(at);
            }
        }
        return tb.buildFeatureType();
    }

    /**
     * Returns true if the schema has just one geometry and the geom type is known
     * @param schema
     * @return
     */
    private boolean isShapefileCompatible(SimpleFeatureType schema) {
        GeometryType gt = null;
        for (AttributeDescriptor at : schema.getAttributeDescriptors()) {
            if(at instanceof GeometryDescriptor) {
                if(gt == null)
                    gt = ((GeometryDescriptor) at).getType();
                else
                    // more than one geometry 
                    return false;
            }
        } 
        
        return gt != null && SHAPEFILE_GEOM_TYPES.contains(gt.getBinding()); 
    }
    
    private File writeShapefile(File tempDir,
            SimpleFeatureCollection collection) {
        SimpleFeatureType schema = collection.getSchema();

        SimpleFeatureStore fstore = null;
        DataStore dstore = null;
        File file = null;
        try {
            file = new File(tempDir, schema.getTypeName() + ".shp");
            dstore = new ShapefileDataStore(file.toURL());
            dstore.createSchema(schema);
            
            fstore = (SimpleFeatureStore) dstore.getFeatureSource(schema.getTypeName());
            fstore.addFeatures(collection);
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING,
                "Error while writing featuretype '" + schema.getTypeName() + "' to shapefile.", ioe);
            throw new ServiceException(ioe);
        } finally {
            if(dstore != null) {
                dstore.dispose();
            }
        }
        
        return file; 
    }
    
    @Override
    public List<String> getCapabilitiesElementNames() {
        return getAllCapabilitiesElementNames();
    }

}
