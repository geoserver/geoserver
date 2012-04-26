package org.geoserver.gss.impl.query;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.geoserver.bxml.atom.FeedEncoder;
import org.geoserver.gss.impl.GSS;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geoserver.gss.service.GetEntries;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.gvsig.bxml.adapt.stax.XmlStreamWriterAdapter;
import org.gvsig.bxml.stream.BxmlFactoryFinder;
import org.gvsig.bxml.stream.BxmlOutputFactory;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.gvsig.bxml.stream.BxmlStreamWriter_Contract;
import org.gvsig.bxml.stream.EncodingOptions;
import org.springframework.util.Assert;

/**
 * OWS {@link Response} that encodes the response of a {@code GetEntries} operation {@link FeedImpl
 * result} as specified in the <i>OGC 10-069r2 Engineering Report</i>, section 9.2.4
 * "Resolution feed", page 48.
 */
public class FeedResponse extends Response {

    private static final String TEXT_XML = "text/xml";

    private static final String TEXT_BXML = "text/x-bxml";

    private static final String APPLICATION_ATOM_XML = "application/atom+xml";

    private static final String APPLICATION_ATOM_X_BXML = "application/atom+x-bxml";

    private static final Set<String> OUTPUT_FORMATS;
    static {
        Set<String> formats = new HashSet<String>();
        formats.add(TEXT_XML);
        formats.add(APPLICATION_ATOM_XML);
        formats.add(APPLICATION_ATOM_X_BXML);
        formats.add(TEXT_BXML);
        OUTPUT_FORMATS = Collections.unmodifiableSet(formats);
    }

    protected final GSS gss;

    /**
     * @param feedName
     *            the kind feed this response can handle
     * @param gss
     */
    public FeedResponse(final GSS gss) {
        super(FeedImpl.class, OUTPUT_FORMATS);
        Assert.notNull(gss);
        this.gss = gss;
    }

    @Override
    public String getMimeType(final Object value, final Operation operation)
            throws ServiceException {
        Assert.isTrue(value instanceof FeedImpl);
        GetEntries request = OwsUtils.parameter(operation.getParameters(), GetEntries.class);
        String outputFormat = request.getOutputFormat();
        Assert.isTrue(getOutputFormats().contains(outputFormat));
        return outputFormat;
    }

    /**
     * Determines whether this response can handle the given operation based on whether the
     * operation parameter is a {@link GetEntries} request.
     * 
     * @see org.geoserver.ows.Response#canHandle(org.geoserver.platform.Operation)
     */
    @Override
    public boolean canHandle(final Operation operation) {
        final GetEntries request = OwsUtils.parameter(operation.getParameters(), GetEntries.class);
        if (request == null) {
            return false;
        }
        return true;
    }

    @Override
    public void write(final Object value, final OutputStream output, final Operation operation)
            throws IOException, ServiceException {
        Assert.isTrue(value instanceof FeedImpl);

        final GetEntries request = OwsUtils.parameter(operation.getParameters(), GetEntries.class);

        final FeedImpl response = (FeedImpl) value;
        final String outputFormat = request.getOutputFormat();

        try {
            BxmlStreamWriter writer;
            writer = getResponseWriter(output, outputFormat);
            encode(request, response, writer);
            writer.flush();
        } catch (ServiceException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw (RuntimeException) e;
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private BxmlStreamWriter getResponseWriter(final OutputStream output, final String outputFormat)
            throws IOException, XMLStreamException, FactoryConfigurationError {

        final boolean isBinary = outputFormat.startsWith(APPLICATION_ATOM_X_BXML)
                || outputFormat.equals(TEXT_BXML);

        BxmlStreamWriter writer;
        if (isBinary) {
            BxmlOutputFactory outputFactory = BxmlFactoryFinder.newOutputFactory();
            writer = outputFactory.createSerializer(output);
        } else {
            XMLStreamWriter staxWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(
                    output);
            EncodingOptions encodingOptions = new EncodingOptions();
            writer = new XmlStreamWriterAdapter(encodingOptions, staxWriter);
            writer = new BxmlStreamWriter_Contract(writer);
        }
        return writer;
    }

    protected void encode(final GetEntries request, final FeedImpl response,
            final BxmlStreamWriter w) throws ServiceException, IOException {

        FeedEncoder encoder = new FeedEncoder();

        encoder.encode(response, w);
        w.flush();
    }

}
