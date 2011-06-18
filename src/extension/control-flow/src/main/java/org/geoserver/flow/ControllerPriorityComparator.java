/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import java.util.Comparator;

/**
 * Sorts the flow controllers based on their priority (lower number means higher priority)
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class ControllerPriorityComparator implements Comparator<FlowController> {

    public int compare(FlowController o1, FlowController o2) {
        return o1.getPriority() - o2.getPriority();
    }

}