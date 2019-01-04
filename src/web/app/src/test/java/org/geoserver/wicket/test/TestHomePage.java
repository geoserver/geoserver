/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wicket.test;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;

/** Homepage that will host tested component, or redirect to the test page */
public class TestHomePage extends WebPage {

    private static final long serialVersionUID = 2340501554860062526L;

    static IComponentFactory componentFactory;

    /**
     * Constructor that is invoked when page is invoked without a session.
     *
     * @param parameters Page parameters
     */
    public TestHomePage() {
        Component component = componentFactory.createComponent("component");
        if (component instanceof Page) setResponsePage((Page) component);
        else {
            if (!"component".equals(component.getId()))
                throw new IllegalArgumentException(
                        "Component factory was asked to produce a componet with "
                                + "id 'component' but returned one with id '"
                                + component.getId());
            add(component);
        }
    }
}
