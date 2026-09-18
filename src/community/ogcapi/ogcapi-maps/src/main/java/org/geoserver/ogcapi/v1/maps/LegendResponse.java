/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.LegendGraphic;

/** Carries a produced legend together with the request that built it, so the OWS legend Response can encode it. */
public record LegendResponse(LegendGraphic legend, GetLegendGraphicRequest request) {}
