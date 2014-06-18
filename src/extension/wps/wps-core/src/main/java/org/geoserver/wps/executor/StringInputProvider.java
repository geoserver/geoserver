/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

/**
 * A input provider for static string data
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class StringInputProvider implements InputProvider {

    String value;

    String inputId;

    public StringInputProvider(String value, String inputId) {
        this.value = value;
        this.inputId = inputId;
    }

    @Override
    public Object getValue() throws Exception {
        return value;
    }

    @Override
    public String getInputId() {
        return inputId;
    }

    @Override
    public boolean resolved() {
        return true;
    }

    @Override
    public boolean longParse() {
        return false;
    }

}
