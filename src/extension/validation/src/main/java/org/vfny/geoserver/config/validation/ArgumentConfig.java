/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.config.validation;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Locale;

import org.geotools.validation.dto.ArgumentDTO;


/**
 * ArgumentConfig purpose.
 * <p>
 * Description of ArgumentConfig ...
 * </p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public class ArgumentConfig {
    private String name;
    private boolean _final;
    private Object value;

    /**
     * ArgumentConfig constructor.
     * <p>
     * Description
     * </p>
     *
     */
    public ArgumentConfig() {
    }

    public ArgumentConfig(ArgumentConfig dto) {
        name = dto.getName();
        _final = isFinal();
        value = dto.getValue();
    }

    public ArgumentConfig(ArgumentDTO dto) {
        name = dto.getName();
        _final = isFinal();
        value = dto.getValue();
    }

    public Object clone() {
        return new ArgumentConfig(this);
    }

    public boolean equals(Object obj) {
        boolean r = true;

        if ((obj == null) || !(obj instanceof ArgumentConfig)) {
            return false;
        }

        ArgumentConfig dto = (ArgumentConfig) obj;
        r = r && (dto.isFinal() == _final);

        if (name != null) {
            r = r && (name.equals(dto.getName()));
        } else if (dto.getName() != null) {
            return false;
        }

        if (value != null) {
            r = r && (value.equals(dto.getValue()));
        } else if (dto.getValue() != null) {
            return false;
        }

        return r;
    }

    public int hashCode() {
        int r = 1;

        if (name != null) {
            r *= name.hashCode();
        }

        if (value != null) {
            r *= value.hashCode();
        }

        return r;
    }

    public ArgumentDTO toDTO() {
        ArgumentDTO dto = new ArgumentDTO();
        dto.setFinal(_final);
        dto.setName(name);
        dto.setValue(value);

        return dto;
    }

    /**
     * Access _final property.
     *
     * @return Returns the _final.
     */
    public boolean isFinal() {
        return _final;
    }

    /**
     * Set _final to _final.
     *
     * @param _final The _final to set.
     */
    public void setFinal(boolean _final) {
        this._final = _final;
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
     * Access value property.
     *
     * @return Returns the value.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set value to value.
     *
     * @param value The value to set.
     */
    public void setValue(Object value) {
        if (value == null) {
            throw new NullPointerException("value should only be set when it has a value");
        }

        this.value = value;
    }

    /**
     *
     * getDisplayName purpose.
     * <p>
     * This is used to provide the locale to the property descriptor if it is required. This method is thread safe.
     * </p>
     * <p>
     * This method must be both synchornized and static. The global locale is maintained from start to completion of execution, even when an unexpected exception occurs.
     * </p>
     * @param pd PropertyDescriptor to get the display name from
     * @param locale Locale to use if required.
     * @return String the Display Name
     */
    public static synchronized String getDisplayName(PropertyDescriptor pd) {
        String r = "";

        try { // to safely reset the locale.
            r = pd.getDisplayName();
        } finally {
        }

        return r;
    }

    public static synchronized void loadPropertyLists(TestConfig testConfig, Locale lc,
        List attributeKeys, List attributeHelps, List attributeValues) {
        if (!lc.equals(Locale.getDefault())) {
            Locale.setDefault(lc);
            Introspector.flushCaches();
        }

        PropertyDescriptor[] pd = testConfig.getPropertyDescriptors();

        for (int i = 0; i < pd.length; i++) {
            PropertyDescriptor property = pd[i];
            String propertyName = property.getName();
            String displayName = ArgumentConfig.getDisplayName(property);
            String description = ArgumentConfig.getDescription(property);

            attributeKeys.add(propertyName);
            attributeHelps.add(description);
            attributeValues.add(testConfig.getArgStringValue(propertyName));
        }
    }

    /**
     *
     * getDescription purpose.
     * <p>
     * This is used to provide the locale to the property descriptor if it is required. This method is thread safe.
     * </p>
     * <p>
     * This method must be both synchornized and static.
     * </p>
     * @param pd PropertyDescriptor to get the display description from
     * @param locale Locale to use if required.
     * @return String the display description
     */
    public static synchronized String getDescription(PropertyDescriptor pd) {
        String r = "";

        try { // to safely reset the locale.
            r = pd.getShortDescription();
        } finally {
        }

        return r;
    }
}
