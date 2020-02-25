/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import org.geoserver.metadata.data.dto.impl.AttributeTypeConfigurationImpl;

/**
 * Object that matches yaml structure.
 *
 * <p>The part describes a complex object. The complex object contains a list of mappings that make
 * the object.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
@JsonDeserialize(as = AttributeTypeConfigurationImpl.class)
public interface AttributeTypeConfiguration extends Serializable, AttributeCollection {

    public String getTypename();
}
