/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.dxf;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Interface for a DXF Writer implementation. DXF exists in many different versions, so we can
 * expect many different implementations, each one supporting one or more of these versions.
 * Implementations are registered as SPI and can be found using DXFWriterFinder.
 *
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 */
public interface DXFWriter {
    /** Creates a new instance of the writer, using the given writer as output. */
    public DXFWriter newInstance(Writer writer);

    /** Checks if the writer supports the requested dxf version. */
    public boolean supportsVersion(String version);

    /** Performs the actual writing. */
    public void write(List featureList, String version) throws IOException;

    /** Configure a writer option. */
    public void setOption(String optionName, Object optionValue);

    /** Gets the writer description. */
    public String getDescription();
}
