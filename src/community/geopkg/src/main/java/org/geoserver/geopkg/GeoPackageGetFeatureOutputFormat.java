package org.geoserver.geopkg;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;


public class GeoPackageGetFeatureOutputFormat extends WFSGetFeatureOutputFormat {

    public GeoPackageGetFeatureOutputFormat(GeoServer gs) {
        super(gs, "application/x-sqlite3");
    }
    
    @Override
    protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
            Operation getFeature) throws IOException, ServiceException {
        
        GeoPackage geopkg = new GeoPackage();
        
        for (FeatureCollection collection: featureCollection.getFeatures()) {
            
            FeatureEntry e = new FeatureEntry();
            
            if (! (collection instanceof SimpleFeatureCollection)) {
                throw new ServiceException("GeoPackage OutputFormat does not support Complex Features.");
            }
           
            geopkg.add(e, (SimpleFeatureCollection)  collection);
        }
        
        geopkg.close();
        
        //write to output and delete temporary file
        InputStream temp = new FileInputStream(geopkg.getFile());
        IOUtils.copy(temp, output);
        output.flush();        
        temp.close();
        geopkg.getFile().delete();
    }

}
