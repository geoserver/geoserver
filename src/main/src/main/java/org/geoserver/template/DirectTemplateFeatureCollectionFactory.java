/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.template;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;

/**
 * Create FeatureCollection Template Model without copying features to memory When using this in a
 * FeatureWrapper, it is necessary to call purge() method after processing template, to close any
 * open database connections
 *
 * @author Niels Charlier, Curtin University of Technology
 */
public class DirectTemplateFeatureCollectionFactory
        implements FeatureWrapper.TemplateFeatureCollectionFactory<
                DirectTemplateFeatureCollectionFactory.TemplateFeatureCollection> {

    static Logger LOGGER = Logging.getLogger(DirectTemplateFeatureCollectionFactory.class);

    /** thread local to track open iterators */
    static ThreadLocal<List<TemplateFeatureIterator>> ITERATORS =
            new ThreadLocal<List<TemplateFeatureIterator>>();

    public void purge() {
        List<TemplateFeatureIterator> its = ITERATORS.get();
        if (its != null) {
            for (TemplateFeatureIterator it : its) {
                try {
                    it.close();
                } catch (Throwable t) {
                    LOGGER.log(Level.WARNING, "Error closing iterator", t);
                }
            }
            its.clear();
            ITERATORS.remove();
        }
    }

    public DirectTemplateFeatureCollectionFactory() {}

    public TemplateCollectionModel createTemplateFeatureCollection(
            FeatureCollection collection, BeansWrapper wrapper) {
        return new TemplateFeatureCollection(collection, wrapper);
    }

    protected class TemplateFeatureCollection
            implements TemplateCollectionModel, TemplateSequenceModel {
        protected BeansWrapper wrapper;

        protected FeatureCollection collection;

        protected TemplateFeatureIterator indexIterator = null;

        protected int currentIndex = -1;

        protected TemplateModel currentItem = null;

        public TemplateFeatureCollection(FeatureCollection collection, BeansWrapper wrapper) {
            this.collection = collection;
            this.wrapper = wrapper;
        }

        public TemplateModelIterator iterator() throws TemplateModelException {
            TemplateFeatureIterator it =
                    new TemplateFeatureIterator(collection.features(), wrapper);
            List<TemplateFeatureIterator> open = ITERATORS.get();
            if (open == null) {
                open = new LinkedList();
                ITERATORS.set(open);
            }
            open.add(it);
            return it;
        }

        @Override
        public TemplateModel get(int index) throws TemplateModelException {
            if (currentIndex > index) {
                // we have gone backwards, close iterator and clean up as we will need to start over
                if (indexIterator != null) {
                    ITERATORS.get().remove(indexIterator);
                    try {
                        indexIterator.close();
                    } catch (Throwable t) {
                        LOGGER.log(Level.WARNING, "Error closing iterator", t);
                    }
                    indexIterator = null;
                }
                currentIndex = -1;
                currentItem = null;
            }
            if (indexIterator == null) {
                indexIterator = (TemplateFeatureIterator) iterator();
            }
            while (currentIndex < index && indexIterator.hasNext()) {
                // forward to correct index
                currentItem = indexIterator.next();
                currentIndex++;
            }
            return index == currentIndex ? currentItem : null;
        }

        @Override
        public int size() throws TemplateModelException {
            return collection.size();
        }
    }

    protected class TemplateFeatureIterator implements TemplateModelIterator {

        protected BeansWrapper wrapper;

        protected FeatureIterator iterator;

        public TemplateFeatureIterator(FeatureIterator iterator, BeansWrapper wrapper) {
            this.iterator = iterator;
            this.wrapper = wrapper;
        }

        public TemplateModel next() throws TemplateModelException {
            return wrapper.wrap(iterator.next());
        }

        public boolean hasNext() throws TemplateModelException {
            return iterator.hasNext();
        }

        public void close() {
            iterator.close();
        }
    }
}
