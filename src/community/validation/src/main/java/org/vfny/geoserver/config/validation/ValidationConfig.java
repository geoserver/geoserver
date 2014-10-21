/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.config.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.geotools.validation.dto.PlugInDTO;
import org.geotools.validation.dto.TestSuiteDTO;
import org.vfny.geoserver.global.GeoValidator;


/**
 * ValidationConfig purpose.
 * <p>
 * Description of ValidationConfig ...
 * </p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public class ValidationConfig {
    public static final String CONFIG_KEY = "Validation.Config";
    private Map plugIns;
    private Map testSuites;

    /**
     * ValidationConfig constructor.
     * <p>
     * Description
     * </p>
     *
     */
    public ValidationConfig() {
        super();
        plugIns = new HashMap();
        testSuites = new HashMap();
    }

    /**
     * ValidationConfig constructor.
     * <p>
     * Description
     * </p>
     * @param validator GeoValidator
     */
    public ValidationConfig(GeoValidator validator) {
        this(validator.getPlugIns(), validator.getTestSuites());
    }

    /**
     * ValidationConfig constructor.
     * <p>
     * Description
     * </p>
     * @param plugIns a List of PlugInDTO objects
     * @param testSuites a List of TestSuiteDTO objects
     */
    public ValidationConfig(Map plugIns, Map testSuites) {
        this.plugIns = new HashMap();
        this.testSuites = new HashMap();

        Iterator i = null;
        i = plugIns.keySet().iterator();

        while (i.hasNext()) {
            PlugInDTO dto = (PlugInDTO) plugIns.get(i.next());
            PlugInConfig config = new PlugInConfig(dto);
            this.plugIns.put(config.getName(), config);
        }

        i = testSuites.keySet().iterator();

        while (i.hasNext()) {
            TestSuiteDTO dto = (TestSuiteDTO) testSuites.get(i.next());
            TestSuiteConfig config = new TestSuiteConfig(dto, this.plugIns);
            this.testSuites.put(config.getName(), config);
        }
    }

    /**
     *
     * getPlugIn purpose.
     * <p>
     * Gets a PlugInConfig
     * </p>
     * @param name
     * @return PlugInConfig or null if one does not exist
     */
    public PlugInConfig getPlugIn(String name) {
        if (name == null) {
            return null;
        }

        return (PlugInConfig) plugIns.get(name);
    }

    /**
     *
     * getTestSuite purpose.
     * <p>
     * Gets a TestSuiteConfig
     * </p>
     * @param name
     * @return TestSuiteConfig or null if one does not exist
     */
    public TestSuiteConfig getTestSuite(String name) {
        if (name == null) {
            return null;
        }

        return (TestSuiteConfig) testSuites.get(name);
    }

    /**
     *
     * getTest purpose.
     * <p>
     * Gets a TestConfig
     * </p>
     * @param name
     * @param testSuite
     * @return TestSuiteConfig or null if one does not exist
     */
    public TestConfig getTest(String name, String testSuite) {
        if ((name == null) || (testSuite == null)) {
            return null;
        }

        TestSuiteConfig tcn = getTestSuite(testSuite);

        if (tcn == null) {
            return null;
        }

        return (TestConfig) tcn.getTests().get(name);
    }

    /**
     *
     * addPlugIn purpose.
     * <p>
     * Adds the plugin.
     * </p>
     * @param plugIn
     * @return true
     */
    public boolean addPlugIn(PlugInConfig plugIn) {
        plugIns.put(plugIn.getName(), plugIn);

        return true;
    }

    /**
     *
     * addTest purpose.
     * <p>
     * Adds the test to the specified testSuite.
     * </p>
     * @param test
     * @param testSuite
     * @return true on sucess (requires specified plugin to exist), false otherwise.
     */
    public boolean addTest(TestConfig test, String testSuite) {
        TestSuiteConfig tsc = (TestSuiteConfig) testSuites.get(testSuite);

        if ((tsc != null) && plugIns.containsKey(test.getPlugIn().getName())) {
            tsc.getTests().put(test.getName(), test);

            return true;
        }

        return false;
    }

    public boolean addTestSuite(TestSuiteConfig testSuite) {
        Iterator i = testSuite.getTests().keySet().iterator();

        while (i.hasNext()) {
            TestConfig test = (TestConfig) testSuite.getTests().get(i.next());

            if (!plugIns.containsKey(test.getPlugIn().getName())) {
                return false;
            }
        }

        // plug ins all exist
        testSuites.put(testSuite.getName(), testSuite);

        return true;
    }

    public Object removeTestSuite(String name) {
        return testSuites.remove(name);
    }

    public Object removePlugIn(String name) {
        return plugIns.remove(name);
    }

    public Object removeTest(String testSuite, String name) {
        return ((TestSuiteConfig) testSuites.get(testSuite)).getTests().remove(name);
    }

    /**
     * toDTO purpose.
     * <p>
     * Creates a representation as DTOs
     * </p>
     * @param plugIns List an empty list to store the resulting plugInDTOs
     * @param testSuites List an empty list to store the resulting TestSuiteDTOs
     * @return true if the lists contain the data, false otherwise.
     */
    public boolean toDTO(Map plugIns, Map testSuites) {
        if ((plugIns == null) || (testSuites == null)) {
            return false;
        }

        if ((plugIns.size() != 0) || (testSuites.size() != 0)) {
            return false;
        }

        // list are empty, and exist.
        Iterator i = null;
        i = this.plugIns.keySet().iterator();

        while (i.hasNext()) {
            PlugInDTO dto = ((PlugInConfig) this.plugIns.get(i.next())).toDTO();
            plugIns.put(dto.getName(), dto);
        }

        i = this.testSuites.keySet().iterator();

        while (i.hasNext()) {
            TestSuiteDTO dto = ((TestSuiteConfig) this.testSuites.get(i.next())).toDTO(plugIns);
            testSuites.put(dto.getName(), dto);
        }

        return true;
    }

    /**
     * Access plugIns property.
     *
     * @return Returns the plugIns.
     */
    public Map getPlugIns() {
        return plugIns;
    }

    /**
     * Access plugIns property.
     *
     * @return Returns the plugIns.
     */
    public Set getPlugInNames() {
        return plugIns.keySet();
    }

    /**
     * Set plugIns to plugIns.
     *
     * @param plugIns The plugIns to set.
     */
    public void setPlugIns(Map plugIns) {
        this.plugIns = plugIns;
    }

    /**
     * Access testSuites property.
     *
     * @return Returns the testSuites.
     */
    public Map getTestSuites() {
        return testSuites;
    }

    /**
     * Access testSuites property.
     *
     * @return Returns the testSuites.
     */
    public Set getTestSuiteNames() {
        return testSuites.keySet();
    }

    /**
     * Set testSuites to testSuites.
     *
     * @param testSuites The testSuites to set.
     */
    public void setTestSuites(Map testSuites) {
        this.testSuites = testSuites;
    }
}
