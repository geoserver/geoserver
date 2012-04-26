package org.geoserver.data.geogit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.geogit.api.CommitOp;
import org.geogit.api.CommitStateResolver;
import org.geogit.api.GeoGIT;
import org.geogit.api.NothingToCommitException;
import org.geogit.api.RevCommit;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;
import org.opengis.util.ProgressListener;

@SuppressWarnings("rawtypes")
public class VersioningTransactionState implements Transaction.State {

    public static final VersioningTransactionState VOID = new VersioningTransactionState(null) {

        @Override
        public List<FeatureId> stageInsert(final Name typeName, FeatureCollection affectedFeatures)
                throws Exception {
            return Collections.emptyList();
        }

        @Override
        public void stageUpdate(final Name typeName, final FeatureCollection affectedFeatures)
                throws Exception {
        }

        @Override
        public void stageDelete(Name typeName, Filter filter, FeatureCollection affectedFeatures)
                throws Exception {
        }

        @Override
        public void stageRename(final Name typeName, final String oldFid, final String newFid) {
        }

    };

    static {
        // only set resolver if not overriden by application
        CommitStateResolver current = GeoGIT.getCommitStateResolver();
        if (GeoGIT.DEFAULT_COMMIT_RESOLVER.equals(current)) {
            GeoGIT.setCommitStateResolver(new GeoToolsCommitStateResolver());
        }
    }

    private static final ProgressListener NULL_PROGRESS_LISTENER = new NullProgressListener();

    private static final Logger LOGGER = Logging.getLogger(VersioningTransactionState.class);

    private Transaction transaction;

    private GeoGIT geoGit;

    private String id;

    private Set<Name> changedTypes;

    public VersioningTransactionState(final GeoGIT geoGit) {
        this.geoGit = geoGit;
        this.id = UUID.randomUUID().toString();
        this.changedTypes = Collections.synchronizedSet(new HashSet<Name>());
    }

    @Override
    public void setTransaction(final Transaction transaction) {
        if (transaction != null) {
            // configure
            this.transaction = transaction;
        } else {
            this.transaction = null;
            // TODO: is there some cleanup to do here?
        }
    }

    @Override
    public void addAuthorization(String AuthID) throws IOException {
        // TODO Auto-generated method stub
    }

    @Override
    public void commit() throws IOException {
        LOGGER.info("Committing changeset " + id);
        try {
            GeoToolsCommitStateResolver.CURRENT_TRANSACTION.set(transaction);
            // final Ref branch = geoGit.checkout().setName(transactionID).call();
            // commit to the branch
            RevCommit commit = null;
            // checkout master
            // final Ref master = geoGit.checkout().setName("master").call();
            // merge branch to master
            // MergeResult mergeResult = geoGit.merge().include(branch).call();
            // TODO: check mergeResult is success?
            // geoGit.branchDelete().setName(transactionID).call();
            try {
                // geoGit.add().call();
                CommitOp commitOp = geoGit.commit();
                commit = commitOp.call();
                LOGGER.info("New commit: " + commit);
            } catch (NothingToCommitException emptyCommit) {
                LOGGER.warning("GeoGit threw NothingToCommitException, this may be due to the transaction having not changed the repository contents");
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        } finally {
            GeoToolsCommitStateResolver.CURRENT_TRANSACTION.remove();
        }
    }

    @Override
    public void rollback() throws IOException {
        geoGit.getRepository().getIndex().reset();
    }

    /**
     * @param transactionID
     * @param typeName
     * @param affectedFeatures
     * @return the list of feature ids of the inserted features, in the order they were added
     * @throws Exception
     */
    public List<FeatureId> stageInsert(final Name typeName, FeatureCollection affectedFeatures)
            throws Exception {
        return stageInsert(typeName, affectedFeatures, false);
    }

    public List<FeatureId> stageInsert(final Name typeName, FeatureCollection affectedFeatures,
            final boolean forceUseProvidedFIDs) throws Exception {

        changedTypes.add(typeName);

        WorkingTree workingTree = geoGit.getRepository().getWorkingTree();
        List<FeatureId> inserted = workingTree.insert(affectedFeatures, forceUseProvidedFIDs,
                NULL_PROGRESS_LISTENER);
        geoGit.add().call();
        return inserted;
    }

    public void stageUpdate(final Name typeName, final FeatureCollection newValues)
            throws Exception {

        changedTypes.add(typeName);

        WorkingTree workingTree = geoGit.getRepository().getWorkingTree();
        workingTree.update(newValues, NULL_PROGRESS_LISTENER);
        geoGit.add().call();
    }

    public void stageDelete(final Name typeName, final Filter filter,
            final FeatureCollection affectedFeatures) throws Exception {

        changedTypes.add(typeName);

        WorkingTree workingTree = geoGit.getRepository().getWorkingTree();
        workingTree.delete(typeName, filter, affectedFeatures);

        geoGit.add().call();

    }

    public void stageRename(final Name typeName, final String oldFid, final String newFid) {

        StagingArea index = geoGit.getRepository().getIndex();

        final String namespaceURI = typeName.getNamespaceURI();
        final String localPart = typeName.getLocalPart();

        List<String> from = Arrays.asList(namespaceURI, localPart, oldFid);
        List<String> to = Arrays.asList(namespaceURI, localPart, newFid);

        index.renamed(from, to);
    }

    private List<String> path(final Name typeName) {
        List<String> path = new ArrayList<String>(2);
        if (typeName.getNamespaceURI() != null) {
            path.add(typeName.getNamespaceURI());
        }
        path.add(typeName.getLocalPart());
        return path;
    }
}
