package org.geoserver.gss.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geoserver.gss.internal.atom.Atom;
import org.geoserver.gss.internal.atom.CategoryImpl;
import org.geoserver.gss.internal.atom.ContentImpl;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.PersonImpl;
import org.geotools.filter.visitor.AbstractFinderFilterVisitor;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.util.Assert;
import org.xml.sax.helpers.NamespaceSupport;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Applies {@link Filter}, {@code SEARCHTERMS}, {@code STARTPOSITION}, and {@code MAXENTRIES}
 * filtering to an {@code Iterator<EntryImpl>}.
 * 
 * @author groldan
 * @see CommitsEntryListBuilder
 * @see DiffEntryListBuilder
 */
class FilteringEntryListBuilder {

    private Filter filter;

    private final List<String> searchTerms;

    private final Long startPosition;

    private final Long maxEntries;

    public FilteringEntryListBuilder(final Filter filter, final List<String> searchTerms,
            final Long startPosition, final Long maxEntries) {

        this.filter = (Filter) filter.accept(new StripRootEntryPropertyVisitor(), null);

        this.searchTerms = searchTerms;
        this.startPosition = startPosition;
        this.maxEntries = maxEntries;
    }

    public Iterator<EntryImpl> filter(Iterator<EntryImpl> entries) {

        if (!Filter.INCLUDE.equals(filter)) {
            EntryFilter entryFilter = new EntryFilter(filter);
            entries = Iterators.filter(entries, entryFilter);
        }

        if (searchTerms != null && searchTerms.size() > 0) {
            entries = Iterators.filter(entries, new SearchTermsPredicate(searchTerms));
        }

        if (startPosition != null && startPosition.intValue() > 1) {
            final int numberToSkip = startPosition.intValue() - 1;
            final int skipped = Iterators.skip(entries, numberToSkip);
            if (skipped < numberToSkip) {
                entries = Iterators.emptyIterator();
            }
        }

        if (maxEntries != null) {
            entries = Iterators.limit(entries, maxEntries.intValue());
        }
        return entries;
    }

    /**
     * Adapts a {@link Filter} to a {@link Predicate} against an {@link EntryImpl}.
     * 
     * @author groldan
     * 
     */
    private static final class EntryFilter implements Predicate<EntryImpl> {

        private final Filter filter;

        private boolean hasGeometryFilter;

        public EntryFilter(final Filter ogcFilter) {
            this.filter = ogcFilter;
            this.hasGeometryFilter = (Boolean) ogcFilter.accept(new GeometryFilterFinder(), null);
        }

        @Override
        public boolean apply(final EntryImpl input) {
            Filter transformedFilter = this.filter;
            if (hasGeometryFilter) {
                CoordinateReferenceSystem targetCRS = null;
                Object where = input.getWhere();
                if (where instanceof Geometry) {
                    Object userData = ((Geometry) where).getUserData();
                    if (userData instanceof CoordinateReferenceSystem) {
                        targetCRS = (CoordinateReferenceSystem) userData;
                    }
                } else if (where instanceof BoundingBox) {
                    targetCRS = ((BoundingBox) where).getCoordinateReferenceSystem();
                }

                if (targetCRS != null) {
                    transformedFilter = (Filter) this.filter.accept(
                            new GeometryReprojectingFilterDuplicator(targetCRS), null);
                }
            }

            boolean applies = transformedFilter.evaluate(input);
            return applies;
        }

    }

    /**
     * {@link Predicate} to evaluate whether an {@link EntryImpl} contains any of the given search
     * terms, as per the GetEntries SEARCHTERMS parameter.
     * 
     * @author groldan
     * 
     */
    private static class SearchTermsPredicate implements Predicate<EntryImpl> {

        private final List<String> searchTerms;

        public SearchTermsPredicate(final List<String> searchTerms) {
            this.searchTerms = new ArrayList<String>(searchTerms.size());
            for (String s : searchTerms) {
                if (s != null && s.trim().length() > 0) {
                    this.searchTerms.add(s.toUpperCase());
                }
            }
        }

        @Override
        public boolean apply(final EntryImpl e) {
            if (searchTerms.size() == 0) {
                return true;
            }

            boolean applies = applies(e.getTitle()) || applies(e.getSummary())
                    || appliesPerson(e.getAuthor()) || appliesCategory(e.getCategory())
                    || appliesPerson(e.getContributor()) || applies(e.getRights())
                    || appliesContent(e.getContent());

            return applies;
        }

