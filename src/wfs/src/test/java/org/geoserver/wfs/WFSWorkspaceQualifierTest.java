/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Collections;
import net.opengis.wfs20.AbstractTransactionActionType;
import net.opengis.wfs20.ReplaceType;
import net.opengis.wfs20.TransactionType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Tests for {@link WFSWorkspaceQualifier}.
 *
 * @author awaterme
 */
public class WFSWorkspaceQualifierTest {

    private static final String WORKSPACE_URI = "http://workspace/namespace";
    private Catalog mockCatalog = createMock(Catalog.class);
    private WorkspaceInfo mockWorkspaceInfo = createMock(WorkspaceInfo.class);
    private NamespaceInfo mockNamespaceInfo = createMock(NamespaceInfo.class);
    private FeatureType mockFeatureType = createMock(FeatureType.class);
    private Feature mockFeature = createMock(Feature.class);
    private Name mockName = createMock(Name.class);

    private WFSWorkspaceQualifier sut = new WFSWorkspaceQualifier(mockCatalog);

    @Before
    public void setup() {
        String workspaceName = "workspacename";

        expect(mockCatalog.getNamespaceByPrefix(workspaceName)).andReturn(mockNamespaceInfo);
        expect(mockWorkspaceInfo.getName()).andReturn(workspaceName);
        expect(mockNamespaceInfo.getURI()).andReturn(WORKSPACE_URI);
        expect(mockFeature.getType()).andReturn(mockFeatureType);
        expect(mockFeatureType.getName()).andReturn(mockName);
    }

    /**
     * Test for {@link WFSWorkspaceQualifier#qualifyRequest(WorkspaceInfo,
     * org.geoserver.catalog.LayerInfo, Operation, org.geoserver.ows.Request)} .Simulates a WFS-T
     * Replace, having one Feature. The namespaceURI of the workspace and the feature match. Result:
     * No exception.
     */
    @Test
    public void testQualifyRequestWithReplaceNamespaceValidationHavingMatchingNamespaces() {
        expect(mockName.getNamespaceURI()).andReturn(WORKSPACE_URI).anyTimes();
        invokeQualifyRequest();
    }

    /**
     * Test for {@link WFSWorkspaceQualifier#qualifyRequest(WorkspaceInfo,
     * org.geoserver.catalog.LayerInfo, Operation, org.geoserver.ows.Request)} . Simulates a WFS-T
     * Replace, having one Feature. The namespaceURI of the workspace and the feature do not match.
     * Result: Exception.
     */
    @Test(expected = WFSException.class)
    public void testQualifyRequestWithReplaceNamespaceValidationHavingNonMatchingNamespaces() {
        expect(mockName.getNamespaceURI()).andReturn("http://foo").anyTimes();
        invokeQualifyRequest();
    }

    private void invokeQualifyRequest() {
        TransactionType transactionType = Wfs20Factory.eINSTANCE.createTransactionType();
        ReplaceType replaceType = Wfs20Factory.eINSTANCE.createReplaceType();
        EList<AbstractTransactionActionType> action =
                transactionType.getAbstractTransactionAction();
        action.add(replaceType);
        replaceType.getAny().add(mockFeature);

        Version version = new Version("2.0.0");
        Service service =
                new Service("id", "service", version, Collections.singletonList("Transaction"));
        Operation operation = new Operation("id", service, null, new Object[] {transactionType});

        replay(
                mockCatalog,
                mockFeature,
                mockFeatureType,
                mockName,
                mockNamespaceInfo,
                mockWorkspaceInfo);
        sut.qualifyRequest(mockWorkspaceInfo, null, operation, null);
    }
}
