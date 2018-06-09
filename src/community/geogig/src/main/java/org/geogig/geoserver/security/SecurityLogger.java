/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.security;

import static java.lang.String.format;
import static org.locationtech.geogig.remotes.RefDiff.Type.ADDED_REF;
import static org.locationtech.geogig.remotes.RefDiff.Type.CHANGED_REF;
import static org.locationtech.geogig.remotes.RefDiff.Type.REMOVED_REF;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.geogig.geoserver.config.LogStore;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.remotes.CloneOp;
import org.locationtech.geogig.remotes.FetchOp;
import org.locationtech.geogig.remotes.PullOp;
import org.locationtech.geogig.remotes.PullResult;
import org.locationtech.geogig.remotes.PushOp;
import org.locationtech.geogig.remotes.RefDiff;
import org.locationtech.geogig.remotes.RemoteAddOp;
import org.locationtech.geogig.remotes.RemoteRemoveOp;
import org.locationtech.geogig.remotes.TransferSummary;
import org.locationtech.geogig.repository.AbstractGeoGigOp;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Repository;
import org.springframework.beans.factory.InitializingBean;

public class SecurityLogger implements InitializingBean {

    private static final Map<Class<? extends AbstractGeoGigOp<?>>, MessageBuilder<?>>
            WATCHED_COMMANDS;

    static {
        Builder<Class<? extends AbstractGeoGigOp<?>>, MessageBuilder<?>> builder =
                ImmutableMap.builder();

        builder.put(RemoteAddOp.class, new RemoteAddMessageBuilder());
        builder.put(RemoteRemoveOp.class, new RemoteRemoveMessageBuilder());
        builder.put(PullOp.class, new PullOpMessageBuilder());
        builder.put(PushOp.class, new PushOpMessageBuilder());
        builder.put(FetchOp.class, new FetchOpMessageBuilder());
        builder.put(CloneOp.class, new CloneOpMessageBuilder());

        WATCHED_COMMANDS = builder.build();
    }

    private LogStore logStore;

    private static SecurityLogger INSTANCE;

