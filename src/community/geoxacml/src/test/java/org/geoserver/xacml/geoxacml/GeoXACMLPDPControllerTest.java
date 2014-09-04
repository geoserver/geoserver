/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.geoxacml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.ProviderManager;
import org.springframework.security.providers.TestingAuthenticationProvider;
import org.springframework.security.providers.TestingAuthenticationToken;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessMode;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.xacml.role.XACMLRole;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.xacml.Indenter;
import com.sun.xacml.ctx.RequestCtx;

public class GeoXACMLPDPControllerTest extends GeoServerTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        ProviderManager providerManager = (ProviderManager) GeoServerExtensions
                .bean("authenticationManager");
        List<AuthenticationProvider> list = new ArrayList<AuthenticationProvider>();
        list.add(new TestingAuthenticationProvider());
        providerManager.setProviders(list);

        Authentication admin = new TestingAuthenticationToken("admin", "geoserver",
                new GrantedAuthority[] { new XACMLRole("ROLE_ADMINISTRATOR") });
        // Authentication anonymous = new TestingAuthenticationToken("anonymous", null, null);
        SecurityContextHolder.getContext().setAuthentication(admin);

    }

    public void testDirExists() throws Exception {
        File dir = new File(testData.getDataDirectoryRoot(), DataDirPolicyFinderModlule.BASE_DIR);
        assertTrue(dir.exists());

    }

    public void testRemote() throws Exception {

        List<RequestCtx> requestCtxs = createRequestCtxList();

        for (RequestCtx requestCtx : requestCtxs) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            requestCtx.encode(out, new Indenter(0), true);
            InputStream resp = post("security/geoxacml", out.toString());
            checkXACMLRepsonse(resp, "Permit");
        }
    }

    public void testCatalogReload() throws Exception {
        // System.out.println(getAsString("/rest/reloadXACML.txt"));
        assertEquals(GeoXACMLRESTRepositoryReloader.ReloadedMsg,
                getAsString("/rest/reloadXACML.txt"));
    }

    private List<RequestCtx> createRequestCtxList() {
        List<RequestCtx> result = new ArrayList<RequestCtx>();
        for (WorkspaceInfo wsInfo : getCatalog().getWorkspaces()) {
            for (GrantedAuthority role : SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities()) {
                RequestCtx rctx = GeoXACMLConfig.getRequestCtxBuilderFactory()
                        .getWorkspaceRequestCtxBuilder((XACMLRole) role, wsInfo, AccessMode.READ)
                        .createRequestCtx();
                result.add(rctx);
            }
        }
        return result;

    }

    protected void checkXACMLRepsonse(InputStream resp, String decision) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(resp);
        Node decisionNode = doc.getElementsByTagName("Decision").item(0);
        assertEquals(decision, decisionNode.getTextContent());
        Node statusNode = doc.getElementsByTagName("StatusCode").item(0);
        String statusCode = statusNode.getAttributes().getNamedItem("Value").getTextContent();
        assertEquals("urn:oasis:names:tc:xacml:1.0:status:ok", statusCode);

    }

    protected void dumpResponse(InputStream resp) throws IOException {
        System.out.println("RESPONSE");
        byte[] bytes = new byte[512];
        while (resp.read(bytes) != -1) {
            System.out.println(new String(bytes));
        }
    }

}
