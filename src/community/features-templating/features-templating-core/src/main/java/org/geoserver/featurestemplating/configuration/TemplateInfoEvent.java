/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

/** A TemplateInfo event. Simply hold the TemplateInfo object affected by the event. */
public class TemplateInfoEvent {

    private TemplateInfo ti;

    public TemplateInfoEvent(TemplateInfo templateInfo) {
        this.ti = templateInfo;
    }

    public TemplateInfo getSource() {
        return ti;
    }
}
