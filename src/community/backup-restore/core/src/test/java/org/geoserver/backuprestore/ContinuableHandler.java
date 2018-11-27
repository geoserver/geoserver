/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import org.geoserver.backuprestore.tasklet.GenericTaskletHandler;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/** Test generic handler that will run 3 times by returning continuable. */
public final class ContinuableHandler implements GenericTaskletHandler, Serializable {

    private static final long serialVersionUID = -1237780850044092748L;

    private static AtomicInteger INVOCATIONS = new AtomicInteger(0);

    @Override
    public void initialize(StepExecution stepExecution, BackupRestoreItem context) {
        // nothing to do here
    }

    @Override
    public RepeatStatus handle(
            StepContribution contribution,
            ChunkContext chunkContext,
            JobExecution jobExecution,
            BackupRestoreItem context) {
        int invocations = INVOCATIONS.incrementAndGet();
        if (invocations > 2) {
            // we are done
            return RepeatStatus.FINISHED;
        }
        // we need to run again
        return RepeatStatus.CONTINUABLE;
    }

    /** Reset the number of invocation of this handler counter. */
    public static void resetInvocationsCount() {
        INVOCATIONS.set(0);
    }

    /** Return the number of time this handler has been invoked. */
    public static int getInvocationsCount() {
        return INVOCATIONS.get();
    }
}
