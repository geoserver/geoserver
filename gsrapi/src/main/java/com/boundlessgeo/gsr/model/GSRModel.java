/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model;

import com.boundlessgeo.gsr.api.GeoServicesJacksonJsonConverter;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Marker interface for classes that can/should be written by {@link GeoServicesJacksonJsonConverter}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface GSRModel {
}