    public SecurityLogger(LogStore logStore) {
        this.logStore = logStore;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    public static boolean interestedIn(Class<? extends AbstractGeoGigOp<?>> clazz) {
        return WATCHED_COMMANDS.containsKey(clazz);
    }

    public static void logPre(AbstractGeoGigOp<?> command) {
        if (INSTANCE == null) {
            return; // not yet initialized
        }
        INSTANCE.pre(command);
    }

    public static void logPost(
            AbstractGeoGigOp<?> command,
            @Nullable Object retVal,
            @Nullable RuntimeException exception) {

        if (INSTANCE == null) {
            return; // not yet initialized
        }
        if (exception == null) {
            INSTANCE.post(command, retVal);
        } else {
            INSTANCE.error(command, exception);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void error(AbstractGeoGigOp<?> command, RuntimeException exception) {
        MessageBuilder builder = builderFor(command);
        String repoUrl = repoUrl(command);
        logStore.error(repoUrl, builder.buildError(command, exception), exception);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void post(AbstractGeoGigOp<?> command, Object commandResult) {
        MessageBuilder builder = builderFor(command);
        String repoUrl = repoUrl(command);
        logStore.info(repoUrl, builder.buildPost(command, commandResult));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void pre(AbstractGeoGigOp<?> command) {
        MessageBuilder builder = builderFor(command);
        String repoUrl = repoUrl(command);
        logStore.debug(repoUrl, builder.buildPre(command));
    }

    private MessageBuilder<?> builderFor(AbstractGeoGigOp<?> command) {
        MessageBuilder<?> builder = WATCHED_COMMANDS.get(command.getClass());
        Preconditions.checkNotNull(builder);
        return builder;
    }

    @Nullable
    private String repoUrl(AbstractGeoGigOp<?> command) {
        Context context = command.context();
        if (context == null) {
            return null;
        }
        Repository repository = context.repository();
        if (repository == null) {
            return null;
        }
        URI location = repository.getLocation();
        if (location == null) {
            return null;
        }
        String uri = location.toString();
        if ("file".equals(location.getScheme())) {
            try {
                File f = new File(location);
                if (f.getName().equals(".geogig")) {
                    f = f.getParentFile();
                    uri = f.toURI().toString();
                }
            } catch (Exception e) {
                uri = location.toString();
            }
        }
        return uri;
    }

    private abstract static class MessageBuilder<T extends AbstractGeoGigOp<?>> {

        CharSequence buildPost(T command, Object commandResult) {
            return format("%s success. Parameters: %s", friendlyName(), params(command));
        }

        CharSequence buildPre(T c) {
            return format("%s: Parameters: %s", friendlyName(), params(c));
        }

        CharSequence buildError(T command, RuntimeException exception) {
            return format(
                    "%s failed. Parameters: %s. Error message: %s",
                    friendlyName(), params(command), exception.getMessage());
        }

        abstract String friendlyName();

        abstract String params(T command);
    }

    private static class RemoteAddMessageBuilder extends MessageBuilder<RemoteAddOp> {
        @Override
        String friendlyName() {
            return "Remote add";
        }

        @Override
        String params(RemoteAddOp c) {
            return format("name='%s', url='%s'", c.getName(), c.getURL());
        }
    }

    private static class RemoteRemoveMessageBuilder extends MessageBuilder<RemoteRemoveOp> {
        @Override
        String friendlyName() {
            return "Remote remove";
        }

        @Override
        String params(RemoteRemoveOp c) {
            return format("name='%s'", c.getName());
        }
    }

    private static class PullOpMessageBuilder extends MessageBuilder<PullOp> {
        @Override
        String friendlyName() {
            return "Pull";
        }

        @Override
        CharSequence buildPost(PullOp command, Object commandResult) {
            PullResult pr = (PullResult) commandResult;
            TransferSummary fr = pr.getFetchResult();
            StringBuilder sb = formatFetchResult(fr);
            return format(
                    "%s success. Parameters: %s. Changes: %s", friendlyName(), params(command), sb);
        }

        @Override
        String params(PullOp c) {
            return format(
                    "remote=%s, refSpecs=%s, depth=%s, author=%s, author email=%s",
                    c.getRemoteName(),
                    c.getRefSpecs(),
                    c.getDepth(),
                    c.getAuthor(),
                    c.getAuthorEmail());
        }
    }

    private static class PushOpMessageBuilder extends MessageBuilder<PushOp> {
        @Override
        String friendlyName() {
            return "Push";
        }

        @Override
        String params(PushOp c) {
            return format("remote=%s, refSpecs=%s", c.getRemoteName(), c.getRefSpecs());
        }
    }

    private static class FetchOpMessageBuilder extends MessageBuilder<FetchOp> {
        @Override
        String friendlyName() {
            return "Fetch";
        }

        @Override
        CharSequence buildPost(FetchOp command, Object commandResult) {
            TransferSummary fr = (TransferSummary) commandResult;
            StringBuilder sb = formatFetchResult(fr);
            return format(
                    "%s success. Parameters: %s. Changes: %s", friendlyName(), params(command), sb);
        }

        @Override
        String params(FetchOp c) {
            return format(
                    "remotes=%s, all=%s, full depth=%s, depth=%s, prune=%s",
                    c.getRemoteNames(), c.isAll(), c.isFullDepth(), c.getDepth(), c.isPrune());
        }
    }

    private static class CloneOpMessageBuilder extends MessageBuilder<CloneOp> {
        @Override
        String friendlyName() {
            return "Clone";
        }

        @Override
        String params(CloneOp c) {
            return format(
                    "url=%s, branch=%s, depth=%s",
                    c.getRemoteURI(), c.getBranch().orNull(), c.getDepth().orNull());
        }
    }

    private static final StringBuilder formatFetchResult(TransferSummary fr) {
        Map<String, Collection<RefDiff>> refs = fr.getRefDiffs();

        StringBuilder sb = new StringBuilder();
        if (refs.isEmpty()) {
            sb.append("already up to date");
        } else {
            for (Iterator<Entry<String, Collection<RefDiff>>> it = refs.entrySet().iterator();
                    it.hasNext(); ) {
                Entry<String, Collection<RefDiff>> entry = it.next();
                String remoteUrl = entry.getKey();
                Collection<RefDiff> changedRefs = entry.getValue();
                sb.append(" From ").append(remoteUrl).append(": [");
                print(changedRefs, sb);
                sb.append("]");
            }
        }
        return sb;
    }

    private static String toString(ObjectId objectId) {
        return objectId.toString().substring(0, 8);
    }

    private static void print(Collection<RefDiff> changedRefs, StringBuilder sb) {
        for (Iterator<RefDiff> it = changedRefs.iterator(); it.hasNext(); ) {
            RefDiff ref = it.next();
            Ref oldRef = ref.getOldRef();
            Ref newRef = ref.getNewRef();
            if (ref.getType() == CHANGED_REF) {
                sb.append(oldRef.getName()).append(" ");
                sb.append(toString(oldRef.getObjectId()));
                sb.append(" -> ");
                sb.append(toString(newRef.getObjectId()));
            } else if (ref.getType() == ADDED_REF) {
                String reftype = (newRef.getName().startsWith(Ref.TAGS_PREFIX)) ? "tag" : "branch";
                sb.append("* [new ")
                        .append(reftype)
                        .append("] ")
                        .append(newRef.getName())
                        .append(" -> ")
                        .append(toString(newRef.getObjectId()));
            } else if (ref.getType() == REMOVED_REF) {
                sb.append("x [deleted] ").append(oldRef.getName());
            } else {
                sb.append("[deepened]" + newRef.getName());
            }
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
    }
}
