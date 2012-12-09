/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.process;

import java.util.Map;

import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.opengis.util.ProgressListener;

public class PythonProcess implements Process {

    String name;
    PythonProcessAdapter adapter;
    
    public PythonProcess(String name, PythonProcessAdapter adapter) {
        this.name = name;
        this.adapter = adapter;
    }
    
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        
        try {
            return adapter.run(name, input);
        } 
        catch (Exception e) {
            throw new ProcessException(e);
        }
    }

}
