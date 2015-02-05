/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.util;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ExecutorService;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.DisposableBean;

/**
 * 
 * @author Kevin Smith, Boundless
 *
 */
public class DisposerTest {
    
    Disposer disp;
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Before
    public void setUp() {
        disp = new Disposer();
    }
    
    @Test
    public void testDisposes() throws Exception {
        DisposableBean bean = createMock(DisposableBean.class);
        
        bean.destroy();EasyMock.expectLastCall().once();
        
        replay(bean);
        disp.register(bean);
        disp.destroy();
        verify(bean);
    }
    
    @Test
    public void testWontDisposeOfSelf() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        disp.register(disp);
    }
    
    @Test
    public void testWaitsToDispose() throws Exception {
        DisposableBean bean = createMock(DisposableBean.class);
        
        // Don't call destroy
        
        replay(bean);
        disp.register(bean);
        verify(bean);
    }
    
    @Test
    public void testExceptionIfAlreadyDisposed() throws Exception {
        DisposableBean bean = createMock(DisposableBean.class);
        
        bean.destroy();EasyMock.expectLastCall().once();
        
        replay(bean);
        
        disp.destroy();
        
        expectedException.expect(IllegalArgumentException.class);
        try {
            disp.register(bean);
        } finally {
            verify(bean);
        }
    }
    
    @Test
    public void testIdempotent() throws Exception {
        DisposableBean bean = createMock(DisposableBean.class);
        
        bean.destroy();EasyMock.expectLastCall().once();
        
        replay(bean);
        
        disp.register(bean);
        disp.destroy();
        disp.destroy();
        
        verify(bean);
    }
    
    @Test
    public void testMultiple() throws Exception {
        DisposableBean bean1 = createMock("bean1", DisposableBean.class);
        DisposableBean bean2 = createMock("bean2", DisposableBean.class);
        
        bean1.destroy();EasyMock.expectLastCall().once();
        bean2.destroy();EasyMock.expectLastCall().once();
        
        replay(bean1, bean2);
        
        disp.register(bean1);
        disp.register(bean2);
        disp.destroy();
        
        verify(bean1, bean2);
    }
    
    @Test
    public void testHandleException() throws Exception {
        DisposableBean bean1 = createMock("bean1", DisposableBean.class);
        DisposableBean bean2 = createMock("bean2", DisposableBean.class);
        
        bean1.destroy();EasyMock.expectLastCall().andThrow(new IllegalArgumentException("TEST!"));
        bean2.destroy();EasyMock.expectLastCall().andThrow(new IllegalArgumentException("TEST!"));
        
        replay(bean1, bean2);
        
        disp.register(bean1);
        disp.register(bean2);
        disp.destroy();
        
        verify(bean1, bean2);
    }
    
    @Test
    public void testReplace() throws Exception {
        DisposableBean bean1 = createMock("bean1", DisposableBean.class);
        DisposableBean bean2 = createMock("bean2", DisposableBean.class);
        DisposableBean bean3 = createMock("bean3", DisposableBean.class);
        
        bean1.destroy();EasyMock.expectLastCall().once();
       
        replay(bean1, bean2, bean3);
        
        disp.register(bean1);
        disp.register(bean2);
        disp.replace(bean1, bean3);
        
        verify(bean1, bean2, bean3);
        reset(bean1, bean2, bean3);
        
        bean2.destroy();EasyMock.expectLastCall().once();
        bean3.destroy();EasyMock.expectLastCall().once();
        
        replay(bean1, bean2, bean3);
       
        disp.destroy();
        
        verify(bean1, bean2, bean3);
    }
    
    @Test
    public void testReplaceNotRegistered() throws Exception {
        DisposableBean bean1 = createMock("bean1", DisposableBean.class);
        DisposableBean bean2 = createMock("bean2", DisposableBean.class);
        DisposableBean bean3 = createMock("bean3", DisposableBean.class);
        
       
        replay(bean1, bean2, bean3);
        
        disp.register(bean2);
        disp.replace(bean1, bean3);
        
        verify(bean1, bean2, bean3);
        reset(bean1, bean2, bean3);
        
        bean2.destroy();EasyMock.expectLastCall().once();
        bean3.destroy();EasyMock.expectLastCall().once();
        
        replay(bean1, bean2, bean3);
       
        disp.destroy();
        
        verify(bean1, bean2, bean3);
    }
    
    @Test
    public void testReplaceNull() throws Exception {
        DisposableBean bean1 = createMock("bean1", DisposableBean.class);
        DisposableBean bean2 = createMock("bean2", DisposableBean.class);
        DisposableBean bean3 = createMock("bean3", DisposableBean.class);
        
       
        replay(bean1, bean2, bean3);
        
        disp.register(bean2);
        disp.replace(null, bean3);
        
        verify(bean1, bean2, bean3);
        reset(bean1, bean2, bean3);
        
        bean2.destroy();EasyMock.expectLastCall().once();
        bean3.destroy();EasyMock.expectLastCall().once();
        
        replay(bean1, bean2, bean3);
       
        disp.destroy();
        
        verify(bean1, bean2, bean3);
    }
    
    @Test
    public void testReplaceWhenDisposed() throws Exception {
        DisposableBean bean1 = createMock("bean1", DisposableBean.class);
        DisposableBean bean2 = createMock("bean2", DisposableBean.class);
        DisposableBean bean3 = createMock("bean3", DisposableBean.class);
        
        bean1.destroy();EasyMock.expectLastCall().once();
        bean2.destroy();EasyMock.expectLastCall().once();
       
        replay(bean1, bean2, bean3);
        
        disp.register(bean1);
        disp.register(bean2);
        
        disp.destroy();
        
        verify(bean1, bean2, bean3);
        reset(bean1, bean2, bean3);
        
        bean3.destroy();EasyMock.expectLastCall().once();
        
        replay(bean1, bean2, bean3);
        
        expectedException.expect(IllegalArgumentException.class);
        try{
            disp.replace(bean1, bean3);
        } finally {
            verify(bean1, bean2, bean3);
        }
    }
    
    @Test
    public void testEarlyDisposal() throws Exception {
        DisposableBean bean1 = createMock("bean1", DisposableBean.class);
        DisposableBean bean2 = createMock("bean2", DisposableBean.class);
        DisposableBean bean3 = createMock("bean3", DisposableBean.class);
        
        bean2.destroy();EasyMock.expectLastCall().once();
       
        replay(bean1, bean2, bean3);
        
        disp.register(bean1);
        disp.register(bean2);
        
        assertThat(disp.disposeOf(bean2), is(true));
        
        verify(bean1, bean2, bean3);
        reset(bean1, bean2, bean3);
        
        bean3.destroy();EasyMock.expectLastCall().once();
        bean1.destroy();EasyMock.expectLastCall().once();
        
        replay(bean1, bean2, bean3);
        
        disp.register(bean3);
        disp.destroy();
        
        verify(bean1, bean2, bean3);
    }
    
    @Test
    public void testEarlyDisposalNotRegistered() throws Exception {
        DisposableBean bean1 = createMock("bean1", DisposableBean.class);
        DisposableBean bean2 = createMock("bean2", DisposableBean.class);
        DisposableBean bean3 = createMock("bean3", DisposableBean.class);
        
        //bean2.destroy();EasyMock.expectLastCall().once();
       
        replay(bean1, bean2, bean3);
        
        disp.register(bean1);
        //disp.register(bean2);
        
        assertThat(disp.disposeOf(bean2), is(false));
        
        verify(bean1, bean2, bean3);
        reset(bean1, bean2, bean3);
        
        bean3.destroy();EasyMock.expectLastCall().once();
        bean1.destroy();EasyMock.expectLastCall().once();
        
        replay(bean1, bean2, bean3);
        
        disp.register(bean3);
        disp.destroy();
        
        verify(bean1, bean2, bean3);
    }
    
    @Test
    public void testEarlyDisposalNull() throws Exception {
        DisposableBean bean1 = createMock("bean1", DisposableBean.class);
        DisposableBean bean2 = createMock("bean2", DisposableBean.class);
        DisposableBean bean3 = createMock("bean3", DisposableBean.class);
        
        //bean2.destroy();EasyMock.expectLastCall().once();
       
        replay(bean1, bean2, bean3);
        
        disp.register(bean1);
        //disp.register(bean2);
        
        assertThat(disp.disposeOf(null), is(false));
        
        verify(bean1, bean2, bean3);
        reset(bean1, bean2, bean3);
        
        bean3.destroy();EasyMock.expectLastCall().once();
        bean1.destroy();EasyMock.expectLastCall().once();
        
        replay(bean1, bean2, bean3);
        
        disp.register(bean3);
        disp.destroy();
        
        verify(bean1, bean2, bean3);
    }
    
    @Test
    public void testEarlyDisposalFail() throws Exception {
        DisposableBean bean1 = createMock("bean1", DisposableBean.class);
        DisposableBean bean2 = createMock("bean2", DisposableBean.class);
        DisposableBean bean3 = createMock("bean3", DisposableBean.class);
        
        bean2.destroy();EasyMock.expectLastCall().andThrow(new IllegalStateException("TEST!"));
       
        replay(bean1, bean2, bean3);
        
        disp.register(bean1);
        disp.register(bean2);
        
        assertThat(disp.disposeOf(bean2), is(false));
        
        verify(bean1, bean2, bean3);
        reset(bean1, bean2, bean3);
        
        bean3.destroy();EasyMock.expectLastCall().once();
        bean1.destroy();EasyMock.expectLastCall().once();
        
        replay(bean1, bean2, bean3);
        
        disp.register(bean3);
        disp.destroy();
        
        verify(bean1, bean2, bean3);
    }
    
    @Test
    public void testDisposesCloseables() throws Exception {
        AutoCloseable bean = createMock(AutoCloseable.class);
        
        bean.close();EasyMock.expectLastCall().once();
        
        replay(bean);
        disp.register(bean);
        disp.destroy();
        verify(bean);
    }
    
    @Test
    public void testDisposesExecutors() throws Exception {
        ExecutorService bean = createMock(ExecutorService.class);
        
        bean.shutdown();EasyMock.expectLastCall().once();
        
        replay(bean);
        disp.register(bean);
        disp.destroy();
        verify(bean);
    }
    
    interface TestInterface {};
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCustomDestroyer() throws Exception {
        TestInterface bean = createMock("bean", TestInterface.class);
        Disposer.Destroyer<TestInterface> destroyer = createMock("destroyer", Disposer.Destroyer.class);
        
        destroyer.accept(bean);EasyMock.expectLastCall().once();
        
        replay(bean,destroyer);
        disp.register(bean, destroyer);
        disp.destroy();
        verify(bean,destroyer);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceDestroyer() throws Exception {
        TestInterface bean = createMock(TestInterface.class);
        Disposer.Destroyer<TestInterface> destroyer1 = createMock(Disposer.Destroyer.class);
        Disposer.Destroyer<TestInterface> destroyer2 = createMock(Disposer.Destroyer.class);
        
        destroyer2.accept(bean);EasyMock.expectLastCall().once();
        
        replay(bean,destroyer1, destroyer2);
        disp.register(bean, destroyer1);
        disp.register(bean, destroyer2);
        disp.destroy();
        verify(bean,destroyer1, destroyer2);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCustomDestroyerFail() throws Exception {
        TestInterface bean = createMock("bean", TestInterface.class);
        Disposer.Destroyer<TestInterface> destroyer = createMock("destroyer", Disposer.Destroyer.class);
        
        destroyer.accept(bean);EasyMock.expectLastCall().andThrow(new IllegalArgumentException("TEST!"));
        
        replay(bean,destroyer);
        disp.register(bean, destroyer);
        disp.destroy();
        verify(bean,destroyer);
    }
}
