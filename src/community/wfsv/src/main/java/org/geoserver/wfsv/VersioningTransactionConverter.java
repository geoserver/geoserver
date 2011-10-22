/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;

import org.eclipse.emf.common.util.EList;
import org.geoserver.wfs.WFSException;
import org.geotools.data.FeatureDiff;
import org.geotools.data.FeatureDiffReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.Converter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;

/**
 * Converts a {@link FeatureDiffReader} or an array of them into a {@link TransactionType} object
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class VersioningTransactionConverter implements Converter {

    FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

    public <T> T convert(Object source, Class<T> target) throws IOException {
        if (!TransactionType.class.isAssignableFrom(target)) {
            throw new IllegalArgumentException("Target can only be " + TransactionType.class);
        }

        final TransactionType transaction = WfsFactory.eINSTANCE.createTransactionType();
        if (source instanceof FeatureDiffReader) {
            convertReader((FeatureDiffReader) source, transaction);
        } else if (source instanceof FeatureDiffReader[]) {
            for (FeatureDiffReader reader : (FeatureDiffReader[]) source) {
                convertReader(reader, transaction);
            }
        } else {
            throw new IllegalArgumentException("The source can only be a "
                    + "FeatureDiffReader or an array of such readers");
        }

        return (T) transaction;
    }

    void convertReader(FeatureDiffReader diffReader, TransactionType transaction)
            throws IOException {
        // create a single insert element, a single delete element, and as
        // many update elements as needed
        final SimpleFeatureType schema = diffReader.getSchema();
        final QName typeName = new QName(schema.getName().getNamespaceURI(), schema.getTypeName());
        final Set deletedIds = new HashSet();
        final InsertElementType insert = WfsFactory.eINSTANCE.createInsertElementType();

        while (diffReader.hasNext()) {
            FeatureDiff diff = diffReader.next();

            switch (diff.getState()) {
            case FeatureDiff.INSERTED:
                insert.getFeature().add(diff.getFeature());

                break;

            case FeatureDiff.DELETED:
                deletedIds.add(filterFactory.featureId(diff.getID()));

                break;

            case FeatureDiff.UPDATED:

                final UpdateElementType update = WfsFactory.eINSTANCE.createUpdateElementType();
                final EList properties = update.getProperty();

                SimpleFeature f = diff.getFeature();

                for (Iterator it = diff.getChangedAttributes().iterator(); it.hasNext();) {
                    final PropertyType property = WfsFactory.eINSTANCE.createPropertyType();
                    String name = (String) it.next();
                    property.setName(new QName(name));
                    property.setValue(f.getAttribute(name));
                    properties.add(property);
                }

                FeatureId featureId = filterFactory.featureId(diff.getID());
                final Filter filter = filterFactory.id(Collections.singleton(featureId));
                update.setFilter(filter);
                update.setTypeName(typeName);
                transaction.getUpdate().add(update);

                break;

            default:
                throw new WFSException("Could not handle diff type " + diff.getState());
            }
        }
        diffReader.close();

        // create insert and delete elements if needed
        if (insert.getFeature().size() > 0) {
            transaction.getInsert().add(insert);
        }

        if (deletedIds.size() > 0) {
            final DeleteElementType delete = WfsFactory.eINSTANCE.createDeleteElementType();
            delete.setFilter(filterFactory.id(deletedIds));
            delete.setTypeName(typeName);
            transaction.getDelete().add(delete);
        }
    }

}
