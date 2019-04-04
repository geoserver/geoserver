/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wicket.test;

import java.io.Serializable;
import org.apache.wicket.Component;

/**
 * Creates a component for the test runner to use
 *
 * @author Andrea Aime - OpenGeo
 */
public interface IComponentFactory extends Serializable {
    /**
     * Creates the Component to be tested (may be a Page)
     *
     * @param id The id that must be assigned to the component. Any other id will cause an exception
     */
    Component createComponent(String id);
}
