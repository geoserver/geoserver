package org.geoserver.data.gss;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.repository.Repository;
import org.geoserver.data.geogit.GeoGitDataStore;
import org.opengis.feature.type.Name;

import com.google.common.base.Preconditions;

public class GeoSyncDataStore extends GeoGitDataStore {

    private final ServerSubscription subscriptionOpts;

    public GeoSyncDataStore(final ServerSubscription subscriptionOpts, final Repository repo)
            throws IOException {
        super(repo, subscriptionOpts.getReplicatedNamespace());
        Preconditions.checkNotNull(subscriptionOpts);

        this.subscriptionOpts = subscriptionOpts;
    }

    public ServerSubscription getSubscriptionOpts() {
        return subscriptionOpts;
    }

    @Override
    public List<Name> getNames() throws IOException {
        final String namespace = subscriptionOpts.getReplicatedNamespace();
        List<Name> names = new LinkedList<Name>(super.getNames());
        for (Iterator<Name> it = names.iterator(); it.hasNext();) {
            Name name = it.next();
            if (!namespace.equals(name.getNamespaceURI())) {
                it.remove();
            }
        }
        return names;
    }
}
