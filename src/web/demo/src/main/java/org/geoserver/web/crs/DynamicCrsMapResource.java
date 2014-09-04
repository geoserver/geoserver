/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.crs;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.value.ValueMap;
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
public class DynamicCrsMapResource extends WebResource {

    private static final long serialVersionUID = 1L;

    private final CoordinateReferenceSystem crs;

    public DynamicCrsMapResource(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    @Override
    public IResourceStream getResourceStream() {

        ValueMap parameters = getParameters();
        int width = parameters.getInt("WIDTH", 400);
        int height = parameters.getInt("HEIGHT", 200);
        String bboxStr = parameters.getString("BBOX");

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

        final byte[] byteArray = output == null ? null : output.toByteArray();

        return new ByteArrayResourceStream(byteArray);
    }

    private Envelope parseEnvelope(String bboxStr) {
        String[] split = bboxStr.split(",");
        double minx = Double.valueOf(split[0]);
        double miny = Double.valueOf(split[1]);
        double maxx = Double.valueOf(split[2]);
        double maxy = Double.valueOf(split[3]);
        return new Envelope(minx, maxx, miny, maxy);
    }

    private static class ByteArrayResourceStream implements IResourceStream {

        private static final long serialVersionUID = 1L;

        private final byte[] content;

        public ByteArrayResourceStream(final byte[] content) {
            this.content = content;
        }

        public void setLocale(Locale arg0) {
        }

        public long length() {
            return content == null? 0 : content.length;
        }

        public Locale getLocale() {
            return null;
        }

        public InputStream getInputStream() throws ResourceStreamNotFoundException {
            if (content == null) {
                throw new ResourceStreamNotFoundException();
            }
            return new ByteArrayInputStream(content);
        }

        public String getContentType() {
            return "image/png";
        }

        public void close() throws IOException {
        }

        public org.apache.wicket.util.time.Time lastModifiedTime() {
            return null;
        }
    }
}
