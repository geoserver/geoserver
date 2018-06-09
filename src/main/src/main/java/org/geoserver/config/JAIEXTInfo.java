/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.Serializable;
import java.util.Set;

public interface JAIEXTInfo extends Serializable, Cloneable {

    Set<String> getJAIOperations();

    void setJAIOperations(Set<String> operations);

    Set<String> getJAIEXTOperations();

    void setJAIEXTOperations(Set<String> operations);
}
