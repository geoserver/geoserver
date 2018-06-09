/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import net.opengis.wcs20.ExtensionItemType;
import net.opengis.wcs20.ExtensionType;
import net.opengis.wcs20.GetCoverageType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wcs.responses.CoverageEncoder;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.response.GMLCovHandler.CoverageData;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.grid.GridCoverage;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Returns a single coverage encoded in the specified output format (eventually the native one)
 * along with the XML describing the coverage, in a MIME multipart package
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WCS20GetCoverageMultipartResponse extends Response {

    CoverageResponseDelegateFinder responseFactory;

    EnvelopeAxesLabelsMapper envelopeDimensionsMapper;

    public WCS20GetCoverageMultipartResponse(
            CoverageResponseDelegateFinder responseFactory,
            EnvelopeAxesLabelsMapper envelopeDimensionsMapper) {
        super(GridCoverage.class);
        this.responseFactory = responseFactory;
        this.envelopeDimensionsMapper = envelopeDimensionsMapper;
    }

    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    public String getMimeType(Object value, Operation operation) {
        return "multipart/related";
    }

    @Override
    public boolean canHandle(Operation operation) {
        Object firstParam = operation.getParameters()[0];
        if (!(firstParam instanceof GetCoverageType)) {
            // we only handle WCS 2.0 requests
            return false;
        }

        GetCoverageType getCoverage = (GetCoverageType) firstParam;

        // this class only handles encoding the coverage with mediatype
        String mediaType = getCoverage.getMediaType();
        return mediaType != null && mediaType.equals("multipart/related");
    }

    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        // grab the coverage
        GridCoverage2D coverage = (GridCoverage2D) value;

        // grab the format
        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String format = getCoverage.getFormat();
        if (format == null) {
            format = "image/tiff";
        }

        // extract additional extensions
        final Map<String, String> encodingParameters = new HashMap<String, String>();
        final ExtensionType extension = getCoverage.getExtension();
        if (extension != null) {
            final EList<ExtensionItemType> extensions = extension.getContents();
            for (ExtensionItemType ext : extensions) {
                encodingParameters.put(ext.getName(), ext.getSimpleContent());
            }
        }

        // grab the delegate
        CoverageResponseDelegate delegate = responseFactory.encoderFor(format);

        // use javamail classes to actually encode the document
        try {
            MimeMultipart multipart = new MimeMultipart();
            multipart.setSubType("related");

            String fileName =
                    "/coverages/"
                            + getCoverage.getCoverageId()
                            + "."
                            + delegate.getFileExtension(format);

            // coverages xml structure, which is very close to the DescribeFeatureType output
            BodyPart coveragesPart = new MimeBodyPart();
            FileReference reference =
                    new FileReference(
                            fileName,
                            delegate.getMimeType(format),
                            delegate.getConformanceClass(format));
            final CoverageData coveragesData =
                    new CoverageData(coverage, reference, envelopeDimensionsMapper);
            coveragesPart.setDataHandler(new DataHandler(coveragesData, "geoserver/coverages20"));
            coveragesPart.setHeader("Content-ID", "wcs");
            coveragesPart.setHeader("Content-Type", "application/gml+xml");
            multipart.addBodyPart(coveragesPart);

            // the actual coverage
            BodyPart coveragePart = new MimeBodyPart();
            CoverageEncoder encoder =
                    new CoverageEncoder(delegate, coverage, format, encodingParameters);
            coveragePart.setDataHandler(new DataHandler(encoder, "geoserver/coverageDelegate"));
            coveragePart.setHeader("Content-ID", fileName);
            coveragePart.setHeader("Content-Type", delegate.getMimeType(format));
            coveragePart.setHeader("Content-Transfer-Encoding", "binary");
            multipart.addBodyPart(coveragePart);

            // write out the multipart (we need to use mime message trying to
            // encode directly with multipart or BodyPart does not set properly
            // the encodings and binary files gets ruined
            MimeMessage message = new GeoServerMimeMessage();
            message.setContent(multipart);
            message.writeTo(output);
            output.flush();
        } catch (MessagingException e) {
            throw new WcsException("Error occurred while encoding the mime multipart response", e);
        }
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        // the only thing that can open this format normally available on a desktop is a e-mail
        // client
        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        return getCoverage.getCoverageId() + ".eml";
    }

    /**
     * A special mime message that does not set any header other than the content type
     *
     * @author Andrea Aime - GeoSolutions
     */
    private static class GeoServerMimeMessage extends MimeMessage {
        public GeoServerMimeMessage() {
            super((Session) null);
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            // it's just ugly to see ...
            removeHeader("Message-ID");
        }
    }
}
