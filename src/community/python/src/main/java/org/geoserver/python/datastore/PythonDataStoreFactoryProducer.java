/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.datastore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.data.DataAccessFactoryProducer;
import org.geoserver.python.Python;
import org.geotools.data.DataAccessFactory;
import static org.geoserver.python.Python.LOGGER;

public class PythonDataStoreFactoryProducer implements DataAccessFactoryProducer {

    Python py;
    
    public PythonDataStoreFactoryProducer(Python py) {
        this.py = py;
    }
    
    public List<DataAccessFactory> getDataStoreFactories() {
        List<DataAccessFactory> factories = new ArrayList();
        
        try {
            File dir = py.getDataStoreRoot();
            for (File f : dir.listFiles()) {
                if ("py".equals(FilenameUtils.getExtension(f.getName()))) {
                    try {
                        factories.add(new PythonDataStoreFactory(new PythonDataStoreAdapter(f, py)));
                    }
                    catch(Exception e) {
                        LOGGER.log(Level.WARNING, "Unable to load data store from " 
                            + f.getAbsolutePath(), e);
                    }
                }
            }
        } 
        
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "", e);
        }
        
        return factories;
    }

}
