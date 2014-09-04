/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import static org.geoserver.python.Python.LOGGER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.geoserver.platform.ExtensionProvider;
import org.geoserver.python.Python;

public abstract class PythonOutputFormatProvider<T> implements ExtensionProvider<T> {

    Python py;
    
    protected PythonOutputFormatProvider(Python py) {
        this.py = py;
    }
    
    public List<T> getExtensions(Class<T> extensionPoint) {
        List<T> formats = new ArrayList();
        
        File dir;
        try {
            dir = py.getFormatRoot();
        } 
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        for (File f : dir.listFiles()) {
            try {
                formats.add(createOutputFormat(new PythonVectorFormatAdapter(f, py)));
            }
            catch(Exception e) {
                LOGGER.warning(e.getLocalizedMessage());
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                }
            }
        }
        
        return formats;
    }

    protected abstract T createOutputFormat(PythonFormatAdapter adapter);
}
