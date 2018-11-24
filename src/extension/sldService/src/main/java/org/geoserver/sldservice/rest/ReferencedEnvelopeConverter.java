/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import org.geoserver.rest.RestException;
import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ReferencedEnvelopeConverter implements Converter<String, ReferencedEnvelope> {

    BBoxKvpParser parser = new BBoxKvpParser();

    @Override
    public ReferencedEnvelope convert(String source) {
        try {
            return (ReferencedEnvelope) parser.parse(source);
        } catch (Exception e) {
            throw new RestException(
                    "Invalid bounding box specification ", HttpStatus.BAD_REQUEST, e);
        }
    }
}
