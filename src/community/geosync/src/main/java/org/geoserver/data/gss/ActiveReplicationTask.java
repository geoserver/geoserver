package org.geoserver.data.gss;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;

import org.apache.commons.httpclient.methods.GetMethod;
import org.geoserver.bxml.FeatureTypeProvider;
import org.geoserver.bxml.atom.FeedDecoder;
import org.geoserver.gss.impl.query.TemporalOp;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.task.LongTask;
import org.geotools.feature.type.DateUtil;
import org.gvsig.bxml.adapt.stax.XmlStreamReaderAdapter;
import org.gvsig.bxml.stream.BxmlFactoryFinder;
import org.gvsig.bxml.stream.BxmlInputFactory;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.opengis.util.ProgressListener;

class ActiveReplicationTask extends LongTask<Object> {

    private final ServerSubscription subscriptionOpts;

    private final GeoSyncClient geoSyncClient;

    public ActiveReplicationTask(final ServerSubscription subscriptionOpts,
            final GeoSyncClient geoSyncClient) {

        super("GSS Replication", "GSS Replication for " + subscriptionOpts.getUrl());

        this.subscriptionOpts = subscriptionOpts;
        this.geoSyncClient = geoSyncClient;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Object callInternal(final ProgressListener listener) throws Exception {
        listener.started();
        listener.setDescription("Getting capabilities form " + subscriptionOpts.getUrl());

        final Capabilities capabilities;
        final String user = subscriptionOpts.getUser();
        final String password = subscriptionOpts.getPassword();
        {
            String capabilitiesURI = subscriptionOpts.getUrl();
            capabilities = geoSyncClient.getCachedCapabilities(capabilitiesURI, user, password);
        }

        final String getEntriesUrl = capabilities.getOperationUrl("GetEntries", false);
        if (getEntriesUrl == null) {
            throw new IllegalStateException(
                    "GSS capabilities does not provide a GetEntries operation URL");
        }

        listener.setDescription("Performing GSS GetEntries request...");

        final HTTPClient httpClient = geoSyncClient.getHttpClient();
        final String requestUrl = buildRequestUrl(getEntriesUrl);
        final GetMethod method = httpClient.get(requestUrl, user, password);
        try {
            final FeatureTypeProvider replicatedTypes = new ReplicatedTypeResolver(geoSyncClient,
                    subscriptionOpts);

            final InputStream response = method.getResponseBodyAsStream();
            final BxmlStreamReader reader = getReader(response);
            EntryImpl lastEntry = null;
            try {
                reader.nextTag();

                listener.setDescription("Processing GSS replication feed entries...");
                final FeedDecoder feedDecoder = new FeedDecoder();
                final FeedImpl feed = feedDecoder.decode(reader);
                ReplicationReceiver replicationReceiver = new ReplicationReceiver();
                lastEntry = replicationReceiver.receive(feed, replicatedTypes, listener);
            } finally {
                reader.close();
            }

            if (lastEntry != null) {
                // save the timestamp of the last replicated entry
                geoSyncClient.saveLastProcessedEntry(subscriptionOpts, lastEntry);
            }

            listener.progress(100f);// .complete() should do this but it doesn't atm
            listener.complete();
        } finally {
            method.releaseConnection();
        }
        return null;
    }

    private BxmlStreamReader getReader(final InputStream input) throws Exception {
        PushbackInputStream in = new PushbackInputStream(input);
        final int binaryMark = in.read();
        in.unread(binaryMark);
        final boolean isBinary = binaryMark == 1;
        BxmlStreamReader reader;
        if (isBinary) {
            BxmlInputFactory inputFactory = BxmlFactoryFinder.newInputFactory();
            inputFactory.setNamespaceAware(true);
            reader = inputFactory.createScanner(in);
        } else {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            reader = new XmlStreamReaderAdapter(factory, in);
        }
        return reader;

    }

    private String buildRequestUrl(final String getEntriesBaseUrl) throws IOException {

        Date lastReplicationTimestamp = geoSyncClient.getLastReplicationTime(subscriptionOpts);

        Map<String, String> kvp = new LinkedHashMap<String, String>();
        kvp.put("service", "GSS");
        kvp.put("version", "1.0.0");
        kvp.put("request", "GetEntries");
        kvp.put("feed", "REPLICATIONFEED");
        kvp.put("outputFormat", "text/x-bxml");
        kvp.put("maxEntries", String.valueOf(Integer.MAX_VALUE));
        if (lastReplicationTimestamp != null) {
            final long time = lastReplicationTimestamp.getTime();
            final String timeStamp = DateUtil.serializeDateTime(time, true);
            kvp.put("STARTTIME", timeStamp);
            kvp.put("TemporalOp", TemporalOp.After.toString());
        }

        final String path = null;
        String queryUrl = ResponseUtils.buildURL(getEntriesBaseUrl, path, kvp, URLType.EXTERNAL);

        return queryUrl;
    }

}
