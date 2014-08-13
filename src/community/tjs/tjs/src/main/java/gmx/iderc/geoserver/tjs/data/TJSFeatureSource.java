package gmx.iderc.geoserver.tjs.data;


import org.geotools.data.*;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;

import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureCollection;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: thijsb
 * Date: 3/25/14
 * Time: 11:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class TJSFeatureSource extends AbstractFeatureSource {           // not ContentFeatureSource  ? AbstractFeatureSource

    TJS_1_0_0_DataStore store;
    String typeName;
    SimpleFeatureType featureType;
    ReferencedEnvelope cacheBounds = null;

    public TJSFeatureSource (TJS_1_0_0_DataStore tjsDataStore, String typeName) {
        this.store = tjsDataStore;
        this.typeName = typeName;
        this.featureType = store.getSchema(typeName);
        this.queryCapabilities = new QueryCapabilities() {
            public boolean isUseProvidedFIDSupported() {
                return false;
            }
        };
    }

    public TJS_1_0_0_DataStore getDataStore(){
        return this.store;
    }

    @Override
    public void addFeatureListener(FeatureListener featureListener) {
        //To change body of implemented methods use File | Settings | File Templates.
        store.listenerManager.addFeatureListener(this, featureListener);
    }

    @Override
    public void removeFeatureListener(FeatureListener featureListener) {
        store.listenerManager.removeFeatureListener(this, featureListener);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return featureType;
    }

   /* protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
        // Note we ignore 'query' because querying/filtering is handled in superclasses.
        // get the reader from the TJSFeatureReader

        // just support one featuretype?

        return tjsDataStore.getFeatureReader(query.getTypeName());
        // return new TJSFeatureReader( getState() );
    }*/

    /**
     * Implementation that generates the total bounds
     * (many file formats record this information in the header)
     */
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        // Just ignore the query for now
        // TODO: implement
        return this.getBounds();
    }

    public final ReferencedEnvelope getBounds ()  throws IOException {
        CoordinateReferenceSystem crs = getCRS();
        if (crs != null ) {
            ReferencedEnvelope bounds  = new ReferencedEnvelope( crs );
            FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = store.getFeatureReader(this.typeName);
            try {
                while( featureReader.hasNext() ){
                    SimpleFeature feature = featureReader.next();
                    bounds.include( feature.getBounds() );
                }
            }
            finally {
                featureReader.close();
            }
            return bounds;
        } else {
            return null;
        }
    }


    public final CoordinateReferenceSystem getCRS() {
        CoordinateReferenceSystem crs = getSchema().getCoordinateReferenceSystem();
        // TODO: the same as the declared crs from the framework?
        if (crs == null)  {
            // first try the declared CRS, if that is not available, try to use the native CRS
            try {
                crs = store.getFrameworkInfo().getFeatureType().getCRS();
                if (crs == null) {
                    crs = store.featureDataStore.getSchema(store.getFrameworkInfo().getFeatureType().getNativeName()).getCoordinateReferenceSystem();
                }
             }  catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return crs;
    }

    // Return the SRS string as declared in the featuretype of the framework
    public final String getSRS() {
        return store.getFrameworkInfo().getFeatureType().getSRS();
    }

    protected int getCountInternal(Query query) throws IOException {
        SimpleFeatureCollection sfc = this.getFeatures(query);
        int count = 0;
        SimpleFeatureIterator iter = sfc.features();
        while(iter.hasNext()) {
            iter.next();
            count++;
        }
        iter.close();
        return count;
    }

}
