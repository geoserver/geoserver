package org.geoserver.jsonld.readers;

import org.geoserver.jsonld.builders.impl.RootBuilder;

/** Base interface for all the Template readers. */
public interface TemplateReader {

    /**
     * Get a builder tree as a ${@link RootBuilder} from a template file
     *
     * @return
     */
    RootBuilder getRootBuilder();
}
