/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2017, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.ppio;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.geoserver.platform.resource.Resource;
import org.geoserver.util.IOUtils;
import org.geoserver.wps.process.AbstractRawData;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.ResourceRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.csv.CSVDataStore;
import org.geotools.data.csv.CSVDataStoreFactory;
import org.geotools.data.csv.CSVFeatureStore;
import org.geotools.data.csv.parse.CSVStrategy;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.logging.Logging;

import net.opengis.wcs11.validation.CoverageSummaryTypeValidator;

/**
 * @author ian
 *
 */
public class CSVPPIO extends CDataPPIO {
    WPSResourceManager resourceManager;
    private static final Logger LOGGER = Logging.getLogger("org.geoserver.wps.ppio.CSVPPIO");

    protected CSVPPIO(WPSResourceManager resourceManager) {
        super(FeatureCollection.class, FeatureCollection.class, "text/csv");
        this.resourceManager = resourceManager;
    }
  


    @Override
    public Object decode(String input) throws Exception {
        return decode(new ByteArrayInputStream(input.getBytes()));
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        Resource tmp = resourceManager.getTemporaryResource(".csv");
        IOUtils.copy(input, tmp.out());
        HashMap<String, Object> params = new HashMap<>();
        params.put(CSVDataStoreFactory.FILE_PARAM.key,tmp.file().getAbsoluteFile());
        params.put(CSVDataStoreFactory.STRATEGYP.key, "CSVAttributesOnlyStrategy");
        CSVDataStore store = (CSVDataStore) DataStoreFinder.getDataStore(params);
        return store.getFeatureSource().getFeatures();
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        Resource tmp = resourceManager.getTemporaryResource(".csv");
        
        HashMap<String, Object> params = new HashMap<>();
        params.put("URL",DataUtilities.fileToURL(tmp.file()));
        CSVDataStore store = (CSVDataStore) DataStoreFinder.getDataStore(params);
        CSVFeatureStore csvFeatureStore = (CSVFeatureStore)store.getFeatureSource();
        csvFeatureStore.addFeatures((FeatureCollection) value);
        
        IOUtils.copy(tmp.in(),os);
        
    }

}
