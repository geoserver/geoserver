/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import javax.xml.namespace.QName;

public interface TestConstants {
    static final String NS_TRACK = "http://www.example.com/track";
    static final String NS_FOO = "http://www.example.com/foo";
    static final QName QN_TRACK = new QName(NS_TRACK, "Track");
    static final QName QN_OTHERTYPE = new QName(NS_TRACK, "OtherType");
    static final QName QN_THIRDTYPE = new QName(NS_FOO, "ThirdType");
    static final QName QN_KNIGHT = new QName(NS_FOO, "knight", "foo");
    static final QName QN_BISHOP = new QName(NS_FOO, "bishop", "foo");
    static final QName QN_BOARD = new QName(NS_FOO, "board", "foo");
}
