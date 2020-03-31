/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.relationship;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.geoserver.gsr.api.GeoServicesJacksonJsonConverter;

/**
 * Marker interface for classes that can/should be written by {@link
 * GeoServicesJacksonJsonConverter}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface RelationshipModel {}
