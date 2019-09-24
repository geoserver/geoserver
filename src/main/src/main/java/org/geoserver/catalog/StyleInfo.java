/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;

/**
 * A style for a geospatial resource.
 *
 * @author Justin Deoliveira, The Open Planning project
 */
public interface StyleInfo extends CatalogInfo {

    /** Name of the default point style. */
    public static String DEFAULT_POINT = "point";
    /** Name of the default line style. */
    public static String DEFAULT_LINE = "line";
    /** Name of the default polygon style. */
    public static String DEFAULT_POLYGON = "polygon";
    /** Name of the default raster style. */
    public static String DEFAULT_RASTER = "raster";

    /** Name of the default generic style. */
    public static String DEFAULT_GENERIC = "generic";

    /**
     * Name of the style.
     *
     * <p>This value is unique among all styles and can be used to identify the style.
     *
     * @uml.property name="name"
     */
    String getName();

    /**
     * Sets the name of the style.
     *
     * @uml.property name="name"
     */
    void setName(String name);

    /** The workspace the style is part of, or <code>null</code> if the style is global. */
    WorkspaceInfo getWorkspace();

    /** Sets the workspace the style is part of. */
    void setWorkspace(WorkspaceInfo workspace);

    /** The styling language/format for the style, for example: "sld" */
    String getFormat();

    /** Sets the styling format for the style, for example: "sld" */
    void setFormat(String format);

    /** The version of the style format. */
    Version getFormatVersion();

    /** Sets the version of the style format. */
    void setFormatVersion(Version version);

    /** The name of the file the style originates from. */
    String getFilename();

    /** Sets the name of the file the style originated from. */
    void setFilename(String fileName);

    /** The style object. */
    Style getStyle() throws IOException;

    /** The full sld object (which contains the style). */
    StyledLayerDescriptor getSLD() throws IOException;

    /** The Legend for the style. */
    LegendInfo getLegend();

    /** Sets the Legend for the style. */
    void setLegend(LegendInfo legend);

    /**
     * The derived prefixed name.
     *
     * <p>If a workspace is set this method returns:
     *
     * <pre>
     *   getWorkspace().getName() + ":" + getName();
     * </pre>
     *
     * Otherwise it simply returns:
     *
     * <pre>getName()</pre>
     */
    String prefixedName();

    /**
     * A persistent map of metadata.
     *
     * <p>Data in this map is intended to be persisted. Common case of use is to have services
     * associate various bits of data with a particular style.
     */
    MetadataMap getMetadata();
}
