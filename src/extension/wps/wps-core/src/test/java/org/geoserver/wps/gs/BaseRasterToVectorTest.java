package org.geoserver.wps.gs;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.MockData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.util.Utilities;

public abstract class BaseRasterToVectorTest extends WPSTestSupport {

	static final double EPS = 1e-6;
	public static QName RESTRICTED = new QName(MockData.SF_URI, "restricted", MockData.SF_PREFIX);
	public static QName DEM = new QName(MockData.SF_URI, "sfdem", MockData.SF_PREFIX);
	public static QName TASMANIA_BM_ZONES = new QName(MockData.SF_URI, "BmZones",
	            MockData.SF_PREFIX);

	public BaseRasterToVectorTest() {
		super();
	}

	@Override
	protected void setUpInternal() throws Exception {
	    // init xmlunit
	    Map<String, String> namespaces = new HashMap<String, String>();
	    namespaces.put("wps", "http://www.opengis.net/wps/1.0.0");
	    namespaces.put("ows", "http://www.opengis.net/ows/1.1");
	    namespaces.put("gml", "http://www.opengis.net/gml");
	    namespaces.put("wfs", "http://www.opengis.net/wfs");
	    namespaces.put("xlink", "http://www.w3.org/1999/xlink");
	    namespaces.put("feature", "http://cite.opengeospatial.org/gmlsf");
	
	    XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
	}

	@Override
	protected void populateDataDirectory(MockData dataDirectory) throws Exception {
	    super.populateDataDirectory(dataDirectory);
	    dataDirectory.addWcs11Coverages();
	    dataDirectory.addPropertiesType(RESTRICTED,
	            getClass().getResource("restricted.properties"), Collections.singletonMap(
	                    MockData.KEY_SRS_NUMBER, "EPSG:26713"));
	    dataDirectory.addPropertiesType(TASMANIA_BM_ZONES, getClass().getResource(
	            "tazdem_zones.properties"), Collections.singletonMap(MockData.KEY_SRS_NUMBER,
	            "EPSG:26713"));
	    dataDirectory.addCoverage(DEM, getClass().getResource("sfdem.tiff"), MockData.TIFF, null);
	}

	/**
	 * This method takes the input {@link SimpleFeatureCollection} and transforms it into a shapefile 
	 * using the provided file. 
	 * 
	 * <p>
	 * Make sure the provided files ends with .shp.
	 * 
	 * @param fc the {@link SimpleFeatureCollection} to be encoded as a shapefile.
	 * @param destination the {@link File} where we want to write the shapefile.
	 * @throws IOException in case an {@link IOException} is thrown by the underlying code.
	 */
	protected static void featureCollectionToShapeFile(final SimpleFeatureCollection fc, final File destination)
			throws IOException {
		
		//
		//checks
		//
		org.geotools.util.Utilities.ensureNonNull("fc", fc);
		Utilities.ensureNonNull("destination", destination);
		//checks on the file
		if(destination.exists()){
			
			if(destination.isDirectory())
				throw new IOException("The provided destination maps to a directory:"+destination);
			
			if(!destination.canWrite())
				throw new IOException("The provided destination maps to an existing file that cannot be deleted:"+destination);
			
			if(!destination.delete())
				throw new IOException("The provided destination maps to an existing file that cannot be deleted:"+destination);
		}
		
		// real work
		final DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", destination.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
	
		ShapefileDataStore store=null;
		Transaction transaction=null;
		try{
			store = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			store.createSchema(fc.getSchema());
			
			final SimpleFeatureStore featureStore =(SimpleFeatureStore) store.getFeatureSource(fc.getSchema().getName());
			transaction=featureStore.getTransaction();
			
			featureStore.addFeatures(fc);		
		}catch (IOException e) {
			e.printStackTrace();
		}finally{
			
			if(transaction!=null){
	
				transaction.commit();
				transaction.close();	
			}
			
			if(store!=null){
				store.dispose();
			}
		}
	}

}