package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.GML;
import org.geotools.GML.Version;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureTypes;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public abstract class AbstractGranuleResource extends CatalogResourceBase {

    protected static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    
    protected CoverageInfo coverage;

    public AbstractGranuleResource(Context context, Request request, Response response, Catalog catalog,
            CoverageInfo coverage) {
        super(context, request, response, SimpleFeatureCollection.class, catalog);
        this.coverage = coverage;
    }
    
    @Override
    public boolean allowDelete() {
        try {
            StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) coverage
                    .getGridCoverageReader(null, null);
            return !reader.isReadOnly();
        } catch(IOException e) {
            throw new RestletException("Failed to determine if the reader index can be written to", 
                    Status.SERVER_ERROR_INTERNAL, e);
        }
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) coverage
                .getGridCoverageReader(null, null);
        String nativeCoverageName = getNativeCoverageName(reader);

        // setup deletion filter
        Query q = getResourceQuery();
        
        // perform the delete
        GranuleStore store = (GranuleStore) reader.getGranules(nativeCoverageName, false);
        int removed = store.removeGranules(q.getFilter());
        if(LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Removed " + removed + " granules from the reader granule store");
        }
    }
    
    /**
     * Sets up the query to identify only the granules pertinent to this resource (and the 
     * eventual request parameters)
     * 
     * @return
     */
    protected abstract Query getResourceQuery();

    @Override
    protected Object handleObjectGet() throws Exception {
        StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) coverage
                .getGridCoverageReader(null, null);
        String nativeCoverageName = getNativeCoverageName(reader);

        GranuleSource source = reader.getGranules(nativeCoverageName, true);
        Query q = getResourceQuery();
        return forceNonNullNamespace(source.getGranules(q));
    }

    private String getNativeCoverageName(StructuredGridCoverage2DReader reader) throws IOException {
        String nativeCoverageName = coverage.getNativeCoverageName();
        if(nativeCoverageName == null) {
            if(reader.getGridCoverageNames().length > 1) {
                throw new IllegalStateException("The grid coverage configuration for " + coverage.getName() 
                        + " does not specify a native coverage name, yet the reader provides more than one coverage. " +
                        "Please assign a native coverage name (the GUI does so automatically)");
            } else {
                nativeCoverageName = reader.getGridCoverageNames()[0];
            }
        }
        return nativeCoverageName;
    }
    
    private SimpleFeatureCollection forceNonNullNamespace(SimpleFeatureCollection features) throws IOException {
        SimpleFeatureType sourceSchema = features.getSchema();
        if(sourceSchema.getName().getNamespaceURI() == null) {
            try {
                String targetNs = "http://www.geoserver.org/rest/granules";
                AttributeDescriptor[] attributes = (AttributeDescriptor[]) sourceSchema.getAttributeDescriptors().toArray(new AttributeDescriptor[sourceSchema.getAttributeDescriptors().size()]);
                SimpleFeatureType targetSchema = FeatureTypes.newFeatureType(attributes, sourceSchema.getName().getLocalPart(), new URI(targetNs));
                RetypingFeatureCollection retyped = new RetypingFeatureCollection(features, targetSchema);
                return retyped;
            } catch(Exception e) {
                throw new IOException("Failed to retype the granules feature schema, in order to force " +
                        "it having a non null namespace", e);
            }
        } else {
            return features;
        }
    }

    /**
     * Creates the list of formats used to serialize and de-serialize instances of the target
     * object.
     * <p>
     * Subclasses may override or extend this method to customize the supported formats. By default
     * this method supports html, xml, and json.
     * </p>
     * 
     * @see #createHTMLFormat()
     * @see #createXMLFormat()
     * @see #createJSONFormat()
     */
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        List<DataFormat> formats = new ArrayList<DataFormat>();
        formats.add(new FeaturesJSONFormat());
        formats.add(new FeaturesGMLFormat());

        return formats;
    }

    /**
     * A format for JSON features
     * 
     * @author Andrea Aime - GeoSolutions
     * 
     */
    public class FeaturesJSONFormat extends StreamDataFormat {
        protected FeaturesJSONFormat() {
            super(MediaType.APPLICATION_JSON);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            throw new UnsupportedOperationException("Can't read JSON documents yet");
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            SimpleFeatureCollection features = (SimpleFeatureCollection) object;
            final FeatureJSON json = new FeatureJSON();
            boolean geometryless = features.getSchema().getGeometryDescriptor() == null;
            json.setEncodeFeatureCollectionBounds(!geometryless);
            json.setEncodeFeatureCollectionCRS(!geometryless);
            json.writeFeatureCollection(features, out);
        }

    }

    /**
     * A format for GML2 features
     * 
     * @author Andrea Aime - GeoSolutions
     * 
     */
    public class FeaturesGMLFormat extends StreamDataFormat {
        protected FeaturesGMLFormat() {
            super(MediaType.TEXT_XML);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            throw new UnsupportedOperationException("Can't read GML documents yet");
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            SimpleFeatureCollection features = (SimpleFeatureCollection) object;
            GML gml = new GML(Version.WFS1_0);
            gml.setNamespace("gf", features.getSchema().getName().getNamespaceURI());
            // gml.setFeatureBounding(false);
            gml.encode(out, features);
        }

    }
    
}
