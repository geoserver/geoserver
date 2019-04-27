/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.geogig.geoserver.config.LogStore;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.porcelain.BlameOp;
import org.locationtech.geogig.porcelain.CleanOp;
import org.locationtech.geogig.porcelain.DiffOp;
import org.locationtech.geogig.remotes.CloneOp;
import org.locationtech.geogig.remotes.FetchOp;
import org.locationtech.geogig.remotes.PullOp;
import org.locationtech.geogig.remotes.PushOp;
import org.locationtech.geogig.remotes.RemoteAddOp;
import org.locationtech.geogig.remotes.RemoteRemoveOp;
import org.locationtech.geogig.repository.Remote;
import org.mockito.ArgumentCaptor;

public class SecurityLoggerTest {

    @SuppressWarnings("unused")
    private SecurityLogger logger;

    private LogStore mockStore;

    @Before
    public void before() throws Exception {
        mockStore = mock(LogStore.class);
        logger = new SecurityLogger(mockStore);
        logger.afterPropertiesSet();
    }

    @Test
    public void testCommandsOfInterest() {
        assertTrue(SecurityLogger.interestedIn(RemoteAddOp.class));
        assertTrue(SecurityLogger.interestedIn(RemoteRemoveOp.class));
        assertTrue(SecurityLogger.interestedIn(PushOp.class));
        assertTrue(SecurityLogger.interestedIn(PullOp.class));
        assertTrue(SecurityLogger.interestedIn(FetchOp.class));
        assertTrue(SecurityLogger.interestedIn(CloneOp.class));

        assertFalse(SecurityLogger.interestedIn(BlameOp.class));
        assertFalse(SecurityLogger.interestedIn(CleanOp.class));
        assertFalse(SecurityLogger.interestedIn(DiffOp.class));
        assertFalse(SecurityLogger.interestedIn(RefParse.class));
    }

    @Test
    public void testRemoteAdd() {
        RemoteAddOp command = new RemoteAddOp();
        String remoteName = "upstream";
        String mappedBranch = "master";
        String username = "gabriel";
        String password = "passw0rd";

        String fetchurl = "http://demo.example.com/testrepo";
        String pushurl = fetchurl;
        String fetch =
                "+"
                        + Ref.append(Ref.HEADS_PREFIX, mappedBranch)
                        + ":"
                        + Ref.append(Ref.append(Ref.REMOTES_PREFIX, remoteName), mappedBranch);
        boolean mapped = true;

        command.setName(remoteName)
                .setBranch(mappedBranch)
                .setMapped(mapped)
                .setPassword(password)
                .setURL(username)
                .setURL(fetchurl);

        ArgumentCaptor<CharSequence> arg = ArgumentCaptor.forClass(CharSequence.class);
        SecurityLogger.logPre(command);
        verify(mockStore).debug(anyString(), arg.capture());

        // Remote add: Parameters: name='upstream', url='http://demo.example.com/testrepo'
        String msg = String.valueOf(arg.getValue());
        assertTrue(msg.startsWith("Remote add"));
        assertTrue(msg.contains(remoteName));
        assertTrue(msg.contains(fetchurl));

        Remote retVal =
                new Remote(
                        remoteName,
                        fetchurl,
                        pushurl,
                        fetch,
                        mapped,
                        mappedBranch,
                        username,
                        password);
        SecurityLogger.logPost(command, retVal, null);
        verify(mockStore).info(anyString(), arg.capture());

        msg = String.valueOf(arg.getValue());
        assertTrue(msg.startsWith("Remote add success"));
        assertTrue(msg.contains(remoteName));
        assertTrue(msg.contains(fetchurl));

        ArgumentCaptor<Throwable> exception = ArgumentCaptor.forClass(Throwable.class);
        SecurityLogger.logPost(command, null, new RuntimeException("test exception"));
        verify(mockStore).error(anyString(), arg.capture(), exception.capture());

        msg = String.valueOf(arg.getValue());
        assertTrue(msg.startsWith("Remote add failed"));
        assertTrue(msg.contains(remoteName));
        assertTrue(msg.contains(fetchurl));
    }
}
