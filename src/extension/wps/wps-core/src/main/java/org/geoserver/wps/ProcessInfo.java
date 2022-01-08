/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import com.google.common.collect.Multimap;
import java.util.List;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.wps.security.SecurityProcessFactory;
import org.geoserver.wps.validator.WPSInputValidator;
import org.opengis.feature.type.Name;

/**
 * Configuration for a specific process to configure enable/disable and roles informations (backed
 * by a {@link SecurityProcessFactory})
 *
 * @used {@link ProcessGroupInfo#getFilteredProcesses()}
 */
public interface ProcessInfo extends Info, Cloneable {

    /** The name of the process */
    Name getName();

    /** Sets the name of the process */
    void setName(Name name);

    /** Whether the process is enabled or disabled */
    boolean isEnabled();

    /** Enables/disables the process */
    void setEnabled(Boolean enabled);

    /** Return roles granted to work with this WPS */
    List<String> getRoles();

    /**
     * The input validators. GeoServer will recognize, advertise and give special treatment to well
     * known ones, but the implementor is free to add extra ones that will simply fail the execute
     * call in case they don't match.
     */
    Multimap<String, WPSInputValidator> getValidators();

    /**
     * The metadata map, can contain any sort of information that non core plugins might use to
     * handle information related to this factory
     */
    MetadataMap getMetadata();
}
