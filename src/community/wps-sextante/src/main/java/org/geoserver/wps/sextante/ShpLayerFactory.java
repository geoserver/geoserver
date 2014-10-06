/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.resource.ShapefileResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import es.unex.sextante.dataObjects.IVectorLayer;

public class ShpLayerFactory extends DatastoreVectorLayerFactory {
    
    static final Logger LOGGER = Logging.getLogger(ShpLayerFactory.class);

	@Override
	protected IVectorLayer createLayer(DataStore dataStore, String sName,
			Object crs) throws IOException {
		GTShpLayer layer = GTShpLayer.createLayer(dataStore, sName,
				(CoordinateReferenceSystem) crs);
		layer.setName(sName);
		return layer;
	}

	public DataStore createDatastore(String m_sFilename, SimpleFeatureType m_FeatureType) throws IOException {
	    WPSResourceManager manager = GeoServerExtensions.bean(WPSResourceManager.class);
	    
		File directory = IOUtils.createTempDirectory("sxttmp");
        File file = new File(directory, m_sFilename);

        try {
            ShapefileDataStore dataStore = new ShapefileDataStore(DataUtilities.fileToURL(file));
            dataStore.createSchema(m_FeatureType);
            manager.addResource(new ShapefileResource(dataStore, directory));
            
            return dataStore;
        } catch(Throwable t) {
            LOGGER.log(Level.SEVERE, "Could not create shapefile output ", t);
            IOUtils.delete(directory);
            throw (IOException) new IOException(t.getMessage()).initCause(t);
        }
        
		
	}

}
