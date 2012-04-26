package org.geoserver.data.geogit;

import java.util.Date;
import java.util.logging.Logger;

import org.geogit.api.CommitStateResolver;
import org.geotools.data.Transaction;
import org.geotools.feature.type.DateUtil;
import org.geotools.util.logging.Logging;

public class GeoToolsCommitStateResolver implements CommitStateResolver {

    private static final Logger LOGGER = Logging.getLogger(GeoToolsCommitStateResolver.class);

    public static final String GEOGIT_AUTHOR = "GEOGIT_AUTHOR";

    public static final String GEOGIT_COMMITTER = "GEOGIT_COMMITTER";

    public static final String GEOGIT_COMMIT_MESSAGE = "GEOGIT_COMMIT_MESSAGE";

    public static final String GEOGIT_COMMIT_TIMESTAMP = "GEOGIT_COMMIT_TIMESTAMP";

    static final ThreadLocal<Transaction> CURRENT_TRANSACTION = new ThreadLocal<Transaction>();

    private static Transaction getTransaction() {
        Transaction transaction = CURRENT_TRANSACTION.get();
        return transaction;
    }

    @Override
    public String getAuthor() {
        String user = null;
        Transaction transaction = getTransaction();
        if (transaction != null) {
            user = (String) transaction.getProperty(GEOGIT_AUTHOR);
            if (user == null) {
                user = (String) transaction.getProperty("VersioningCommitAuthor");
            }
        }
        if (user == null) {
            user = "anonymous";
        }
        return user;
    }

    @Override
    public String getCommitter() {
        String committer = null;
        Transaction transaction = getTransaction();
        if (transaction != null) {
            committer = (String) transaction.getProperty(GEOGIT_COMMITTER);
            if (committer == null) {
                committer = getAuthor();
            }
        }
        return committer;
    }

    @Override
    public long getCommitTimeMillis() {
        long timestamp = System.currentTimeMillis();
        Transaction transaction = getTransaction();
        if (transaction != null) {
            Object providedTimestamp = transaction.getProperty(GEOGIT_COMMIT_TIMESTAMP);
            if (providedTimestamp instanceof Date) {
                timestamp = ((Date) providedTimestamp).getTime();
            } else if (providedTimestamp instanceof Number) {
                timestamp = ((Number) providedTimestamp).longValue();
            } else if (providedTimestamp != null) {
                String str = String.valueOf(providedTimestamp);
                try {
                    timestamp = DateUtil.parseDateTime(str);
                } catch (Exception e) {
                    try {
                        timestamp = Long.parseLong(str);
                    } catch (Exception e2) {
                        LOGGER.warning("Can't parse commit timestamp out of Transaction's "
                                + GEOGIT_COMMIT_TIMESTAMP + " hint: '" + str
                                + "'. Using System's current time");
                    }
                }
            }
        }
        return timestamp;
    }

    @Override
    public String getCommitMessage() {
        String message = null;
        Transaction transaction = getTransaction();
        if (transaction != null) {
            message = (String) transaction.getProperty(GEOGIT_COMMIT_MESSAGE);
            if (message == null) {
                message = (String) transaction.getProperty("VersioningCommitMessage");
            }
        }
        if (message == null) {
            message = "No commit message provided";
        }
        return message;
    }

}
