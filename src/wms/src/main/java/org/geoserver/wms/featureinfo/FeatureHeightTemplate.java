/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.IOException;

import org.opengis.feature.simple.SimpleFeature;

/**
 * Template supporting height values for features.
 *
 * @author David Winslow <dwinslow@opengeo.org>
 */
public class FeatureHeightTemplate {

    FeatureTemplate delegate;
    
    public FeatureHeightTemplate() {
        this(new FeatureTemplate());
    }
    
    public FeatureHeightTemplate(FeatureTemplate delegate) {
        this.delegate = delegate;
    }
    
    /**
     * Executes the template against the feature.
     * <p>
     * This method returns:
     * <ul>
     *  <li><code>{"01/01/07"}</code>: timestamp as 1 element array
     *  <li><code>{"01/01/07","01/12/07"}</code>: timespan as 2 element array
     *  <li><code>{null,"01/12/07"}</code>: open ended (start) timespan as 2 element array
     *  <li><code>{"01/12/07",null}</code>: open ended (end) timespan as 2 element array 
     *  <li><code>{}</code>: no timestamp information as empty array
     * </ul>
     * </p>
     * @param feature The feature to execute against.
     */
    public double execute(SimpleFeature feature) throws IOException {
        String output = delegate.template(feature, "height.ftl", getClass());
        return Double.valueOf(output);
    }
}
