/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.config.validation;

import java.beans.PropertyDescriptor;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.geotools.validation.dto.ArgumentDTO;
import org.geotools.validation.dto.PlugInDTO;
import org.geotools.validation.dto.TestDTO;
import org.geotools.validation.xml.ArgHelper;
import org.geotools.validation.xml.ValidationException;


/**
 * TestConfig purpose.
 *
 * <p>
 * Used to represent a copy of the config information required for the UI.
 * </p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public class TestConfig {
    public final static String CURRENTLY_SELECTED_KEY = "selectedTest";

    /** the test name */
    private String name;

    /** the test description */
    private String description;

    /**
     * The plug-in which contains the class definition and default runtime
     * values
     */
    private PlugInConfig plugIn;

    /** lazily loaded */
    private PropertyDescriptor[] pds;

    /**
     * The set of runtime args for this particular test to override the
     * defaults in the plug-in
     */
    private Map args;

    /**
     * TestConfig constructor.
     *
     * <p>
     * Does nothing
     * </p>
     */
    public TestConfig() {
        args = new HashMap();
    }

    /**
     * TestConfig constructor.
     *
     * <p>
     * Creates a copy from the TestConfig specified.
     * </p>
     *
     * @param t the data to copy
     */
    public TestConfig(TestConfig t) {
        name = t.getName();
        description = t.getDescription();
        plugIn = new PlugInConfig(t.getPlugIn());
        args = new HashMap();

        if (t.getArgs() != null) {
            Iterator i = t.getArgs().keySet().iterator();

            while (i.hasNext()) {
                String key = (String) i.next();

                //TODO clone value.
                args.put(key, new ArgumentConfig((ArgumentConfig) t.getArgs().get(key)));
            }
        }
    }

    /**
     * TestConfig constructor.
     *
     * <p>
     * Creates a copy from the TestDTO specified.
     * </p>
     *
     * @param t the data to copy
     */
    public TestConfig(TestDTO t, Map plugInConfigs) {
        name = t.getName();
        description = t.getDescription();
        plugIn = (PlugInConfig) plugInConfigs.get(t.getPlugIn().getName());
        args = new HashMap();

        if (t.getArgs() != null) {
            Iterator i = t.getArgs().keySet().iterator();

            while (i.hasNext()) {
                String key = (String) i.next();

                //TODO clone value.
                args.put(key, new ArgumentConfig((ArgumentDTO) t.getArgs().get(key)));
            }
        }
    }

    /**
     * Implementation of clone.
     *
     * @return A copy of this TestConfig
     *
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        return new TestConfig(this);
    }

    /**
     * Implementation of equals.
     *
     * @param obj
     *
     * @return true when they have the same data.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof TestDTO)) {
            return false;
        }

        TestDTO t = (TestDTO) obj;
        boolean r = true;

        if (name != null) {
            r = r && (name.equals(t.getName()));
        }

        if (description != null) {
            r = r && (description.equals(t.getDescription()));
        }

        if (plugIn == null) {
            if (t.getPlugIn() != null) {
                return false;
            }
        } else {
            if (t.getPlugIn() != null) {
                r = r && plugIn.equals(t.getPlugIn());
            } else {
                return false;
            }
        }

        if (args == null) {
            if (t.getArgs() != null) {
                return false;
            }
        } else {
            if (t.getArgs() != null) {
                r = r && args.equals(t.getArgs());
            } else {
                return false;
            }
        }

        return r;
    }

    /**
     * Implementation of hashCode.
     *
     * @return int hashcode
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int r = 1;

        if (name != null) {
            r *= name.hashCode();
        }

        if (description != null) {
            r *= description.hashCode();
        }

        if (plugIn != null) {
            r *= plugIn.hashCode();
        }

        if (args != null) {
            r *= args.hashCode();
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
     * @return TestDTO
     */
    public TestDTO toDTO(Map plugIns) {
        TestDTO dto = new TestDTO();

        dto.setName(name);
        dto.setDescription(description);
        dto.setPlugIn((PlugInDTO) plugIns.get(plugIn.getName()));

        Map myArgs = new HashMap();

        if (this.args != null) {
            Iterator i = this.args.keySet().iterator();

            while (i.hasNext()) {
                String key = (String) i.next();
                myArgs.put(key, ((ArgumentConfig) this.args.get(key)).toDTO());
            }
        }

        dto.setArgs(myArgs);

        return dto;
    }

    /**
     * Access args property.
     *
     * @return Returns the args.
     */
    public Map getArgs() {
        return args;
    }

    /**
     * Set args to args.
     *
     * @param args The args to set.
     */
    public void setArgs(Map args) {
        Iterator i = args.keySet().iterator();

        while (i.hasNext())

            if (((ArgumentConfig) args.get(i.next())).isFinal()) {
                throw new IllegalArgumentException(
                    "Cannot include final arguments as part of a test.");
            }

        this.args = args;
    }

    /**
     * getArgStringValue purpose.
     * <p>
     * Returns a human friendly version
     * </p>
     * @param name
     * @return
     */
    public String getArgStringValue(String name) {
        ArgumentConfig ac = (ArgumentConfig) args.get(name);

        if (ac == null) {
            return null;
        }

        return ArgHelper.getArgumentStringEncoding(ac.getValue());
    }

    /**
     * getArgValue purpose.
     * <p>
     * Returns an Object version
     * </p>
     * @param name
     * @return
     */
    public Object getArgValue(String name) {
        ArgumentConfig ac = (ArgumentConfig) args.get(name);

        if (ac == null) {
            return null;
        }

        return ac.getValue();
    }

    /**
     * setArgStringValue purpose.
     * <p>
     * Stores a human friendly version. If this is a new Argument, then the type is String.
     * </p>
     * @param name
     * @param value
     * @return
     */
    public boolean setArgStringValue(String name, String value) {
        ArgumentConfig ac = (ArgumentConfig) args.get(name);

        if ((value == null) || value.equals("")) {
            args.remove(name);

            return true;
        }

        if (ac == null) {
            return addArgStringValue(name, value);
        } else {
            if (ac.isFinal()) {
                throw new IllegalArgumentException(
                    "Cannot include final arguments as part of a test.");
            }

            StringReader sr = new StringReader(value);

            try {
                ac.setValue(ArgHelper.getArgumentInstance(ArgHelper.getArgumentType(ac.getValue()),
                        value));

                return true;
            } catch (Exception e) {
                e.printStackTrace();

                // error, log it
                return false;
            }
        }
    }

    /**
     * setArgStringValue purpose.
     * <p>
     * Stores a human friendly version.
     * </p>
     * @param name
     * @param value
     * @return
     */
    public boolean addArgStringValue(String name, String value) {
        PropertyDescriptor pd = getPropertyDescriptor(name);

        if (pd == null) {
            return false;
        }

        if ((value == null) || value.equals("")) {
            args.remove(name);

            return false;
        }

        Class cl = pd.getPropertyType();
        ArgumentConfig ac = new ArgumentConfig();
        ac.setName(name);

        try {
            String argType = ArgHelper.getArgumentType(cl);
            ac.setValue(ArgHelper.getArgumentInstance(argType, value));
        } catch (ValidationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            return false;
        }

        args.put(name, ac);

        return true;
    }

    public Object createArg(String name, String value)
        throws Exception {
        PropertyDescriptor pd = getPropertyDescriptor(name);

        if (pd == null) {
            return null;
        }

        if ((value == null) || value.equals("")) {
            return null;
        }

        Class cl = pd.getPropertyType();

        return ArgHelper.getArgumentInstance(ArgHelper.getArgumentType(cl), value);
    }

    /**
     * setArgStringValue purpose.
     * <p>
     * Stores a human friendly version
     * </p>
     * @param name
     * @param value
     * @return
     */
    public boolean setArgValue(String name, Object value) {
        if ((value == null) || value.equals("")) {
            args.remove(name);

            return true;
        }

        ArgumentConfig ac = (ArgumentConfig) args.get(name);

        if (ac == null) {
            ac = new ArgumentConfig();
            ac.setName(name);
            args.put(name, ac);
        }

        if (ac.isFinal()) {
            throw new IllegalArgumentException("Cannot include final arguments as part of a test.");
        }

        ac.setValue(value);

        return true;
    }

    /**
     * getPropertyDescriptors purpose.
     * <p>
     * Get the descriptors for this plugin's map of attributes
     * </p>
     * @return
     */
    public PropertyDescriptor[] getPropertyDescriptors() {
        if (pds == null) {
            PropertyDescriptor[] completeList = plugIn.getPropertyDescriptors();
            Set these = new HashSet();

            for (int i = 0; i < completeList.length; i++) {
                PropertyDescriptor property = completeList[i];

                if (property.isHidden()) {
                    continue; // only for tool use
                }

                if (property.isExpert()) {
                    continue; // limited to plugin definition
                }

                if (property.getWriteMethod() == null) {
                    continue; // skip read-only properties
                }

                if ("name".equals(property.getName())) {
                    continue; // not handled dynamically
                }

                if ("description".equals(property.getName())) {
                    continue; // not handled dynamically
                }

                these.add(property);
            }

            Object[] ob = these.toArray();
            pds = new PropertyDescriptor[ob.length];

            for (int i = 0; i < ob.length; i++)
                pds[i] = (PropertyDescriptor) ob[i];
        }

        return pds;
    }

    /**
     * PropertyDescriptor purpose.
     * <p>
     * Get the descriptor for this plugin's attribute named
     * </p>
     * @param name
     * @return
     */
    public PropertyDescriptor getPropertyDescriptor(String name) {
        if (name == null) {
            throw new NullPointerException("name must be defined to get a PropertyDescriptor.");
        }

        if (pds == null) {
            pds = getPropertyDescriptors();
        }

        for (int i = 0; i < pds.length; i++) {
            if (name.equals(pds[i].getName())) {
                return pds[i];
            }
        }

        return null;
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
     * Access plugIn property.
     *
     * @return Returns the plugIn.
     */
    public PlugInConfig getPlugIn() {
        return plugIn;
    }

    /**
     * Set plugIn to plugIn.
     *
     * @param plugIn The plugIn to set.
     */
    public void setPlugIn(PlugInConfig plugIn) {
        this.plugIn = plugIn;
    }
}
