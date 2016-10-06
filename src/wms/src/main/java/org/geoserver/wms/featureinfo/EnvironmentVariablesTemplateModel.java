/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import org.geoserver.platform.GeoServerExtensions;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class EnvironmentVariablesTemplateModel implements TemplateHashModel {

    @Override
    public TemplateModel get(String propertyName) throws TemplateModelException {
        return new SimpleScalar(GeoServerExtensions.getProperty(propertyName));
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    

}
