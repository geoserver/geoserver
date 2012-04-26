package org.geoserver.wfs.versioning;

import static org.geotools.feature.type.DateUtil.serializeDateTime;

import java.util.Date;

import org.geoserver.config.GeoServer;
import org.geoserver.wfs.WFSInfo;
import org.geotools.filter.v2_0.FES;
import org.opengis.filter.identity.Version;
import org.w3c.dom.Document;

public class GetFeatureResourceIdTest extends WFS20VersioningTestSupport {

    public void testStartEndTime() throws Exception {
        final String fid = "Buildings.1107531701011";
        Date startDate = new Date(commit4.getTimestamp() - 500);
        Date endDate = null;
        Version version = null;
        String xml = buildGetFeatureXml("cite:Buildings", fid, startDate, endDate, version);
        Document dom = postAsDOM("wfs?", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        
        GeoServer geoServer = getGeoServer();
        WFSInfo service = geoServer.getService(WFSInfo.class);
        service.setCiteCompliant(true);
        geoServer.save(service);
        
        dom = postAsDOM("wfs?", xml);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    }

    public void testStartEndTimeWithVersion() throws Exception {
        final String fid = "Buildings.1107531701011";
        Date startDate = new Date(commit4.getTimestamp() - 500);
        Date endDate = new Date(commit5.getTimestamp());
        
        Version version = new Version(Integer.valueOf(1));
        String xml = buildGetFeatureXml("cite:Buildings", fid, startDate, endDate, version);
        Document dom = postAsDOM("wfs?", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        
        GeoServer geoServer = getGeoServer();
        WFSInfo service = geoServer.getService(WFSInfo.class);
        service.setCiteCompliant(true);
        geoServer.save(service);
        
        dom = postAsDOM("wfs?", xml);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
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
