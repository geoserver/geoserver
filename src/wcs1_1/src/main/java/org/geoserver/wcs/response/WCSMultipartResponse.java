/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import net.opengis.wcs11.GetCoverageType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.response.CoveragesHandler.CoveragesData;
import org.geoserver.wcs.responses.CoverageEncoder;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.grid.GridCoverage;
import org.vfny.geoserver.wcs.WcsException;

public class WCSMultipartResponse extends Response {

    MimeMultipart multipart;

    Catalog catalog;

    CoverageResponseDelegateFinder responseFactory;

    public WCSMultipartResponse(Catalog catalog, CoverageResponseDelegateFinder responseFactory) {
        super(GridCoverage[].class);
        this.catalog = catalog;
        this.multipart = new MimeMultipart();
        this.responseFactory = responseFactory;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        // javamail outputs multipart/mixed, but in our case we're producing multipart/related
        return multipart
                .getContentType()
                .replace("mixed", "related")
                .replace("\n", "")
                .replace("\r", "");
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    public String getAttachmentFileName(Object value, Operation operation) {
        final GetCoverageType request = (GetCoverageType) operation.getParameters()[0];
        final String identifier = request.getIdentifier().getValue();
        return identifier.replace(':', '_') + ".eml";
    }

    @Override
    public boolean canHandle(Operation operation) {
        // this one can handle GetCoverage responses where store = false
        if (!(operation.getParameters()[0] instanceof GetCoverageType)) return false;

        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        return !getCoverage.getOutput().isStore();
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        GridCoverage[] coverages = (GridCoverage[]) value;

        // grab the delegate for coverage encoding
        GetCoverageType request = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = request.getOutput().getFormat();
        CoverageResponseDelegate delegate = responseFactory.encoderFor(outputFormat);
        if (delegate == null)
            throw new WcsException("Could not find encoder for output format " + outputFormat);

        // grab the coverage info for Coverages document encoding
        final GridCoverage2D coverage = (GridCoverage2D) coverages[0];
        CoverageInfo coverageInfo = catalog.getCoverageByName(request.getIdentifier().getValue());

        // use javamail classes to actually encode the document
        try {
            // coverages xml structure (always set the headers after the data
            // handlers, setting
            // the data handlers kills some of them)
            BodyPart coveragesPart = new MimeBodyPart();
            final CoveragesData coveragesData = new CoveragesData(coverageInfo, request);
            coveragesPart.setDataHandler(new DataHandler(coveragesData, "geoserver/coverages11"));
            coveragesPart.setHeader("Content-ID", "<urn:ogc:wcs:1.1:coverages>");
            coveragesPart.setHeader("Content-Type", "text/xml");
            multipart.addBodyPart(coveragesPart);

            // the actual coverage
            BodyPart coveragePart = new MimeBodyPart();
            CoverageEncoder encoder =
                    new CoverageEncoder(
                            delegate, coverage, outputFormat, new HashMap<String, String>());
            coveragePart.setDataHandler(new DataHandler(encoder, "geoserver/coverageDelegate"));
            coveragePart.setHeader("Content-ID", "<theCoverage>");
            coveragePart.setHeader("Content-Type", delegate.getMimeType(outputFormat));
            coveragePart.setHeader("Content-Transfer-Encoding", "base64");
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

    /**
     * A special mime message that does not set any header other than the content type
     *
     * @author Andrea Aime - TOPP
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
