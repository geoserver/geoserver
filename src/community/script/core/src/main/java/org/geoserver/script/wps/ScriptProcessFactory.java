/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

import java.awt.RenderingHints.Key;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
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

/** Process factory that creates processes from scripts located in the data directory. */
public class ScriptProcessFactory extends ScriptFactory implements ProcessFactory {

    /** logger */
    static Logger LOGGER = Logging.getLogger(ScriptProcessFactory.class);

    /** softly cached process objects */
    SoftValueHashMap<Name, ScriptProcess> processes = new SoftValueHashMap<Name, ScriptProcess>(10);

    public ScriptProcessFactory() {
        super(null);
    }

    public ScriptProcessFactory(ScriptManager scriptMgr) {
        super(scriptMgr);
    }

    abstract class CollectProcessNames {

        public CollectProcessNames(Resource directory, ScriptManager scriptMgr, Set<Name> names) {
            for (Resource f : directory.list()) {
                if (Resources.isHidden(f) || f.getType() == Type.DIRECTORY) {
                    continue;
                }
                WpsHook hook = scriptMgr.lookupWpsHook(f);
                if (hook == null) {
                    LOGGER.fine("Skipping " + f.name() + ", no hook found");
                } else {
                    // the base name is the process name, the namespace depends on the
                    // condition
                    NameImpl name = new NameImpl(getScriptNamespace(f), getBaseName(f.name()));
                    if (names.contains(name)) {
                        throw new RuntimeException(
                                "Script "
                                        + f.path()
                                        + " conflicts with an already existing process named "
                                        + name);
                    }
                    names.add(name);
                }
            }
        }

        abstract String getScriptNamespace(Resource f);
    }

    public Set<Name> getNames() {
        LOGGER.fine("Performing process lookup");

        ScriptManager scriptMgr = scriptMgr();
        Set<Name> names = new TreeSet<Name>();

        try {
            // load the scripts in the root, the extension is the namespace
            Resource wpsRoot = scriptMgr.wps();
            new CollectProcessNames(wpsRoot, scriptMgr, names) {

                @Override
                String getScriptNamespace(Resource f) {
                    return getExtension(f.name());
                }
            };

            // go over all directories, the directory name is the namespace instead
            for (final Resource directory : wpsRoot.list()) {
                if (Resources.isHidden(directory) || directory.getType() != Type.DIRECTORY) {
                    continue;
                }
                new CollectProcessNames(directory, scriptMgr, names) {

                    @Override
                    String getScriptNamespace(Resource f) {
                        return directory.name();
                    }
                };
            }
        } catch (IOException e) {
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
     * Get description given a process identifier. Returns null if process has no description (WPS
     * spec says process abstract is optional).
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
            synchronized (this) {
                process = processes.get(name);
                if (process == null) {
                    try {
                        ScriptManager scriptMgr = scriptMgr();

                        // see if the process is a root level one
                        String localName = name.getLocalPart();
                        String namespace = name.getNamespaceURI();
                        Resource f = scriptMgr.wps().get(localName + "." + namespace);
                        if (!Resources.exists(f)) {
                            // see if it's nested in a directory then
                            Resource directory = scriptMgr.wps().get(namespace);
                            if (!Resources.exists(directory)) {
                                throw new FileNotFoundException(
                                        "Could not find script file "
                                                + f.name()
                                                + " nor a directory of scripts named "
                                                + directory.name());
                            }
                            Resource script = null;
                            for (Resource file : directory.list()) {
                                if (Resources.isHidden(file) || file.getType() != Type.RESOURCE) {
                                    continue;
                                }
                                if (localName.equals(getBaseName(file.name()))) {
                                    script = file;
                                }
                            }
                            if (script == null) {
                                throw new FileNotFoundException(
                                        "Could not find script file "
                                                + f.name()
                                                + " nor a script named "
                                                + localName
                                                + " in the "
                                                + directory.name()
                                                + " sub-directory");
                            }
                            f = script;
                        }

                        process = new ScriptProcess(name, f, scriptMgr);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    processes.put(name, process);
                }
            }
        }
        return process;
    }
}
