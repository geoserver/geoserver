/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.util;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.IAnswer;
import org.geotools.util.logging.LoggerAdapter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.ExpectedException;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class LoggerRuleTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    @Test
    public void testDoNothingUntilRun() {
        Logger log = createMock("log", Logger.class);
        Description desc = createMock("desc", Description.class);
        Statement base = createMock("base", Statement.class);

        replay(log, desc, base);
        LoggerRule rule = new LoggerRule(log, Level.FINE);
        rule.apply(base, desc);
        verify(log, desc, base);
    }

    @Test
    public void testCleansUp() throws Throwable {
        final Logger log = createMock("log", Logger.class);
        Description desc = createMock("desc", Description.class);
        Statement base = createMock("base", Statement.class);

        expect(log.getLevel()).andReturn(Level.OFF);
        log.setLevel(Level.FINE);
        expectLastCall().once();
        final Capture<Handler> handlerCap = Capture.newInstance(CaptureType.LAST);
        log.addHandler(capture(handlerCap));
        expectLastCall().once();
        base.evaluate();
        expectLastCall()
                .andAnswer(
                        new IAnswer<Void>() {

                            @Override
                            public Void answer() throws Throwable {
                                verify(log);
                                reset(log);

                                log.removeHandler(handlerCap.getValue());
                                expectLastCall().once();
                                log.setLevel(Level.OFF);
                                expectLastCall().once();

                                replay(log);
                                return null;
                            }
                        });
        replay(log, desc, base);
        LoggerRule rule = new LoggerRule(log, Level.FINE);
        Statement s = rule.apply(base, desc);
        s.evaluate();
        verify(log, desc, base);
    }

    @Test
    public void testCleansUpAfterException() throws Throwable {
        final Logger log = createMock("log", Logger.class);
        Description desc = createMock("desc", Description.class);
        Statement base = createMock("base", Statement.class);

        expect(log.getLevel()).andReturn(Level.OFF);
        log.setLevel(Level.FINE);
        expectLastCall().once();
        final Capture<Handler> handlerCap = Capture.newInstance(CaptureType.LAST);
        final Exception ex = new IllegalArgumentException();
        log.addHandler(capture(handlerCap));
        expectLastCall().once();
        base.evaluate();
        expectLastCall()
                .andAnswer(
                        new IAnswer<Void>() {

                            @Override
                            public Void answer() throws Throwable {
                                verify(log);
                                reset(log);

                                log.removeHandler(handlerCap.getValue());
                                expectLastCall().once();
                                log.setLevel(Level.OFF);
                                expectLastCall().once();

                                replay(log);
                                throw ex;
                            }
                        });
        replay(log, desc, base);
        LoggerRule rule = new LoggerRule(log, Level.FINE);
        Statement s = rule.apply(base, desc);

        exception.expect(sameInstance(ex));
        try {
            s.evaluate();
        } finally {
            verify(log, desc, base);
        }
    }

    @Test
    public void testRecordsLogs() throws Throwable {
        final Logger log = createMock("log", Logger.class);
        Description desc = createMock("desc", Description.class);
        Statement base = createMock("base", Statement.class);

        expect(log.getLevel()).andReturn(Level.OFF);
        log.setLevel(Level.FINE);
        expectLastCall().once();
        final Capture<Handler> handlerCap = Capture.newInstance(CaptureType.LAST);
        final Exception ex = new IllegalArgumentException();

        final LogRecord record = createMock("record1", LogRecord.class);

        log.addHandler(capture(handlerCap));
        expectLastCall().once();
        base.evaluate();
        expectLastCall()
                .andAnswer(
                        new IAnswer<Void>() {

                            @Override
                            public Void answer() throws Throwable {
                                verify(log);
                                reset(log);

                                log.removeHandler(handlerCap.getValue());
                                expectLastCall().once();
                                log.setLevel(Level.OFF);
                                expectLastCall().once();

                                handlerCap.getValue().publish(record);
                                assertThat(
                                        ((LoggerRule) handlerCap.getValue()).records(),
                                        contains(record));
                                replay(log);
                                return null;
                            }
                        });
        replay(log, desc, base);
        LoggerRule rule = new LoggerRule(log, Level.FINE);
        Statement s = rule.apply(base, desc);

        s.evaluate();
        verify(log, desc, base);
    }

    @Test
    public void testAssertFail() throws Throwable {
        final Logger log = createMock("log", Logger.class);
        Description desc = createMock("desc", Description.class);
        Statement base = createMock("base", Statement.class);

        expect(log.getLevel()).andReturn(Level.OFF);
        log.setLevel(Level.FINE);
        expectLastCall().once();
        final Capture<Handler> handlerCap = Capture.newInstance(CaptureType.LAST);

        final LogRecord record = createMock("record1", LogRecord.class);

        log.addHandler(capture(handlerCap));
        expectLastCall().once();
        base.evaluate();
        expectLastCall()
                .andAnswer(
                        new IAnswer<Void>() {

                            @Override
                            public Void answer() throws Throwable {
                                verify(log);
                                reset(log);

                                log.removeHandler(handlerCap.getValue());
                                expectLastCall().once();
                                log.setLevel(Level.OFF);
                                expectLastCall().once();
                                replay(log);

                                handlerCap.getValue().publish(record);
                                ((LoggerRule) handlerCap.getValue())
                                        .assertLogged(sameInstance(record));
                                ((LoggerRule) handlerCap.getValue()).assertLogged(not(anything()));
                                return null;
                            }
                        });
        replay(log, desc, base);
        LoggerRule rule = new LoggerRule(log, Level.FINE);
        Statement s = rule.apply(base, desc);

        try {
            s.evaluate();
            fail("Expected Assertion Exception");
        } catch (AssertionError ex) {
            assertThat(ex, hasProperty("message", containsString("Expected record")));
        } finally {
            verify(log, desc, base);
        }
    }

    @Test
    public void testAssertPass() throws Throwable {
        final Logger log = createMock("log", Logger.class);
        Description desc = createMock("desc", Description.class);
        Statement base = createMock("base", Statement.class);

        expect(log.getLevel()).andReturn(Level.OFF);
        log.setLevel(Level.FINE);
        expectLastCall().once();
        final Capture<Handler> handlerCap = Capture.newInstance(CaptureType.LAST);

        final LogRecord record = createMock("record1", LogRecord.class);

        log.addHandler(capture(handlerCap));
        expectLastCall().once();
        base.evaluate();
        expectLastCall()
                .andAnswer(
                        new IAnswer<Void>() {

                            @Override
                            public Void answer() throws Throwable {
                                verify(log);
                                reset(log);

                                log.removeHandler(handlerCap.getValue());
                                expectLastCall().once();
                                log.setLevel(Level.OFF);
                                expectLastCall().once();
                                replay(log);

                                handlerCap.getValue().publish(record);
                                ((LoggerRule) handlerCap.getValue())
                                        .assertLogged(sameInstance(record));
                                return null;
                            }
                        });
        replay(log, desc, base);
        LoggerRule rule = new LoggerRule(log, Level.FINE);
        Statement s = rule.apply(base, desc);

        try {
            s.evaluate();
        } finally {
            verify(log, desc, base);
        }
    }

    @Test
    public void testAdapter() throws Throwable {
        final Logger log = createMock("log", LoggerAdapter.class);
        Description desc = createMock("desc", Description.class);
        Statement base = createMock("base", Statement.class);

        expect(log.getLevel()).andReturn(Level.OFF);
        log.setLevel(Level.FINE);
        expectLastCall().once();
        final Capture<Handler> handlerCap = Capture.newInstance(CaptureType.LAST);

        final LogRecord record = createMock("record1", LogRecord.class);

        log.addHandler(capture(handlerCap));
        expectLastCall().once();
        base.evaluate();
        expectLastCall()
                .andAnswer(
                        new IAnswer<Void>() {

                            @Override
                            public Void answer() throws Throwable {
                                verify(log);
                                reset(log);

                                log.removeHandler(handlerCap.getValue());
                                expectLastCall().once();
                                log.setLevel(Level.OFF);
                                expectLastCall().once();
                                replay(log);

                                handlerCap.getValue().publish(record);
                                ((LoggerRule) handlerCap.getValue())
                                        .assertLogged(sameInstance(record));
                                return null;
                            }
                        });
        replay(log, desc, base);
        LoggerRule rule = new LoggerRule(log, Level.FINE);
        Statement s = rule.apply(base, desc);

        try {
            s.evaluate();
        } catch (AssumptionViolatedException ex) {
            if (!ex.getMessage().equals("LoggerRule can't capture logs for LoggerAdapter")) {
                throw ex;
            }
            // Eventually hopefully we can handle this case and we can fail if this particular
            // assumption failure occurs.  For now it's a pass rather than an ignore.
        } finally {
            verify(log, desc, base);
        }
    }
}
