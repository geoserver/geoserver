package org.geoserver.gsr.model.relationship;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.geoserver.gsr.api.GeoServicesJacksonJsonConverter;

/**
 * Marker interface for classes that can/should be written by {@link
 * GeoServicesJacksonJsonConverter}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface RelationshipModel {}
