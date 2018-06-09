/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.Serializable;

/**
 * Transformation to apply at some stage of the import.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface ImportTransform extends Serializable {

    /**
     * Should this transform stop on an error.
     *
     * @todo example of why it shouldn't?
     * @param e The error in question
     * @return true if processing should stop, false otherwise
     */
    boolean stopOnError(Exception e);

    /**
     * Initialize any transient or temporary state. This should be called prior to invoking any
     * other methods on the transform.
     */
    void init();
}
