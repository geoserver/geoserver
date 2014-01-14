/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import javax.xml.namespace.QName;

public class LinkEx extends Link {
    @Override
    public QName getForeign() {
        return foreign == null ? key : foreign;
    }
}
