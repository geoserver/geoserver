/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.IPagingLabelProvider;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigation;

/**
 * A custom navigator that sets classes for the elements
 *
 * @author Andrea Aime - TOPP
 */
@SuppressWarnings("serial")
public class GeoServerPagingNavigator extends AjaxPagingNavigator {

    public GeoServerPagingNavigator(String id, IPageable pageable) {
        super(id, pageable);
        setOutputMarkupId(true);
    }

    @Override
    protected PagingNavigation newNavigation(
            String id, IPageable pageable, IPagingLabelProvider labelProvider) {
        // make sure we don't have too many links, it gets quite busy in popups
        PagingNavigation navigation = super.newNavigation(id, pageable, labelProvider);
        navigation.setViewSize(5);
        return navigation;
    }

    @Override
    protected AbstractLink newPagingNavigationLink(String id, IPageable pageable, int pageNumber) {
        AbstractLink link = super.newPagingNavigationLink(id, pageable, pageNumber);
        // we turn the id into the css class
        link.add(AttributeModifier.replace("class", id));
        return link;
    }

    @Override
    protected AbstractLink newPagingNavigationIncrementLink(
            String id, IPageable pageable, int increment) {
        AbstractLink link = super.newPagingNavigationIncrementLink(id, pageable, increment);
        // we turn the id into the css class
        link.add(AttributeModifier.replace("class", id));
        return link;
    }
}
