/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import static org.geoserver.geopkg.GeoPkg.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * 
 * WFS GetFeature OutputFormat for GeoPackage
 * 
 * @author Niels Charlier
 *
 */
public class GeoPackageGetFeatureOutputFormat extends WFSGetFeatureOutputFormat {

    public GeoPackageGetFeatureOutputFormat(GeoServer gs) {
        super(gs, Sets.union(Sets.newHashSet(MIME_TYPE), Sets.newHashSet(NAMES)));
    }

    @Override
    public String getMimeType(Object value, Operation operation)
            throws ServiceException {
        return MIME_TYPE;
    }

    @Override
    public String getCapabilitiesElementName() {
        return NAMES.iterator().next();
    }

    @Override
    public List<String> getCapabilitiesElementNames() {
        return Lists.newArrayList(NAMES);
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        GetFeatureRequest req = GetFeatureRequest.adapt(operation.getParameters()[0]);

        return Joiner.on("_").join(Iterables.transform(req.getQueries(), new Function<Query,String>() {
            @Override
            public String apply(Query input) {
                return input.getTypeNames().get(0).getLocalPart();
            }
        })) + "." + EXTENSION;
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
