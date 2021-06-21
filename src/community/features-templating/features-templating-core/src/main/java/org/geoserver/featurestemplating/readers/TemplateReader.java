/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import org.geoserver.featurestemplating.builders.impl.RootBuilder;

/** Base interface for all the Template readers. */
public interface TemplateReader {

    public static final String FILTERKEY = "$filter";

    public static final String EXPRSTART = "${";

    public static final String SOURCEKEY = "$source";

    public static final String VENDOROPTION = "$options";

    /**
     * Get a builder tree as a ${@link RootBuilder} from a template file
     *
     * @return
     */
    RootBuilder getRootBuilder();
}
