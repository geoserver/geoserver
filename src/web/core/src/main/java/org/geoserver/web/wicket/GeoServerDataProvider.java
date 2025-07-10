/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.stream.Streams;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Predicates;
import org.geoserver.web.GeoServerApplication;
import org.geotools.api.filter.Filter;
import org.geotools.util.logging.Logging;

/**
 * GeoServer specific data provider. In addition to the services provided by a SortableDataProvider it can perform
 * keyword based filtering, enum the model properties used for display and sorting.
 *
 * <p>Implementors of providers for editable tables need to remember to raise the {@link #editable} flag.
 *
 * @param <T>
 */
public abstract class GeoServerDataProvider<T> extends SortableDataProvider<T, Object> {

    /**
     * Matches a search keyword term enclosed by either double or single quotes.
     *
     * <p>The first matching group captures either a double quote (<code>"</code>) or a single quote (<code>'</code>).
     * The "keyword" named capturing group matches any sequence of one or more characters. The backreference (<code>\\1
     * </code>) ensures that the start and end quotes are the same, maintaining balance.
     */
    private static final Pattern EXACT_TERM_KEYWORD_PATTERN = Pattern.compile("^([\"'])(?<keyword>.+)\\1$");

    private static final long serialVersionUID = -6876929036365601443L;

    static final Logger LOGGER = Logging.getLogger(GeoServerDataProvider.class);

    /** Keywords used for filtering data */
    protected String[] keywords;

    /** regular expression matchers, one per keyword */
    private transient Matcher[] matchers;

    /**
     * A cache used to avoid recreating models over and over, this make it possible to make {@link GeoServerTablePanel}
     * editable
     */
    IdentityHashMap<T, IModel<T>> modelCache = new IdentityHashMap<>();

    /** Sets the data provider as editable, in that case the models should be preserved */
    boolean editable = false;

    /** Returns true if this data provider is setup for editing (it will reuse models). Defaults to false */
    public boolean isEditable() {
        return editable;
    }

    /** Sets the data provider as editable/non editable */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /** Returns the current filtering keywords */
    public String[] getKeywords() {
        return keywords;
    }

