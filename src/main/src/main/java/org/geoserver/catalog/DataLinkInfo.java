/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * A link to underlying data represented by resource.
 *
 * @author Marcus Sen, British Geological Survey
 */
public interface DataLinkInfo extends Info {

    /** @uml.property name="about" */
    String getAbout();

    /** @uml.property name="about" */
    void setAbout(String about);

    /** @uml.property name="type" */
    String getType();

    /** @uml.property name="type" */
    void setType(String type);

    /** @uml.property name="content" */
    String getContent();

    /** @uml.property name="content" */
    void setContent(String content);
}
