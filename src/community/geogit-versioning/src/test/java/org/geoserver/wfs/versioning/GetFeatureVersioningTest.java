package org.geoserver.wfs.versioning;

import static org.geotools.feature.type.DateUtil.serializeDateTime;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.geotools.filter.v2_0.FES;
import org.geotools.util.logging.Logging;
import org.opengis.filter.identity.ResourceId;
import org.opengis.filter.identity.Version;
import org.opengis.filter.identity.Version.Action;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Functional test suite for the {@code GetFeature} WFS 2.0.0 operation with {@link ResourceId}
 * filter predicates.
 * <p>
 * The
 * </p>
 * 
 * @author groldan
 * 
 */
public class GetFeatureVersioningTest extends WFS20VersioningTestSupport {

    private static final Logger LOGGER = Logging.getLogger(GetFeatureVersioningTest.class);

    String buildings, bridges;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetFeatureVersioningTest());
    }

    @Override
    protected void setUpInternal() {
        buildings = getLayerId(MockData.BUILDINGS);
        bridges = getLayerId(MockData.BRIDGES);
    }

    public void testGetFeature() throws Exception {

        Document dom = getAsDOM("wfs?request=GetFeature&version=2.0.0&typeName=" + buildings);
        assertGetFeatures(dom, 1, buildings, commit5FeatureIdentifiers);

        dom = getAsDOM("wfs?request=GetFeature&version=2.0.0&typeName=" + bridges);
        assertGetFeatures(dom, 1, bridges, commit5FeatureIdentifiers);
    }

    private void assertGetFeatures(Document dom, int expectedFeatures, String typeName,
            Set<String> allRids) throws Exception {

        // assert features returned
        XMLAssert.assertXpathEvaluatesTo(String.valueOf(expectedFeatures), "count(//" + typeName
                + ")", dom);

        try {
            // assert features returned have version info in ids
            NodeList features = dom.getElementsByTagName(typeName);
            for (int i = 0; i < features.getLength(); i++) {
                Element feature = (Element) features.item(i);

                String fid = feature.getAttribute("gml:id");
                assertNotNull(fid);
                String msg = "'" + fid + "' is not in " + allRids;
                assertTrue(msg, allRids.contains(fid));
            }
        } catch (AssertionFailedError e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void testGetFeatureResourceIdKVP() throws Exception {

        for (String rid : commit4FeatureIdentifiers) {
            String typeName = rid.startsWith("Buildings") ? buildings : bridges;
            String query = "wfs?request=GetFeature&version=2.0.0&typeName=" + typeName
                    + "&resourceId=" + rid;
            Document dom = getAsDOM(query);
            assertGetFeatures(dom, 1, typeName, Collections.singleton(rid));
        }

        // current versions with normal feature id (with no <@version> attached)
        {
            String query = "wfs?request=GetFeature&version=2.0.0&typeName=" + bridges
                    + "&resourceId=Bridges.1107531599613";
            Document dom = getAsDOM(query);
            assertGetFeatures(dom, 1, bridges, commit5FeatureIdentifiers);
        }
        {
            String query = "wfs?request=GetFeature&version=2.0.0&typeName=" + buildings
                    + "&resourceId=Buildings.1107531701011";
            Document dom = getAsDOM(query);
            assertGetFeatures(dom, 1, buildings, commit5FeatureIdentifiers);
        }
    }

    public void testGetFeatureStartDate() throws Exception {

        final String fid = "Buildings.1107531701011";
        // this feature was modified at commit4, so asking for a startDate that's between commit3
        // and commit4 should give commit4's version
        Date startDate = new Date(commit4.getTimestamp() - 500);
        Date endDate = null;
        Version version = null;
        String xml = buildGetFeatureXml(buildings, fid, startDate, endDate, version);
        Document dom = postAsDOM("wfs?", xml);
        assertGetFeatures(dom, 1, buildings, commit4FeatureIdentifiers);

    }

    public void testGetFeatureEndDate() throws Exception {
        final String fid = "Buildings.1107531701011";
        // this feature was modified at commit4, so asking for an endDate that's between commit3
        // and commit4 should give commit3's version
        Date startDate = null;
        Date endDate = new Date(commit4.getTimestamp() - 500);
        Version version = null;
        String xml = buildGetFeatureXml(buildings, fid, startDate, endDate, version);
        Document dom = postAsDOM("wfs?", xml);
        assertGetFeatures(dom, 1, buildings, commit3FeatureIdentifiers);
    }

    public void testGetFeatureStartDateEndDate() throws Exception {
        final String fid = "Buildings.1107531701011";
        // this feature was modified at commit4, so asking for an endDate that's between commit3
        // and commit5 should give commit4's version
        Date startDate = new Date(commit4.getTimestamp() - 500);
        Date endDate = new Date(commit4.getTimestamp() + 500);
        Version version = null;
        String xml = buildGetFeatureXml(buildings, fid, startDate, endDate, version);
        Document dom = postAsDOM("wfs?", xml);
        assertGetFeatures(dom, 1, buildings, commit4FeatureIdentifiers);
    }

    public void testGetFeatureVersionDate() throws Exception {
        final String rid = "Bridges.1107531599613";
        String xml;
        Document dom;
        Version version;

        // requested time prior to first commit, should return feature version at first commit
        version = new Version(new Date(commit1.getTimestamp() - 5000));
        xml = buildGetFeatureXml(bridges, rid, null, null, version);
        dom = postAsDOM("wfs?", xml);

        //from the spec:
        //The version attribute may also be date indicating that the version of the resource
        //closest to the specified date shall be selected.
        assertGetFeatures(dom, 1, bridges, commit1FeatureIdentifiers);
        //assertGetFeatures(dom, 0, bridges, commit1FeatureIdentifiers);

        // version of the first commit
        version = new Version(new Date(commit1.getTimestamp()));
        xml = buildGetFeatureXml(bridges, rid, null, null, version);
        dom = postAsDOM("wfs?", xml);
        assertGetFeatures(dom, 1, bridges, commit1FeatureIdentifiers);

        // version timestamp between first and second commit, should return first
        version = new Version(new Date(commit1.getTimestamp() + 500));
        xml = buildGetFeatureXml(bridges, rid, null, null, version);
        dom = postAsDOM("wfs?", xml);
        assertGetFeatures(dom, 1, bridges, commit1FeatureIdentifiers);

        // version 2, when the bridge was updated (at Commit 3, see superclass javadocs)
        version = new Version(new Date(commit3.getTimestamp()));
        xml = buildGetFeatureXml(bridges, rid, null, null, version);
        dom = postAsDOM("wfs?", xml);
        assertGetFeatures(dom, 1, bridges, commit3FeatureIdentifiers);

        // version greater then available largest, should return available largest
        version = new Version(new Date(commit5.getTimestamp() + 5000));
        xml = buildGetFeatureXml(bridges, rid, null, null, version);
        dom = postAsDOM("wfs?", xml);
        assertGetFeatures(dom, 1, bridges, commit5FeatureIdentifiers);
    }

    public void testGetFeatureVersionIndex() throws Exception {

        String xml;
        Document dom;

        // version 1, when the buildings were inserted (at Commit 2, see superclass javadocs)
        xml = buildGetFeatureXml(buildings, "Buildings.1107531701011", null, null, new Version(Integer.valueOf(1)));
        dom = postAsDOM("wfs?", xml);
        assertGetFeatures(dom, 1, buildings, commit2FeatureIdentifiers);

        // version 2, "Moved building" commit (at Commit 4, see superclass javadocs)
        xml = buildGetFeatureXml(buildings, "Buildings.1107531701011", null, null, new Version(Integer.valueOf(2)));
        dom = postAsDOM("wfs?", xml);
        assertGetFeatures(dom, 1, buildings, commit4FeatureIdentifiers);

        // version greater than available largest, should return available largest
        xml = buildGetFeatureXml(buildings, "Buildings.1107531701011", null, null, new Version(Integer.valueOf(12)));
        dom = postAsDOM("wfs?", xml);
        assertGetFeatures(dom, 1, buildings, commit5FeatureIdentifiers);

        // but this one was deleted at commit 5, to shall match commit4 resource ids...
        xml = buildGetFeatureXml(buildings, "Buildings.1107531701010", null, null, new Version(Integer.valueOf(12)));
        dom = postAsDOM("wfs?", xml);
        assertGetFeatures(dom, 1, buildings, commit4FeatureIdentifiers);

    }

    public void testGetFeatureActionFirst() throws Exception {
        final Set<String> firstVersions = new HashSet<String>();
        firstVersions.addAll(commit1FeatureIdentifiers);// insert of Bridges
        firstVersions.addAll(commit2FeatureIdentifiers);// insert of Buildings
        String xml;
        Document dom;
        int expectedSize;
        final Version first = new Version(Action.FIRST);

        for (String rid : commit4FeatureIdentifiers) {
            String typeName = rid.startsWith("Buildings") ? buildings : bridges;
            xml = buildGetFeatureXml(typeName, rid, null, null, first);
            dom = postAsDOM("wfs?", xml);
            expectedSize = 1;
            assertGetFeatures(dom, expectedSize, typeName, firstVersions);
        }
    }

    public void testGetFeatureActionLast() throws Exception {
        final Set<String> firstVersions = new HashSet<String>();
        firstVersions.addAll(commit1FeatureIdentifiers);// insert of Bridges
        firstVersions.addAll(commit2FeatureIdentifiers);// insert of Buildings

        final Set<String> lastVersions = new HashSet<String>();
        lastVersions.addAll(commit4FeatureIdentifiers);

        String xml;
        Document dom;
        int expectedSize;
        final Version last = new Version(Action.LAST);

        for (String rid : firstVersions) {
            String typeName = rid.startsWith("Buildings") ? buildings : bridges;
            xml = buildGetFeatureXml(typeName, rid, null, null, last);
            dom = postAsDOM("wfs?", xml);
            expectedSize = 1;
            assertGetFeatures(dom, expectedSize, typeName, lastVersions);
        }
    }

    public void testGetFeatureActionAll() throws Exception {
        Set<String> allRids = new HashSet<String>(commit1FeatureIdentifiers);
        allRids.addAll(commit2FeatureIdentifiers);
        allRids.addAll(commit3FeatureIdentifiers);
        allRids.addAll(commit4FeatureIdentifiers);
        allRids.addAll(commit5FeatureIdentifiers);

        Set<String> allBuildingVersions = new HashSet<String>();
        Set<String> allBridgesVersions = new HashSet<String>();
        for (String rid : allRids) {
            if (rid.startsWith("Buildings")) {
                allBuildingVersions.add(rid);
            } else if (rid.startsWith("Bridges")) {
                allBridgesVersions.add(rid);
            } else {
                throw new IllegalStateException(rid);
            }
        }

        String xml;
        Document dom;
        int expectedSize;
        final Version all = new Version(Action.ALL);

        xml = buildGetFeatureXml(buildings, "Buildings.1107531701010", null, null, all);
        dom = postAsDOM("wfs?", xml);
        // there's only one version of this building. It was added at commit 2 and deleted at commit
        // 5
        expectedSize = 1;
        assertGetFeatures(dom, expectedSize, buildings, allBuildingVersions);

        xml = buildGetFeatureXml(buildings, "Buildings.1107531701011", null, null, all);
        dom = postAsDOM("wfs?", xml);
        // there're are two versions of this building. It was added at commit 2 and modified at
        // commit 4
        expectedSize = 2;
        assertGetFeatures(dom, expectedSize, buildings, allBuildingVersions);
    }

    public void testGetFeatureActionPrevious() throws Exception {
        final Set<String> currentVersions = new HashSet<String>();
        currentVersions.add("Bridges.1107531599613");
        currentVersions.add("Buildings.1107531701011");

        final Set<String> previousVersions = new HashSet<String>();
        // last time that bridge was modified was at commit 3, so previous should come from commit 2
        previousVersions.add(find("Bridges.1107531599613", commit2FeatureIdentifiers));
        // last time that building was modified was at commit4, so previous should come from commit3
        previousVersions.add(find("Buildings.1107531701011", commit3FeatureIdentifiers));

        String xml;
        Document dom;
        int expectedSize;
        final Version previous = new Version(Action.PREVIOUS);

        for (String rid : currentVersions) {
            String typeName = rid.startsWith("Buildings") ? buildings : bridges;
            xml = buildGetFeatureXml(typeName, rid, null, null, previous);
            dom = postAsDOM("wfs?", xml);
            expectedSize = 1;
            assertGetFeatures(dom, expectedSize, typeName, previousVersions);
        }

        // and what if there's no previous?
        final Set<String> firstVersions = new HashSet<String>();
        firstVersions.addAll(commit1FeatureIdentifiers);// insert of Bridges
        firstVersions.addAll(commit2FeatureIdentifiers);// insert of Buildings
        for (String rid : firstVersions) {
            String typeName = rid.startsWith("Buildings") ? buildings : bridges;
            xml = buildGetFeatureXml(typeName, rid, null, null, previous);
            dom = postAsDOM("wfs?", xml);
            expectedSize = 0;
            assertGetFeatures(dom, expectedSize, typeName, Collections.EMPTY_SET);
        }
    }

    private String find(String featureID, Set<String> commitFeatureIdentifiers) {
        for (String commitFid : commitFeatureIdentifiers) {
            if (commitFid.startsWith(featureID)) {
                return commitFid;
            }
        }
        throw new IllegalArgumentException(featureID + " not found in " + commitFeatureIdentifiers);
    }

    public void testGetFeatureActionNext() throws Exception {
        final Set<String> firstVersions = new HashSet<String>();
        firstVersions.add(find("Bridges.1107531599613", commit1FeatureIdentifiers));
        firstVersions.add(find("Buildings.1107531701011", commit2FeatureIdentifiers));

        final Set<String> nextVersions = new HashSet<String>();
        nextVersions.add(find("Bridges.1107531599613", commit3FeatureIdentifiers));
        nextVersions.add(find("Buildings.1107531701011", commit4FeatureIdentifiers));

        String xml;
        Document dom;
        int expectedSize;
        final Version next = new Version(Action.NEXT);

        for (String rid : firstVersions) {
            String typeName = rid.startsWith("Buildings") ? buildings : bridges;
            xml = buildGetFeatureXml(typeName, rid, null, null, next);
            dom = postAsDOM("wfs?", xml);
            expectedSize = 1;
            assertGetFeatures(dom, expectedSize, typeName, nextVersions);
        }
    }

    public void testGetFeatureBadVersion() throws Exception {
        String xml = "    <wfs:GetFeature service='WFS' version='2.0.0' " + 
                "      xmlns:wfs='http://www.opengis.net/wfs/2.0' " + 
                "      xmlns:fes='http://www.opengis.net/fes/2.0'> " + 
                "      <wfs:Query typeNames='cite:Buildings'> " + 
                "        <fes:Filter> " + 
                "          <fes:ResourceId rid='Buildings.1107531701010' version='-1'/> " + 
                "        </fes:Filter> " + 
                "      </wfs:Query> " + 
                "    </wfs:GetFeature> ";
        Document dom = postAsDOM("wfs", xml);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("OperationProcessingFailed", "//ows:Exception/@exceptionCode", dom);
    }

    /**
     * Builds an XML GetFeature request
     * 
     * @param typeName
     *            the type name to query, non null
     * @param rid
     *            the resource id to query, if null no filter will be generated
     * @param startDate
     *            the resourceId startDate, may be null, and only used if {@code rid != null}
     * @param endDate
     *            the resourceId endDate, may be null, and only used if {@code rid != null}
     * @param version
     *            the resourceId version action predicate, may be null, and only used if
     *            {@code rid != null}
     */
    private String buildGetFeatureXml(final String typeName, String rid, Date startDate,
            Date endDate, Version version) {

        StringBuilder sb = new StringBuilder();
        sb.append("<wfs:GetFeature service='WFS' version='2.0.0' ");
        sb.append(" xmlns:fes='" + FES.NAMESPACE + "' ");
        sb.append(" xmlns:cite='http://www.opengis.net/cite' ");
        sb.append(" xmlns:wfs='http://www.opengis.net/wfs/2.0' " + ">\n");
        sb.append(" <wfs:Query typeNames='" + typeName + "'>\n");
        if (rid != null) {
            sb.append("  <fes:Filter>\n");
            sb.append("   <fes:ResourceId rid='" + rid + "' \n");
            if (startDate != null) {
                sb.append("        startDate='" + serializeDateTime(startDate.getTime(), true)
                        + "'\n");
            }
            if (endDate != null) {
                sb.append("        endDate='" + serializeDateTime(endDate.getTime(), true) + "'\n");
            }
            if (version != null) {
                sb.append("        version='");
                if (version.getDateTime() != null) {
                    sb.append(serializeDateTime(version.getDateTime().getTime(), true));
                } else if (version.getIndex() != null) {
                    sb.append(version.getIndex());
                } else if (version.getVersionAction() != null) {
                    sb.append(version.getVersionAction());
                }
                sb.append("'");
            }
            sb.append("/>\n");
            sb.append("  </fes:Filter>\n");
        }
        sb.append(" </wfs:Query> ");
        sb.append("</wfs:GetFeature>");

        String xml = sb.toString();
        return xml;
    }
}
