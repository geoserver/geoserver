/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.easymock.EasyMock;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wps.ProcessEvent;
import org.geoserver.wps.ProcessListener;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.NameImpl;
import org.junit.Before;
import org.junit.Test;

public class CoverageResourceListenerTest {

    private WPSResourceManager resourceManager;

    private ProcessListener listener;

    private ExecutionStatus status;

    private Map<String, Object> inputs;

    @Before
    public void setUp() {
        this.resourceManager = createMock(WPSResourceManager.class);
        this.listener =
                new CoverageResourceListener(this.resourceManager, new CoverageCleanerCallback());
        this.status =
                new ExecutionStatus(
                        new NameImpl("gs", "TestProcess"), UUID.randomUUID().toString(), false);
        this.status.setPhase(ProcessState.RUNNING);
        this.inputs = new HashMap<>();
        this.inputs.put("coverageA", null);
        this.inputs.put("coverageB", null);
        this.inputs.put("string", null);
        this.inputs.put("integer", null);
    }

    @Test
    public void testCheckInputWhenSucceeded() {
        // expected addResource to be called twice
        this.resourceManager.addResource(EasyMock.<GridCoverageResource>anyObject());
        expectLastCall().times(2);
        replay(this.resourceManager);

        this.listener.progress(new ProcessEvent(this.status, this.inputs));
        this.inputs.put("coverageA", createMock(GridCoverage2D.class));
        this.listener.progress(new ProcessEvent(this.status, this.inputs));
        this.inputs.put("coverageB", createMock(GridCoverage2D.class));
        this.listener.progress(new ProcessEvent(this.status, this.inputs));
        this.inputs.put("string", "testString");
        this.listener.progress(new ProcessEvent(this.status, this.inputs));
        this.inputs.put("integer", 1);
        this.listener.progress(new ProcessEvent(this.status, this.inputs));
        this.listener.progress(new ProcessEvent(this.status, this.inputs));
        this.status.setPhase(ProcessState.SUCCEEDED);
        this.listener.succeeded(new ProcessEvent(this.status, this.inputs));

        // verify that addResource was called twice
        verify(this.resourceManager);
    }

    @Test
    public void testCheckInputWhenFailed() {
        // expected addResource to be called once
        this.resourceManager.addResource(EasyMock.<GridCoverageResource>anyObject());
        expectLastCall().once();
        replay(this.resourceManager);

        // failure loading second coverage
        this.listener.progress(new ProcessEvent(this.status, this.inputs));
        this.inputs.put("coverageA", createMock(GridCoverage2D.class));
        this.listener.progress(new ProcessEvent(this.status, this.inputs));
        this.status.setPhase(ProcessState.FAILED);
        this.listener.failed(new ProcessEvent(this.status, this.inputs));

        // verify that addResource was called once
        verify(this.resourceManager);
    }
}