        private boolean appliesContent(final ContentImpl content) {
            if (content == null) {
                return false;
            }
            if (applies(content.getSrc()) || applies(content.getType())) {
                return true;
            }
            return false;
        }

        private boolean appliesCategory(final List<CategoryImpl> categories) {
            if (categories.size() == 0) {
                return searchTerms.size() == 0;
            }
            for (CategoryImpl c : categories) {
                if (applies(c.getTerm()) || applies(c.getScheme())) {
                    return true;
                }
            }
            return false;
        }

        private boolean appliesPerson(final List<PersonImpl> persons) {
            if (persons.size() == 0) {
                return searchTerms.size() == 0;
            }
            for (PersonImpl p : persons) {
                if (applies(p.getName()) || applies(p.getEmail()) || applies(p.getUri())) {
                    return true;
                }
            }
            return false;
        }

        private boolean applies(final String s) {
            if (s == null || s.length() == 0) {
                return searchTerms.size() == 0;
            }
            for (int i = 0; i < searchTerms.size(); i++) {
                if (s.toUpperCase().contains(searchTerms.get(i))) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Strips the leading {@code atom:entry/} property name in a compound property name like
     * {@code atom:entry/georss:where}
     * 
     * @author Andrea Aime - GeoSolutions
     */
    private static class StripRootEntryPropertyVisitor extends DuplicatingFilterVisitor {

        @Override
        public Object visit(PropertyName expression, Object extraData) {
            NamespaceSupport namespaceContext = expression.getNamespaceContext();
            final String prefix = namespaceContext == null ? null : namespaceContext
                    .getPrefix(Atom.NAMESPACE);

            final String rootName = "entry/";
            final String prefixedName = (prefix == null ? "" : (prefix + ":")) + rootName;

            final String propertyName = expression.getPropertyName();

            if (propertyName != null
                    && (propertyName.startsWith(prefixedName) || propertyName.startsWith(rootName))
                    || propertyName.startsWith("atom:entry/")) {
                String target = propertyName.substring(propertyName.indexOf('/') + 1);
                return getFactory(extraData).property(target, namespaceContext);
            } else {
                return super.visit(expression, extraData);
            }
        }

        @Override
        public Object visit(BBOX filter, Object extraData) {
            // rename if necessary
            Expression e1 = filter.getExpression1();
            if (e1 instanceof PropertyName) {
                e1 = (Expression) e1.accept(this, extraData);
            }

            double minx = filter.getMinX();
            double miny = filter.getMinY();
            double maxx = filter.getMaxX();
            double maxy = filter.getMaxY();
            String srs = filter.getSRS();
            return getFactory(extraData).bbox(e1, minx, miny, maxx, maxy, srs);
        }

    }

    /**
     * Finds whether a Filter uses a geometry at all
     * 
     */
    private static class GeometryFilterFinder extends AbstractFinderFilterVisitor {

        @Override
        public Object visit(Literal expression, Object data) {
            Object value = expression.getValue();
            found = value instanceof Geometry || value instanceof Envelope;
            return found;
        }

    }

    private static class GeometryReprojectingFilterDuplicator extends DuplicatingFilterVisitor {

        private final CoordinateReferenceSystem targetCRS;

        public GeometryReprojectingFilterDuplicator(final CoordinateReferenceSystem targetCRS) {
            this.targetCRS = targetCRS;
        }

        @Override
        public Object visit(BBOX filter, Object extraData) {
            Expression e1 = transform(filter.getExpression1());
            Expression e2 = filter.getExpression2();

            BoundingBox bounds = null;

            if (e2 instanceof Literal) {
                Object value = ((Literal) e2).getValue();
                if (value instanceof Geometry) {
                    Geometry geom = (Geometry) value;
                    if (!(geom.getUserData() instanceof CoordinateReferenceSystem)) {
                        CoordinateReferenceSystem crs;
                        try {
                            crs = CRS.decode(filter.getSRS());
                            geom.setUserData(crs);
                        } catch (Exception e) {
                            Throwables.propagate(e);
                        }
                    }
                    e2 = transform(e2);
                    bounds = JTS.toEnvelope((Geometry) ((Literal) e2).getValue());

                } else if (value instanceof BoundingBox) {
                    BoundingBox source = (BoundingBox) value;
                    if (!CRS.equalsIgnoreMetadata(targetCRS, source.getCoordinateReferenceSystem())) {
                        try {
                            bounds = new ReferencedEnvelope(source).transform(targetCRS, true);
                        } catch (Exception e) {
                            Throwables.propagate(e);
                        }
                    }
                }
            }
            if (bounds == null) {
                return super.visit(filter, extraData);
            } else {
                return getFactory(extraData).bbox(e1, bounds);
            }
        }

        @Override
        public Object visit(Beyond filter, Object data) {
            return getFactory(data).beyond(transform(filter.getExpression1()),
                    transform(filter.getExpression2()), filter.getDistance(),
                    filter.getDistanceUnits());
        }

        @Override
        public Object visit(Contains filter, Object data) {
            return getFactory(data).contains(transform(filter.getExpression1()),
                    transform(filter.getExpression2()), filter.getMatchAction());
        }

        @Override
        public Object visit(Crosses filter, Object data) {
            return getFactory(data).crosses(transform(filter.getExpression1()),
                    transform(filter.getExpression2()), filter.getMatchAction());
        }

        @Override
        public Object visit(Disjoint filter, Object data) {
            return getFactory(data).disjoint(transform(filter.getExpression1()),
                    transform(filter.getExpression2()), filter.getMatchAction());
        }

        @Override
        public Object visit(DWithin filter, Object data) {
            return getFactory(data).dwithin(transform(filter.getExpression1()),
                    transform(filter.getExpression2()), filter.getDistance(),
                    filter.getDistanceUnits(), filter.getMatchAction());
        }

        @Override
        public Object visit(Equals filter, Object data) {
            return getFactory(data).equal(transform(filter.getExpression1()),
                    transform(filter.getExpression2()), filter.getMatchAction());
        }

        @Override
        public Object visit(Intersects filter, Object data) {
            return getFactory(data).intersects(transform(filter.getExpression1()),
                    transform(filter.getExpression2()), filter.getMatchAction());
        }

        @Override
        public Object visit(Overlaps filter, Object data) {
            return getFactory(data).overlaps(transform(filter.getExpression1()),
                    transform(filter.getExpression2()), filter.getMatchAction());
        }

        @Override
        public Object visit(Touches filter, Object data) {
            return getFactory(data).touches(transform(filter.getExpression1()),
                    transform(filter.getExpression2()), filter.getMatchAction());
        }

        @Override
        public Object visit(Within filter, Object data) {
            return getFactory(data).within(transform(filter.getExpression1()),
                    transform(filter.getExpression2()), filter.getMatchAction());
        }

        private Expression transform(Expression expression) {
            Expression clone = (Expression) expression.accept(this, null);
            if (clone instanceof Literal) {
                Object value = ((Literal) clone).getValue();
                if (value instanceof Geometry) {
                    value = reproject((Geometry) value, null);
                } else if (value instanceof BoundingBox) {
                    BoundingBox bounds = (BoundingBox) value;
                    Polygon geometry = JTS.toGeometry(bounds);
                    CoordinateReferenceSystem boundsCrs = bounds.getCoordinateReferenceSystem();
                    value = reproject(geometry, boundsCrs);
                }
                return getFactory(null).literal(value);
            }
            return clone;
        }

        /**
         * Helper method to reproject a geometry.
         */
        protected Geometry reproject(Geometry geom, CoordinateReferenceSystem geomCrs) {
            if (geom == null) {
                return null;
            }

            if (geomCrs == null) {
                Object userData = geom.getUserData();
                if (userData instanceof CoordinateReferenceSystem) {
                    geomCrs = (CoordinateReferenceSystem) userData;
                } else if (userData instanceof String) {
                    try {
                        geomCrs = CRS.decode((String) userData);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Can't determine source CRS");
                    }
                }
            }

            Assert.notNull(geomCrs);
            try {
                // reproject
                MathTransform mathTransform = CRS.findMathTransform(geomCrs, targetCRS, true);
                Geometry transformed = JTS.transform(geom, mathTransform);
                transformed.setUserData(this.targetCRS);

                return transformed;
            } catch (Exception e) {
                throw new RuntimeException("Could not reproject geometry " + geom, e);
            }
        }
    }
}
