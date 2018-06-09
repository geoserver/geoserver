/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.function;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.script.ScriptFactory;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.wps.ScriptProcessFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.filter.FunctionFactory;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;

/** Function factory that creates processes from scripts located in the data directory. */
public class ScriptFunctionFactory extends ScriptFactory implements FunctionFactory {

    /** logger */
    static Logger LOGGER = Logging.getLogger(ScriptProcessFactory.class);

    /** script manager */
    ScriptManager scriptMgr;

    SoftValueHashMap<Name, ScriptFunction> functions = new SoftValueHashMap<Name, ScriptFunction>();

    public ScriptFunctionFactory() {
        super(null);
    }

    public ScriptFunctionFactory(ScriptManager scriptMgr) {
        super(scriptMgr);
    }

    @Override
    public List<FunctionName> getFunctionNames() {
        LOGGER.fine("Performing filter lookup");

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

        ScriptManager scriptMgr = scriptMgr();
        List<FunctionName> names = new ArrayList<FunctionName>();

        try {
            Resource filterRoot = scriptMgr.function();
            for (Resource file : filterRoot.list()) {

                FunctionHook hook = scriptMgr.lookupFilterHook(file);
                if (!Resources.exists(file)) {
                    LOGGER.fine("Skipping " + file.name() + ", no hook found");
                }

                // TODO: support multiple functions in one file
                // TODO: support the function defining its namespace
                names.add(
                        ff.functionName(
                                new NameImpl(getExtension(file.name()), getBaseName(file.name())),
                                -1));
            }
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING, "Error looking up filters", e);
        }
        return names;
    }

    @Override
    public Function function(String name, List<Expression> args, Literal fallback) {
        return function(new NameImpl(name), args, fallback);
    }

    @Override
    public Function function(Name name, List<Expression> args, Literal fallback) {
        ScriptFunction f = function(name);
        return f != null ? f.instance(name, args) : null;
    }

    ScriptFunction function(Name name) {
        ScriptFunction function = functions.get(name);
        if (function == null) {
            synchronized (this) {
                function = functions.get(name);
                if (function == null) {
                    ScriptManager scriptMgr = scriptMgr();

                    Resource filterRoot = scriptMgr.function();
                    Resource f = null;
                    if (name.getNamespaceURI() != null) {
                        f = filterRoot.get(name.getLocalPart() + "." + name.getNamespaceURI());
                    } else {
                        // look for a file based on basename
                        for (Resource file : filterRoot.list()) {
                            if (name.getLocalPart().equals(getBaseName(file.name()))) {
                                f = file;
                                break;
                            }
                        }
                    }

                    if (f == null) {
                        return null;
                    }

                    if (!Resources.exists(f)) {
                        LOGGER.log(Level.WARNING, "File not found : " + f.path());
                        return null;
                    }

                    function = new ScriptFunction(f, scriptMgr);
                    functions.put(name, function);
                }
            }
        }
        return function;
    }
}
