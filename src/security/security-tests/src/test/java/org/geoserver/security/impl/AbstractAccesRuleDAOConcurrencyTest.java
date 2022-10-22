/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractAccesRuleDAOConcurrencyTest<D extends AbstractAccessRuleDAO> {

    private static final int LOOPS = 10000;
    private static final int THREADS = 4;
    D dao;
    private ExecutorService executors;

    @Before
    public void setUp() throws Exception {
        dao = buildDAO();

        executors = Executors.newFixedThreadPool(THREADS);
    }

    /**
     * Builds the DAO that will be tested
     *
     * @return
     */
    protected abstract D buildDAO() throws Exception;

    @After
    public void shutdown() {
        executors.shutdown();
    }

    @Test
    public void testConcurrentModifications() throws Exception {
        // REST operations change the contents of the DAO by using
        List<Callable<Void>> manipulators =
                IntStream.range(1, LOOPS)
                        .mapToObj(c -> (Callable<Void>) () -> manipulate(c))
                        .collect(Collectors.toList());

        // run, just check there were no exceptions and no assertion failures
        List<Future<Void>> futures = executors.invokeAll(manipulators);
        for (Future<Void> future : futures) {
            future.get();
        }
    }

    /**
     * Performs a read/write manipulation on the DAO contents
     *
     * @param c
     * @return
     */
    protected abstract Void manipulate(int c);
}
