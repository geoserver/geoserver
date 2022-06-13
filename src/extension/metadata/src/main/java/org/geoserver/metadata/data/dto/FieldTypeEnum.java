/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto;

/**
 * Object that matches in fixed choice in the yaml structure.
 *
 * <p>Choose the Gui component Type.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public enum FieldTypeEnum {
    COMPLEX,
    TEXT,
    NUMBER,
    TEXT_AREA,
    DATE,
    DATETIME,
    BOOLEAN,
    UUID,
    DROPDOWN,
    SUGGESTBOX,
    REQUIREBOX,
    DERIVED
}
