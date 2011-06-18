/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;


@SuppressWarnings("serial")
public class AttributeNewPage extends AttributeEditPage {

    public AttributeNewPage(AttributeDescription attribute, NewFeatureTypePage previousPage) {
        super(attribute, previousPage, true);
    }

}
