package org.geoserver.gss.functional.v_1_0_0;

import junit.framework.Test;

import org.geoserver.gss.internal.atom.Atom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This test suite contains only the assertions for the empty repository, see
 * {@link Query_OGC_KVP_Test} for a more complete test suite.
 * 
 * @author groldan
 * @see Query_OGC_KVP_Test
 * 
 */
public class Query_OGC_KVP_EmptyRepoTest extends GSSFunctionalTestSupport {

    private static final String BASE_REQUEST_PATH = "/ows?service=GSS&version=1.0.0&request=GetEntries";

    private static final String REPLICATION_FEED_BASE = BASE_REQUEST_PATH + "&FEED=REPLICATIONFEED";

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new Query_OGC_KVP_EmptyRepoTest());
    }

    /**
     * Only mandatory param besides service/request/version is FEED.
     */
    public void testEmptyFeed() throws Exception {
        final String request = REPLICATION_FEED_BASE;
        Document dom = super.getAsDOM(request);
        print(dom);
        Element root = dom.getDocumentElement();
        String nodeName = root.getLocalName();
        assertEquals(Atom.NAMESPACE, root.getNamespaceURI());
        assertEquals("feed", nodeName);
        // assertXpathEvaluatesTo(FeedImpl.NULL_ID, "atom:feed/atom:id", dom);
        // assertXpathExists("atom:feed/atom:updated", dom);
    }
}
