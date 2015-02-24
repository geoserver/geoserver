/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.validation.FeatureValidation;
import org.geotools.validation.IntegrityValidation;
import org.geotools.validation.PlugIn;
import org.geotools.validation.Validation;
import org.geotools.validation.ValidationProcessor;
import org.geotools.validation.dto.ArgumentDTO;
import org.geotools.validation.dto.PlugInDTO;
import org.geotools.validation.dto.TestDTO;
import org.geotools.validation.dto.TestSuiteDTO;
import org.geotools.validation.xml.ValidationException;
import org.geotools.validation.xml.XMLReader;


/**
 * GeoValidator purpose.
 * <p>
 * Description of GeoValidator ...
 * </p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: jive $ (last modification)
 * @version $Id$
 */
public class GeoValidator extends ValidationProcessor {
    public static final String WEB_CONTAINER_KEY = "GeoValidator";

    /**
     * GeoValidator constructor.
     * <p>
     * super();
     * </p>
     *
     */
    public GeoValidator() {
        super();
    }

    /**
     * Creates a new geo validator.
     *
     * @param config The configuration module.
     */
    public GeoValidator(GeoServerResourceLoader resourceLoader) {
        loadPlugins(resourceLoader);
    }

    /**
     * Loads validations plugins.
     *
     * @param dataDir The data directory.
     */
    protected void loadPlugins(GeoServerResourceLoader loader) {
        Map plugIns = null;
        Map testSuites = null;

        try {
            File plugInDir = loader.get("plugIns").dir();
            File validationDir = loader.get("validation").dir();
            if (plugInDir != null && plugInDir.exists()) {
                plugIns = XMLReader.loadPlugIns(plugInDir);

                if (validationDir != null && validationDir.exists()) {
                    testSuites = XMLReader.loadValidations(validationDir, plugIns);
                }
            } 
        } catch (Exception e) {
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.global").log(Level.WARNING, "loading plugins", e);
        }
        
        if(testSuites == null)
            testSuites = new HashMap();
        if(plugIns == null)
            plugIns = new HashMap();

        load(testSuites, plugIns);
    }

    /**
     * ValidationProcessor constructor.
     *
     * <p>
     * Builds a ValidationProcessor with the DTO provided.
     * </p>
     *
     * @see load(Map,Map)
     * @param testSuites Map a map of names -> TestSuiteDTO objects
     * @param plugIns Map a map of names -> PlugInDTO objects
     */
    public GeoValidator(Map testSuites, Map plugIns) {
        super();
        load(testSuites, plugIns);
    }

    private Map testSuites;
    private Map plugIns;
    private Map errors;

    /**
     * Map of errors encountered during loading process
     * <p>
     * Map of true (loaded), false (never used), or exception (error) keyed
     * by PlugIn and Test DataTransferObjects.
     * </p>
     * @return Map of status by PlugInDTO and TestDTO
     */
    public Map getErrors() {
        return errors;
    }

    /**
     * load purpose.
     * <p>
     * loads this instance data into this instance.
     * </p>
     * @param testSuites
     * @param plugIns
     */
    public void load(Map testSuites, Map plugIns) {
        this.plugIns = plugIns;
        this.testSuites = testSuites;
        errors = new HashMap();

        // step 1 make a list required plug-ins
        Set plugInNames = new HashSet();
        Iterator i = testSuites.keySet().iterator();

        while (i.hasNext()) {
            TestSuiteDTO dto = (TestSuiteDTO) testSuites.get(i.next());
            Iterator j = dto.getTests().keySet().iterator();

            while (j.hasNext()) {
                TestDTO tdto = (TestDTO) dto.getTests().get(j.next());
                plugInNames.add(tdto.getPlugIn().getName());
            }
        }

        // Mark all plug-ins as not loaded
        //
        i = plugIns.values().iterator();

        while (i.hasNext()) {
            PlugInDTO dto = (PlugInDTO) i.next();
            errors.put(dto, Boolean.FALSE);
        }

        // step 2 configure plug-ins with defaults
        Map defaultPlugIns = new HashMap(plugInNames.size());
        i = plugInNames.iterator();

        while (i.hasNext()) {
            String plugInName = (String) i.next();
            PlugInDTO dto = (PlugInDTO) plugIns.get(plugInName);
            Class plugInClass = null;

            try {
                plugInClass = Class.forName(dto.getClassName());
            } catch (ClassNotFoundException e) {
                //Error, using default.
                errors.put(dto, e);
                e.printStackTrace();
            }

            if (plugInClass == null) {
                plugInClass = Validation.class;
            }

            Map plugInArgs = dto.getArgs();

            if (plugInArgs == null) {
                plugInArgs = new HashMap();
            }

            try {
                PlugIn plugIn = new org.geotools.validation.PlugIn(plugInName, plugInClass,
                        dto.getDescription(), plugInArgs);
                defaultPlugIns.put(plugInName, plugIn);
            } catch (ValidationException e) {
                e.printStackTrace();
                // Update dto entry w/ an error?
                errors.put(dto, e);

                continue;
            }

            // mark dto entry as a success
            errors.put(dto, Boolean.TRUE);
        }

        // step 3 configure plug-ins with tests + add to processor
        i = testSuites.keySet().iterator();

        while (i.hasNext()) {
            TestSuiteDTO tdto = (TestSuiteDTO) testSuites.get(i.next());
            Iterator j = tdto.getTests().keySet().iterator();

            while (j.hasNext()) {
                TestDTO dto = (TestDTO) tdto.getTests().get(j.next());

                // deal with test
                Map testArgs = dto.getArgs();

                if (testArgs == null) {
                    testArgs = new HashMap();
                } else {
                    Map m = new HashMap();
                    Iterator k = testArgs.keySet().iterator();

                    while (k.hasNext()) {
                        ArgumentDTO adto = (ArgumentDTO) testArgs.get(k.next());
                        m.put(adto.getName(), adto.getValue());
                    }

                    testArgs = m;
                }

                try {
                    PlugIn plugIn = (org.geotools.validation.PlugIn) defaultPlugIns.get(dto.getPlugIn()
                                                                                           .getName());
                    Validation validation = plugIn.createValidation(dto.getName(),
                            dto.getDescription(), testArgs);

                    if (validation instanceof FeatureValidation) {
                        addValidation((FeatureValidation) validation);
                    }

                    if (validation instanceof IntegrityValidation) {
                        addValidation((IntegrityValidation) validation);
                    }
                } catch (ValidationException e) {
                    e.printStackTrace();
                    // place test error under the plugIn DTO that spawned it
                    errors.put(dto, e);

                    //error should log here
                    continue;
                }

                errors.put(dto, Boolean.TRUE);
            }

            errors.put(tdto, Boolean.TRUE);
        }
    }

    public Object toPlugInDTO() {
        return plugIns;
    }

    public Object toTestSuiteDTO() {
        return testSuites;
    }

    public Map getPlugIns() {
        return plugIns;
    }

    public Map getTestSuites() {
        return testSuites;
    }
}
