/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import org.geoserver.featurestemplating.configuration.AbstractFeatureTemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfo;

/**
 * Implementation of AbstractFeatureTemplateInfo used to keep the a reference to the previous state
 * of a TemplateInfo.
 */
class TemplateInfoMemento extends AbstractFeatureTemplateInfo {

    private String rawTemplate;

    TemplateInfoMemento(TemplateInfo ti) {
        super(ti.getTemplateName(), ti.getWorkspace(), ti.getFeatureType(), ti.getExtension());
    }

    TemplateInfoMemento(TemplateInfo ti, String rawTemplate) {
        this(ti);
        this.rawTemplate = rawTemplate;
    }

    String getRawTemplate() {
        return rawTemplate;
    }

    void setRawTemplate(String rawTemplate) {
        this.rawTemplate = rawTemplate;
    }
}
