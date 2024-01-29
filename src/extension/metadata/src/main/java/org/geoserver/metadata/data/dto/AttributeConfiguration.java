/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import org.geoserver.metadata.data.dto.impl.AttributeConfigurationImpl;

/**
 * Object that matches yaml structure.
 *
 * <p>The configuration descibes one field for the gui.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
@JsonDeserialize(as = AttributeConfigurationImpl.class)
public interface AttributeConfiguration extends Serializable {

    String PREFIX = "metadata.generated.form.";

    String getKey();

    String getLabel();

    List<String> getTab();

    void setLabel(String label);

    FieldTypeEnum getFieldType();

    List<String> getValues();

    String getTypename();

    OccurrenceEnum getOccurrence();

    String getDerivedFrom();

    String getCondition();
}
