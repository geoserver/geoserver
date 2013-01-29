/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.crs;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

/**
 * A wicket resource that acts as a mini WMS to generate a map for a
 * {@link CoordinateReferenceSystem CRS}'s area of validity.
 * <p>
 * This resource expects the following parameters in order to generate the area of validity map:
 * <ul>
 * <li>WIDTH
 * <li>HEIGHT
 * <li>BBOX
 * </ul>
 * </p>
 * 
 * @author Gabriel Roldan
 */
public class DynamicCrsMapResource extends DynamicImageResource {

    private static final long serialVersionUID = 1L;

    private final CoordinateReferenceSystem crs;

    public DynamicCrsMapResource(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }
    
    @Override
    protected byte[] getImageData(Attributes attributes) {
        PageParameters parameters = attributes.getParameters();
        int width = parameters.get("WIDTH").toInt(400);
        int height = parameters.get("HEIGHT").toInt(200);
        String bboxStr = parameters.get("BBOX").toString();

        ByteArrayOutputStream output = null;
        if (bboxStr != null) {

            try {
                CRSAreaOfValidityMapBuilder builder = new CRSAreaOfValidityMapBuilder(width, height);
                Envelope envelope = parseEnvelope(bboxStr);
                RenderedImage image = builder.createMapFor(crs, envelope);
                output = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", output);
            } catch (Exception e) {
                output = null;
                e.printStackTrace();
            }
        }

        return output == null ? null : output.toByteArray();
    }

    private Envelope parseEnvelope(String bboxStr) {
        String[] split = bboxStr.split(",");
        double minx = Double.valueOf(split[0]);
        double miny = Double.valueOf(split[1]);
        double maxx = Double.valueOf(split[2]);
        double maxy = Double.valueOf(split[3]);
        return new Envelope(minx, maxx, miny, maxy);
    }
}
