package org.geoserver.wfstemplating.readers;

import org.geoserver.wfstemplating.builders.impl.RootBuilder;

/** Base interface for all the Template readers. */
public interface TemplateReader {

    /**
     * Get a builder tree as a ${@link RootBuilder} from a template file
     *
     * @return
     */
    RootBuilder getRootBuilder();
}