    /** Sets the keywords used for filtering */
    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
        this.matchers = null;
    }

    /**
     * This method returns an array of regex matchers based on the defined {@link #keywords}, if any. If no keywords are
     * defined, an empty array is returned.
     *
     * <p>If a keyword is enclosed in quotes or double-quotes, the regex for that specific search keyword is created for
     * an exact match (word-bound).
     *
     * @return an array containing a regex matcher for each search keyword. If no keywords are present, an empty array
     *     is returned.
     */
    protected Matcher[] getMatchers() {
        if (matchers != null) {
            return matchers;
        }

        if (keywords == null) {
            return new Matcher[0];
        }

        // build the case-insensitive regex patterns
        matchers = new Matcher[keywords.length];

        String keyword;
        String regex;
        Pattern pattern;
        for (int i = 0; i < keywords.length; i++) {
            keyword = keywords[i];
            regex = composeRegex(keyword);
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            matchers[i] = pattern.matcher("");
        }

        return matchers;
    }

    private String composeRegex(String keyword) {
        Matcher exactTermKeywordMatcher = EXACT_TERM_KEYWORD_PATTERN.matcher(keyword);
        if (exactTermKeywordMatcher.matches()) {
            return ".*\\b" + escape(exactTermKeywordMatcher.group("keyword")) + "\\b.*";
        } else {
            return ".*" + escape(keyword) + ".*";
        }
    }

    /** Escape any character that's special for the regex api */
    private String escape(String keyword) {
        final String escapeSeq = "\\";
        final int len = keyword.length();
        StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0; i < len; i++) {
            c = keyword.charAt(i);
            if (isSpecial(c)) {
                sb.append(escapeSeq);
            }
            sb.append(keyword.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Convenience method to determine if a character is special to the regex system.
     *
     * @param chr the character to test
     * @return is the character a special character.
     */
    private boolean isSpecial(final char chr) {
        return ((chr == '.')
                || (chr == '?')
                || (chr == '*')
                || (chr == '^')
                || (chr == '$')
                || (chr == '+')
                || (chr == '[')
                || (chr == ']')
                || (chr == '(')
                || (chr == ')')
                || (chr == '|')
                || (chr == '\\')
                || (chr == '&'));
    }

    /** Returns the application singleton. */
    protected GeoServerApplication getApplication() {
        return GeoServerApplication.get();
    }

    /** Provides catalog access for the provider (cannot be stored as a field, this class is going to be serialized) */
    protected Catalog getCatalog() {
        return getApplication().getCatalog();
    }

    /** @see org.apache.wicket.markup.repeater.data.IDataProvider#iterator(int, int) */
    @Override
    public Iterator<T> iterator(long first, long count) {
        List<T> items = getFilteredItems();

        // global sorting
        Comparator<T> comparator = getComparator(getSort());
        if (comparator != null) {
            Collections.sort(items, comparator);
        }
        if (items.size() <= count) {
            // the list has been paged for us.
            return items.iterator();
        }
        // in memory paging
        long last = first + count;
        if (last > items.size()) last = items.size();
        return items.subList((int) first, (int) last).iterator();
    }

    /**
     * Returns a filtered list of items. Subclasses can override if they have a more efficient way of filtering than in
     * memory keyword comparison
     */
    protected List<T> getFilteredItems() {
        List<T> items = getItems();

        // if needed, filter
        if (keywords != null && keywords.length > 0) {
            return filterByKeywords(items);
        } else {
            // make a deep copy anyways, the catalog does not do that for us
            return new ArrayList<>(items);
        }
    }

    /**
     * Returns the size of the filtered item collection
     *
     * @see org.apache.wicket.markup.repeater.data.IDataProvider#size()
     */
    @Override
    public long size() {
        return getFilteredItems().size();
    }

    /** Returns the global size of the collection, without filtering it */
    public int fullSize() {
        return getItems().size();
    }

    private List<T> filterByKeywords(List<T> items) {
        List<T> result = new ArrayList<>();

        final Matcher[] matchers = getMatchers();

        List<Property<T>> properties = getProperties();
        for (T item : items) {
            ITEM:
            // find any match of any pattern over any property
            for (Property<T> property : properties) {
                if (property.isSearchable()) {
                    Object value = property.getPropertyValue(item);
                    if (value != null) {
                        // brute force check for keywords
                        for (Matcher matcher : matchers) {
                            matcher.reset(String.valueOf(value));
                            if (matcher.matches()) {
                                result.add(item);
                                break ITEM;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /** Returns only the properties that have been marked as visible */
    List<Property<T>> getVisibleProperties() {
        List<Property<T>> results = new ArrayList<>();
        for (Property<T> p : getProperties()) {
            if (p.isVisible()) results.add(p);
        }
        return results;
    }

    /**
     * Returns the list of properties served by this provider. The property keys are used to establish the layer
     * sorting, whilst the Property itself is used to extract the value of the property from the item.
     */
    protected abstract List<Property<T>> getProperties();

    /** Returns a non filtered list of all the items the provider must return */
    protected abstract List<T> getItems();

    /** Returns a comparator given the sort property. */
    protected Comparator<T> getComparator(SortParam<?> sort) {
        if (sort == null) {
            return null;
        }

        Property<T> property = getProperty(sort);
        if (property != null) {
            Comparator<T> comparator = property.getComparator();
            if (comparator != null) {
                if (!sort.isAscending()) return new ReverseComparator<>(comparator);
                else return comparator;
            }
        }
        LOGGER.log(Level.WARNING, "Could not find any comparator " + "for sort property " + sort);
        return null;
    }

    protected Property<T> getProperty(SortParam<?> sort) {
        if (sort == null || sort.getProperty() == null) return null;

        for (Property<T> property : getProperties()) {
            if (sort.getProperty().equals(property.getName())) {
                return property;
            }
        }
        return null;
    }

    /**
     * This implementation uses the {@link #modelCache} to avoid recreating over and over different models for the
     * various items, this allows the grid panel to be editable
     *
     * @see org.apache.wicket.markup.repeater.data.IDataProvider#model(java.lang.Object)
     */
    @Override
    public final IModel<T> model(T object) {
        if (editable) {
            IModel<T> result = modelCache.get(object);
            if (result == null) {
                result = newModel(object);
                modelCache.put(object, result);
            }
            return result;
        } else {
            return newModel(object);
        }
    }

    /**
     * This method returns a filter based on the defined {@link #keywords} if any, otherwise a {@link Filter#INCLUDE} is
     * returned.
     *
     * <p>Multiple keywords are joined with the <i>OR</i> operator. If a keyword is contained in quotes or
     * double-quotes, the filter for that specific search term is created for the exact match.
     *
     * @return a {@link Filter} which uses the defined {@link #keywords}. If no keyword is present,
     *     {@link Filter#INCLUDE} is returned.
     */
    protected Filter getFilter() {
        return Streams.of(getKeywords())
                .map(this::computeKeywordFilter)
                .reduce(Predicates::or)
                .orElseGet(Predicates::acceptAll);
    }

    private Filter computeKeywordFilter(String keyword) {
        Matcher exactTermKeywordMatcher = EXACT_TERM_KEYWORD_PATTERN.matcher(keyword);
        if (exactTermKeywordMatcher.matches()) {
            return Predicates.exactTermSearch(exactTermKeywordMatcher.group("keyword"));
        } else {
            return Predicates.fullTextSearch(keyword);
        }
    }

    /** Simply wraps the object into a Model assuming the Object is serializable. Subclasses can override this */
    @SuppressWarnings("unchecked")
    protected IModel<T> newModel(T object) {
        return (IModel<T>) new Model<>((Serializable) object);
    }

    /**
     * Simply models the concept of a property in this provider. A property has a key, that identifies it and can be
     * used for i18n, and can return the value of the property given an item served by the {@link GeoServerDataProvider}
     *
     * @author Andrea Aime - OpenGeo
     * @param <T>
     */
    public interface Property<T> extends Serializable {
        public String getName();

        /** Given the item, returns the property */
        public Object getPropertyValue(T item);

        /** Given the item model, returns a model for the property value */
        public IModel<?> getModel(IModel<T> itemModel);

        /** Allows for sorting the property */
        public Comparator<T> getComparator();

        /** If false the property will be used for searches, but not shown in the table */
        public boolean isVisible();

        /** Returns true if it makes sense to search over this property */
        public boolean isSearchable();
    }

    /** Base property class. Assumes T is serializable, if it's not, manually override the getModel() method */
    public abstract static class AbstractProperty<T> implements Property<T> {
        private static final long serialVersionUID = 6286992721731224988L;
        String name;
        boolean visible;

        public AbstractProperty(String name) {
            this(name, true);
        }

        public AbstractProperty(String name, boolean visible) {
            this.name = name;
            this.visible = visible;
        }

        @Override
        public Comparator<T> getComparator() {
            return new PropertyComparator<>(this);
        }

        /**
         * Returns a model based on the getPropertyValue(...) result. Mind, this is not suitable for editable tables, if
         * you need to make one you'll have to roll your own getModel() implementation ( {@link BeanProperty} provides a
         * good example)
         */
        @Override
        public IModel<?> getModel(IModel<T> itemModel) {
            Object value = getPropertyValue(itemModel.getObject());
            if (value instanceof IModel) {
                return (IModel<?>) value;
            } else {
                return new Model<>((Serializable) value);
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public String toString() {
            return "Property[" + name + "]";
        }

        @Override
        public boolean isSearchable() {
            return true;
        }
    }

    /**
     * A Property implementation that uses BeanUtils to access a bean properties
     *
     * @author Andrea Aime - OpenGeo
     * @param <T>
     */
    public static class BeanProperty<T> extends AbstractProperty<T> {
        private static final long serialVersionUID = 5532661316457341748L;
        String propertyPath;

        public BeanProperty(String key) {
            this(key, key, true);
        }

        public BeanProperty(String key, String propertyPath) {
            this(key, propertyPath, true);
        }

        public BeanProperty(String key, String propertyPath, boolean visible) {
            super(key, visible);
            this.propertyPath = propertyPath;
        }

        public String getPropertyPath() {
            return propertyPath;
        }

        /**
         * Overrides the base class {@link #getModel(IModel)} to allow for editable tables: uses a property model
         * against the bean so that writes will hit the bean instead of the possibly immutable values contained in it
         * (think a String property)
         */
        @Override
        public IModel<T> getModel(IModel<T> itemModel) {
            return new PropertyModel<>(itemModel, propertyPath);
        }

        @Override
        public Object getPropertyValue(T bean) {
            // allow rest of the code to assume bean != null
            if (bean == null) return null;

            try {
                return PropertyUtils.getProperty(bean, propertyPath);
            } catch (NestedNullException nne) {
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Could not find property " + propertyPath + " in " + bean.getClass(), e);
            }
        }

        @Override
        public String toString() {
            return "BeanProperty[" + name + "]";
        }
    }

    /**
     * Placeholder for a column that does not contain a real property (for example, a column containing commands instead
     * of data). Will return the item model as the model, and as the property value.
     *
     * @author Andrea Aime
     * @param <T>
     */
    public static class PropertyPlaceholder<T> implements Property<T> {
        private static final long serialVersionUID = -6605207892648199453L;
        String name;

        public PropertyPlaceholder(String name) {
            this.name = name;
        }

        @Override
        public Comparator<T> getComparator() {
            return null;
        }

        @Override
        public IModel<T> getModel(IModel<T> itemModel) {
            return itemModel;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getPropertyValue(T item) {
            return item;
        }

        @Override
        public boolean isVisible() {
            // the very reason for placeholder existence
            // is to show up in the table
            return true;
        }

        @Override
        public String toString() {
            return "PropertyPlacehoder[" + name + "]";
        }

        @Override
        public boolean isSearchable() {
            return false;
        }
    }

    /**
     * Uses {@link Property} to extract the values, and then compares them assuming they are {@link Comparable}
     *
     * @param <T>
     */
    public static class PropertyComparator<T> implements Comparator<T> {
        Property<T> property;

        public PropertyComparator(Property<T> property) {
            this.property = property;
        }

        @Override
        @SuppressWarnings("unchecked")
        public int compare(T o1, T o2) {
            Comparable<Object> p1 = (Comparable<Object>) property.getPropertyValue(o1);
            Comparable<Object> p2 = (Comparable<Object>) property.getPropertyValue(o2);

            // what if any property is null? We assume null < (not null)
            if (p1 == null) return p2 != null ? -1 : 0;
            else if (p2 == null) return 1;

            return p1.compareTo(p2);
        }
    }

    /**
     * A simple comparator inverter
     *
     * @author Andrea Aime - OpenGeo
     * @param <T>
     */
    private static class ReverseComparator<T> implements Comparator<T> {
        Comparator<T> comparator;

        public ReverseComparator(Comparator<T> comparator) {
            this.comparator = comparator;
        }

        @Override
        public int compare(T o1, T o2) {
            return comparator.compare(o1, o2) * -1;
        }
    }
}
