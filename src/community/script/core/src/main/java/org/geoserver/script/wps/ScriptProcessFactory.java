/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.script.ScriptFactory;
import org.geoserver.script.ScriptManager;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * Process factory that creates processes from scripts located in the data directory.
 */
public class ScriptProcessFactory extends ScriptFactory implements ProcessFactory {

    /** logger */
    static Logger LOGGER = Logging.getLogger(ScriptProcessFactory.class);

    /**
     * softly cached process objects
     */
    SoftValueHashMap<Name, ScriptProcess> processes = new SoftValueHashMap<Name,ScriptProcess>(10);

    public ScriptProcessFactory() {
        super(null);
    }

    public ScriptProcessFactory(ScriptManager scriptMgr) {
        super(scriptMgr);
    }

    public Set<Name> getNames() {
        LOGGER.fine("Performing process lookup");

        ScriptManager scriptMgr = scriptMgr();
        Set<Name> names = new TreeSet<Name>();

        try {
            File wpsRoot = scriptMgr.getWpsRoot();
            for (String file : wpsRoot.list()) {
                File f = new File(wpsRoot, file);
                if (f.isHidden()) {
                    continue;
                }
                WpsHook hook = scriptMgr.lookupWpsHook(f);
                if (hook == null) {
                    LOGGER.fine("Skipping " + f.getName() + ", no hook found");
                } else {
                    //use the extension as the namespace, and the basename as the process name 
                    names.add(new NameImpl(getExtension(f.getName()), getBaseName(f.getName())));

                    //TODO: support the process defining its namespace
                }
            }
        }
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error looking up processes", e);
        }
        return names;
    }

    public InternationalString getTitle() {
        return new SimpleInternationalString("script");
    }

    public InternationalString getTitle(Name name) {
        try {
            return new SimpleInternationalString(process(name).getTitle());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getVersion(Name name) {
        try {
            return process(name).getVersion();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get description given a process identifier.  Returns null if process
     * has no description (WPS spec says process abstract is optional).
     */
    public InternationalString getDescription(Name name) {
        String desc;
        try {
            desc = process(name).getDescription();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return desc != null ? new SimpleInternationalString(desc) : null;
    }
    
    public Map<String, Parameter<?>> getParameterInfo(Name name) {
        try {
            return process(name).getInputs();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public Map<String, Parameter<?>> getResultInfo(Name name, Map<String, Object> parameters)
        throws IllegalArgumentException {
        try {
            return process(name).getOutputs();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean supportsProgress(Name name) {
        return false;
    }
    
    public Process create(Name name) {
        return process(name);
    }
    
    public boolean isAvailable() {
        return true;
    }

    public Map<Key, ?> getImplementationHints() {
        return null;
    }

    ScriptProcess process(Name name) {
        ScriptProcess process = processes.get(name);
        if (process == null) {
            synchronized(this) {
                process = processes.get(name);
                if (process == null) {
                    try {
                        ScriptManager scriptMgr = scriptMgr();

                        File f = new File(scriptMgr.getWpsRoot(), 
                            name.getLocalPart() + "." + name.getNamespaceURI());
                        if (!f.exists()) {
                            throw new FileNotFoundException(f.getPath());
                        }

                        process = new ScriptProcess(name, f, scriptMgr);
                    } 
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    processes.put(name, process);
                }
            }
        }
        return process;
    }
}
