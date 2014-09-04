/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.config.validation;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.geotools.validation.dto.ArgumentDTO;
import org.geotools.validation.dto.PlugInDTO;
import org.geotools.validation.xml.ArgHelper;
import org.geotools.validation.xml.ValidationException;


/**
 * PlugInConfig purpose.
 *
 * <p>
 * Used to represent a copy of the config information required for the UI.
 * </p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public class PlugInConfig {
    public static final String CONFIG_KEY = "Validation.PlugIn";

    /** the plug-in name */
    private String name;

    /** the plug-in description */
    private String description;

    /** the class name this plug-in represents */
    private String className;

    /** the default arguments */
    private Map args;

    /**
     * PlugInConfig constructor.
     *
     * <p>
     * Does nothing.
     * </p>
     */
    public PlugInConfig() {
        args = new HashMap();
    }

    /**
     * PlugInConfig constructor.
     *
     * <p>
     * Creates a copy of the PlugInConfig passed in in this object.
     * </p>
     *
     * @param pi
     */
    public PlugInConfig(PlugInConfig pi) {
        name = pi.getName();
        description = pi.getDescription();
        className = pi.getClassName();
        args = new HashMap();

        if (pi.getArgs() != null) {
            Iterator i = pi.getArgs().keySet().iterator();

            while (i.hasNext()) {
                String key = (String) i.next();

                //TODO clone value.
                args.put(key, new ArgumentConfig((ArgumentConfig) pi.getArgs().get(key)));
            }
        }
    }

    /**
     * PlugInConfig constructor.
     *
     * <p>
     * Creates a copy of the PlugInDTO passed in in this object.
     * </p>
     *
     * @param pi
     */
    public PlugInConfig(PlugInDTO pi) {
        name = pi.getName();
        description = pi.getDescription();
        className = pi.getClassName();
        args = new HashMap();

        if (pi.getArgs() != null) {
            Iterator i = pi.getArgs().keySet().iterator();

            while (i.hasNext()) {
                String key = (String) i.next();

                //TODO clone value.
                args.put(key, new ArgumentConfig((ArgumentDTO) pi.getArgs().get(key)));
            }
        }
    }

    /**
     * Implementation of clone.
     *
     * @return a copy of this class.
     *
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        return new PlugInConfig(this);
    }

    /**
     * Implementation of equals.
     *
     * @param obj
     *
     * @return true when the two objects are equal.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof PlugInDTO)) {
            return false;
        }

        PlugInDTO pi = (PlugInDTO) obj;
        boolean r = true;

        if (name != null) {
            r = r && (name.equals(pi.getName()));
        }

        if (description != null) {
            r = r && (description.equals(pi.getDescription()));
        }

        if (className != null) {
            r = r && (className.equals(pi.getClassName()));
        }

        if (args == null) {
            if (pi.getArgs() != null) {
                return false;
            }
        } else {
            if (pi.getArgs() != null) {
                r = r && args.equals(pi.getArgs());
            } else {
                return false;
            }
        }

        return r;
    }

    /**
     * Implementation of hashCode.
     *
     * @return the hashcode.
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int i = 1;

        if (name != null) {
            i *= name.hashCode();
        }

        if (description != null) {
            i *= description.hashCode();
        }

        if (className != null) {
            i *= className.hashCode();
        }

        if (args != null) {
            i *= args.hashCode();
        }

        return i;
    }

    /**
     * toDTO purpose.
     * <p>
     * Clones this config as a DTO.
     * </p>
     * @see java.lang.Object#clone()
     * @return PlugInDTO
     */
    public PlugInDTO toDTO() {
        PlugInDTO dto = new PlugInDTO();

        dto.setName(name);
        dto.setDescription(description);
        dto.setClassName(className);

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
     * Stores a human friendly version
     * </p>
     * @param name
     * @param value
     * @return
     */
    public boolean setArgStringValue(String name, String value) {
        if ((value == null) || value.equals("")) {
            args.remove(name);

            return true;
        }

        ArgumentConfig ac = (ArgumentConfig) args.get(name);

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
        if ((value == null) || value.equals("")) {
            args.remove(name);

            return false;
        }

        PropertyDescriptor pd = getPropertyDescriptor(name);

        if (pd == null) {
            return false;
        }

        Class cl = pd.getPropertyType();
        ArgumentConfig ac = new ArgumentConfig();
        ac.setName(name);

        try {
            ac.setValue(ArgHelper.getArgumentInstance(ArgHelper.getArgumentType(cl), value));
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
        try {
            Class plugIn = this.getClass().getClassLoader().loadClass(className);
            BeanInfo bi = Introspector.getBeanInfo(plugIn);

            return bi.getPropertyDescriptors();
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
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

        PropertyDescriptor[] pds = getPropertyDescriptors();

        for (int i = 0; i < pds.length; i++) {
            if (name.equals(pds[i].getName())) {
                return pds[i];
            }
        }

        return null;
    }

    /**
     * Access className property.
     *
     * @return Returns the className.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set className to className.
     *
     * @param className The className to set.
     */
    public void setClassName(String className) {
        this.className = className;
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
}
