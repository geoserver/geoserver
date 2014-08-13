/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.config.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.geotools.validation.dto.TestDTO;
import org.geotools.validation.dto.TestSuiteDTO;


/**
 * TestSuiteConfig purpose.
 *
 * <p>
 * Used to represent a copy of the config information required for the UI.
 * </p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public class TestSuiteConfig {
    public static final String CONFIG_KEY = "Validation.TestSuite";
    public static final String CURRENTLY_SELECTED_KEY = "selectedTestSuite";

    /** the test suite name */
    private String name;

    /** the test suite description */
    private String description;

    /** the list of tests - should never be null */
    private Map tests;

    /**
     * TestSuiteConfig constructor.
     * <p>
     * Creates a blank HashMap for tests
     * </p>
     *
     */
    public TestSuiteConfig() {
        tests = new HashMap();
    }

    /**
     * TestSuiteConfig constructor.
     *
     * <p>
     * Creates a copy of the TestSuiteConfig passed in.
     * </p>
     *
     * @param ts The Test Suite to copy
     */
    public TestSuiteConfig(TestSuiteConfig ts) {
        name = ts.getName();
        description = ts.getDescription();
        tests = new HashMap();

        Iterator i = ts.getTests().keySet().iterator();

        while (i.hasNext()) {
            TestConfig t = (TestConfig) ts.getTests().get(i.next());
            tests.put(t.getName(), new TestConfig(t));
        }
    }

    /**
     * TestSuiteConfig constructor.
     *
     * <p>
     * Creates a copy of the TestSuiteConfig passed in.
     * </p>
     *
     * @param ts The Test Suite to copy
     */
    public TestSuiteConfig(TestSuiteDTO ts, Map plugInConfigs) {
        name = ts.getName();
        description = ts.getDescription();
        tests = new HashMap();

        Iterator i = ts.getTests().keySet().iterator();

        while (i.hasNext()) {
            TestDTO t = (TestDTO) ts.getTests().get(i.next());
            tests.put(t.getName(), new TestConfig(t, plugInConfigs));
        }
    }

    /**
     * Implementation of clone.
     *
     * @return An instance of TestSuiteConfig.
     *
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        return new TestSuiteConfig(this);
    }

    public int hashCode() {
        int r = 1;

        if (tests != null) {
            r *= tests.hashCode();
        }

        if (name != null) {
            r *= name.hashCode();
        }

        if (description != null) {
            r *= description.hashCode();
        }

        return r;
    }

    /**
     * Implementation of equals.
     *
     * @param obj An object to compare for equality.
     *
     * @return true when the objects have the same data in the same order.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof TestSuiteDTO)) {
            return false;
        }

        boolean r = true;
        TestSuiteDTO ts = (TestSuiteDTO) obj;

        if (name != null) {
            r = r && (name.equals(ts.getName()));
        }

        if (description != null) {
            r = r && (description.equals(ts.getDescription()));
        }

        if (tests == null) {
            if (ts.getTests() != null) {
                return false;
            }
        } else {
            if (ts.getTests() != null) {
                r = r && tests.equals(ts.getTests());
            } else {
                return false;
            }
        }

        return r;
    }

    /**
     * toDTO purpose.
     * <p>
     * Clones this config as a DTO.
     * </p>
     * @see java.lang.Object#clone()
     * @param plugIns Map of PlugInDTO objects
     * @return TestSuiteDTO
     */
    public TestSuiteDTO toDTO(Map plugIns) {
        TestSuiteDTO ts = new TestSuiteDTO();
        ts.setName(name);
        ts.setDescription(description);

        Map myTests = new HashMap();

        Iterator i = this.tests.keySet().iterator();

        while (i.hasNext()) {
            TestConfig t = (TestConfig) this.tests.get(i.next());
            myTests.put(t.getName(), t.toDTO(plugIns));
        }

        ts.setTests(myTests);

        return ts;
    }

    /**
     * Access description property.
     *
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set description to description.
     *
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Access name property.
     *
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set name to name.
     *
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Access tests property.
     *
     * @return Returns the tests.
     */
    public Map getTests() {
        return tests;
    }

    public Object removeTest(String name) {
        return tests.remove(name);
    }

    public void addTest(TestConfig test) {
        tests.put(test.getName(), test);
    }

    /**
     * Set tests to tests.
     *
     * @param tests The tests to set.
     */
    public void setTests(Map tests) {
        this.tests = tests;
    }
}
