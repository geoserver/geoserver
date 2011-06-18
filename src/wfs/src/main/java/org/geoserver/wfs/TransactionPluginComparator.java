/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Comparator;


/**
 * Compares transaction plugins based on the priority field.
 * @author Administrator
 *
 */
public class TransactionPluginComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        TransactionPlugin t1 = (TransactionPlugin) o1;
        TransactionPlugin t2 = (TransactionPlugin) o2;

        // high priority number -> earlier in the sorting
        return t2.getPriority() - t1.getPriority();
    }
}
