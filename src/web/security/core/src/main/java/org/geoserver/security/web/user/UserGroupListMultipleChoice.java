/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import java.util.List;

import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;
import org.geoserver.security.impl.GeoServerUserGroup;

/**
 * List multiple choice widget for {@Link GeoServerUserGroup}.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class UserGroupListMultipleChoice extends ListMultipleChoice<GeoServerUserGroup> {

    public UserGroupListMultipleChoice(String id, IModel<List<GeoServerUserGroup>> model, 
        IModel<List<GeoServerUserGroup>> choicesModel) {
        super(id, model, choicesModel, new UserGroupRenderer());
    }
}
