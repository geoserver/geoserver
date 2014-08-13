/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.Identifier;

public interface NotificationSerializer {

    String serializeInsertOrUpdate(Feature f);

    String serializeDelete(Name typeName, Identifier id);

}