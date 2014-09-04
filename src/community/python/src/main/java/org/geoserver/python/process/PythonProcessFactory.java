/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.process;

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.python.Python;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.SoftValueHashMap;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

public class PythonProcessFactory implements ProcessFactory {

    Python py;
    SoftValueHashMap<Name, PythonProcessAdapter> processes = new SoftValueHashMap(10);
    
    public Set<Name> getNames() {
        Python.LOGGER.fine("Performning process lookup");
        Set<Name> names = new TreeSet<Name>();
        
        Python py = py();
        try {
            File processRoot = py.getProcessRoot();
            for (String file : processRoot.list()) {
                if (file.endsWith(".py")) {
                    File f = new File(processRoot, file);
                    PythonProcessAdapter adapter = new PythonProcessAdapter(f, py);
                    
                    Python.LOGGER.finest("Examining "+f.getAbsolutePath()+" for process functions");
                    for(String n : adapter.getNames()) {
                        Python.LOGGER.finest("Found process function " + n);
                        names.add(new NameImpl(FilenameUtils.getBaseName(file), n));
                    }
                }
            }
        }
        catch (IOException e) {
            Python.LOGGER.log(Level.WARNING, "Error looking up processes", e);
        }
        return names;
    }

    public InternationalString getTitle() {
        return new SimpleInternationalString("python");
    }

    public InternationalString getTitle(Name name) {
        PythonProcessAdapter adapter = process(name);
        return new SimpleInternationalString(adapter.getTitle(name.getLocalPart()));
    }
    
    public String getVersion(Name name) {
        PythonProcessAdapter adapter = process(name);
        return adapter.getVersion(name.getLocalPart());
    }

    public InternationalString getDescription(Name name) {
        PythonProcessAdapter adapter = process(name);
        return new SimpleInternationalString(adapter.getDescription(name.getLocalPart()));
    }
    
    public Map<String, Parameter<?>> getParameterInfo(Name name) {
        return process(name).getInputParameters(name.getLocalPart());
    }
    
    public Map<String, Parameter<?>> getResultInfo(Name name, Map<String, Object> parameters)
        throws IllegalArgumentException {
        
        return process(name).getOutputParameters(name.getLocalPart());
    }
    
    public boolean supportsProgress(Name name) {
        return false;
    }
    
    public Process create(Name name) {
        return new PythonProcess(name.getLocalPart(), process(name));
    }
    
    public boolean isAvailable() {
        return py() != null;
    }

    public Map<Key, ?> getImplementationHints() {
        return null;
    }
    
    Python py() {
        if (py == null) {
            synchronized(this) {
                if (py == null) {
                    py = GeoServerExtensions.bean(Python.class);
                }
            }
        }
        return py;
    }
    
    PythonProcessAdapter process(Name name) {
        PythonProcessAdapter adapter = processes.get(name);
        if (adapter == null) {
            synchronized(this) {
                adapter = processes.get(name);
                if (adapter == null) {
                    try {
                        adapter = createProcessAdapter(name);
                    } 
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    processes.put(name, adapter);
                }
            }
        }
        return adapter;
    }

    PythonProcessAdapter createProcessAdapter(Name name) throws IOException {
        File f = new File(py().getProcessRoot(), name.getNamespaceURI() + ".py");
        if (f.exists()) {
            return new PythonProcessAdapter(f, py());    
        }
        throw new FileNotFoundException(f.getAbsolutePath());
    }

}
