/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
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
import java.io.Serial;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;

/**
 * A wicket resource that acts as a mini WMS to generate a map for a {@link CoordinateReferenceSystem CRS}'s area of
 * validity.
 *
 * <p>This resource expects the following parameters in order to generate the area of validity map:
 *
 * <ul>
 *   <li>WIDTH
 *   <li>HEIGHT
 *   <li>BBOX
 * </ul>
 *
 * @author Gabriel Roldan
 */
public class DynamicCrsMapResource extends AbstractResource {

    static final Logger LOGGER = Logging.getLogger(DynamicCrsMapResource.class);

    @Serial
    private static final long serialVersionUID = 1L;

    private final CoordinateReferenceSystem crs;

    public DynamicCrsMapResource(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    @Override
    protected ResourceResponse newResourceResponse(Attributes attributes) {
        ResourceResponse rsp = new ResourceResponse();
        rsp.setWriteCallback(new WriteCallback() {
            @Override
            public void writeData(Attributes attributes) throws IOException {
                IRequestParameters params = attributes.getRequest().getQueryParameters();
                int width = params.getParameterValue("WIDTH").toInt(400);
                int height = params.getParameterValue("HEIGHT").toInt(200);
                String bboxStr = params.getParameterValue("BBOX").toOptionalString();

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
                        LOGGER.log(Level.WARNING, "", e);
                    }
                }

                final byte[] byteArray = output == null ? null : output.toByteArray();
                if (byteArray != null) {
                    attributes.getResponse().write(byteArray);
                }
            }
        });
        return rsp;
    }

    private Envelope parseEnvelope(String bboxStr) {
        String[] split = bboxStr.split(",");
        double minx = Double.valueOf(split[0]);
        double miny = Double.valueOf(split[1]);
        double maxx = Double.valueOf(split[2]);
        double maxy = Double.valueOf(split[3]);
        return new Envelope(minx, maxx, miny, maxy);
    }

    private static class ByteArrayResourceStream extends AbstractResourceStream {

        @Serial
        private static final long serialVersionUID = 1L;

        private final byte[] content;

        public ByteArrayResourceStream(final byte[] content) {
            this.content = content;
        }

        @Override
        public void setLocale(Locale arg0) {}

        @Override
        public Bytes length() {
            return Bytes.bytes(content == null ? 0 : content.length);
        }

        @Override
        public InputStream getInputStream() throws ResourceStreamNotFoundException {
            if (content == null) {
                throw new ResourceStreamNotFoundException();
            }
            return new ByteArrayInputStream(content);
        }

        @Override
        public String getContentType() {
            return "image/png";
        }

        @Override
        public void close() throws IOException {}
    }
}
