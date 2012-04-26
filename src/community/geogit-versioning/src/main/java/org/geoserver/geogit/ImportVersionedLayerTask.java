package org.geoserver.geogit;

import java.util.logging.Logger;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.GeoGIT;
import org.geogit.api.RevCommit;
import org.geogit.repository.WorkingTree;
import org.geoserver.task.LongTask;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.SubProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

class ImportVersionedLayerTask extends LongTask<RevCommit> {

    private static final Logger LOGGER = Logging.getLogger(ImportVersionedLayerTask.class);

    final GeoGIT geoGit;

    @SuppressWarnings("rawtypes")
    private final FeatureSource featureSource;

    private final Name featureTypeName;

    @SuppressWarnings("rawtypes")
    public ImportVersionedLayerTask(final FeatureSource featureSource, final GeoGIT geoGit) {
        super();
        this.featureSource = featureSource;
        this.geoGit = geoGit;

        this.featureTypeName = featureSource.getName();

        final String title = "Import " + featureTypeName.getLocalPart() + " as Versioned";
        final String description = "GeoGit is creating the initial import of this FeatureType";
        setTitle(title);
        setDescription(description);
        setProgressMessage("Waiting for operation to start");
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected RevCommit callInternal(final ProgressListener listener) throws Exception {

        listener.started();
        final float insertProgressAmount = 33f;
        final float addProgressAmount = 33f;
        final float commitProgressAmount = 34f;

        final FeatureType schema = featureSource.getSchema();
        final FeatureCollection features = featureSource.getFeatures();

        final String commitMessage = "Import of FeatureType " + featureTypeName.getLocalPart()
                + ".\nThis is the initial import of FeatureType " + featureTypeName.getLocalPart()
                + " as a versioned Layer in GeoServer";

        // geoGit.checkout();TODO: check out master branch
        setProgressMessage("Copying features to working tree");
        WorkingTree workingTree = geoGit.getRepository().getWorkingTree();
        workingTree.init(schema);
        try {
            final boolean forceUseProvidedFIDs = true;
            long t = System.currentTimeMillis();
            workingTree.insert(features, forceUseProvidedFIDs, new SubProgressListener(listener,
                    insertProgressAmount));
            if (listener.isCanceled()) {
                return null;
            }
            t=System.currentTimeMillis() - t;
            LOGGER.warning("WorkingTree.insert: " + t + "ms");
        } catch (Exception e) {
            setProgressMessage("Exception ocuurred, cleaning up working tree...");
            workingTree.delete(featureTypeName);
            setProgressMessage("Error: " + e.getMessage());
            throw e;
        }

        final RevCommit revCommit;
        if (listener.isCanceled()) {
            String msg = "Import process for " + featureTypeName.getLocalPart() + " cancelled.";
            LOGGER.warning(msg);
            setProgressMessage(msg);
            revCommit = null;
        } else {
            // add only the features of this type, other imports may be running in parallel and we
            // don't want to commit them all
            // geoGit.add().addPattern(pattern).call();
            setProgressMessage("Staging changes...");
            long t= System.currentTimeMillis();
            geoGit.add().setProgressListener(new SubProgressListener(listener, addProgressAmount))
                    .call();
            if (listener.isCanceled()) {
                return null;
            }
            t = System.currentTimeMillis() - t;
            LOGGER.warning("add operation: " + t + "ms");

            setProgressMessage("Committing....");
            AbstractGeoGitOp<RevCommit> commitOp = geoGit.commit().setMessage(commitMessage)
                    .setProgressListener(new SubProgressListener(listener, commitProgressAmount));
            
            t = System.currentTimeMillis();
            revCommit = commitOp.call();
            t = System.currentTimeMillis() - t;
            LOGGER.warning("commit: " + t + "ms");

            if (!listener.isCanceled()) {
                setProgressMessage("Committed.");
                listener.complete();
                LOGGER.info("Initial commit of " + featureTypeName + ": " + revCommit.getId());
            }
        }
        return revCommit;
    }

}
