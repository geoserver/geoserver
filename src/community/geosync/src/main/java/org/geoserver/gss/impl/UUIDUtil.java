package org.geoserver.gss.impl;

import org.geogit.api.DiffEntry;
import org.geogit.api.DiffEntry.ChangeType;
import org.geogit.api.ObjectId;

public class UUIDUtil {

    /**
     * Builds a UUID out of a diff entry based on the entry's old and new commit ids and the id of
     * the blob it references.
     * 
     * @param diffEnty
     * @return
     */
    public static String buildReplicationEntryUUID(DiffEntry diffEnty) {
        // commit is always the diffEntry newCommitId, as newCommitId doesn't mean it's temporaly
        // newest, just at the "new" end of the diff, and may be the older or the newer commit
        // depending on which direction the diff was performed
        final ObjectId oldCommitId = diffEnty.getOldCommitId();
        final ObjectId newCommitId = diffEnty.getNewCommitId();
        final ObjectId objectId = diffEnty.getType() == ChangeType.DELETE ? diffEnty
                .getOldObjectId() : diffEnty.getNewObjectId();

        // O = old commit, N = new commit, B = blob
        // "OOOOOOOO-NNNN-NNNN-BBBB-BBBBBBBBBBBB"
        final String oldCommit = oldCommitId.toString().substring(0, 8);
        final String newCommit = newCommitId.toString().substring(0, 8);
        final String object = objectId.toString().substring(0, 12);

        StringBuilder uuid = new StringBuilder();
        uuid.append(oldCommit);
        uuid.append('-');
        uuid.append(newCommit.substring(0, 4));
        uuid.append('-');
        uuid.append(newCommit.substring(4));
        uuid.append('-');
        uuid.append(object.substring(0, 4));
        uuid.append('-');
        uuid.append(object.substring(4));

        return uuid.toString();
    }

    public static String getOldCommitHash(final String uuid) {
        String oldCommitHash = uuid.substring(0, 8);
        return oldCommitHash;
    }

    public static String getNewCommitHash(final String uuid) {
        StringBuilder sb = new StringBuilder();
        sb.append(uuid.substring(9, 13));
        sb.append(uuid.substring(14, 18));
        return sb.toString();
    }

    public static String getBlobHash(final String uuid) {
        StringBuilder sb = new StringBuilder();
        sb.append(uuid.substring(19, 23));
        sb.append(uuid.substring(24));
        return sb.toString();
    }
}
