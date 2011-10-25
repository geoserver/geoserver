/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp.v2_0;

import net.opengis.wfs20.ListStoredQueriesType;
import net.opengis.wfs20.Wfs20Factory;

import org.geoserver.wfs.kvp.WFSKvpRequestReader;

public class ListStoredQueriesKvpRequestReader extends WFSKvpRequestReader {

    public ListStoredQueriesKvpRequestReader() {
        super(ListStoredQueriesType.class, Wfs20Factory.eINSTANCE);
    }

}
