/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.geoserver.security.csp.predicate.CSPPredicate;
import org.geoserver.security.csp.predicate.CSPPredicateParameter;
import org.geoserver.security.csp.predicate.CSPPredicatePath;
import org.geoserver.security.csp.predicate.CSPPredicateProperty;

/**
 * Contains a filter for HTTP requests and the Content Security Policy directives to use for requests matching the
 * filter.
 */
public class CSPRule implements CSPPredicate, Serializable {

    private static final long serialVersionUID = 838336921193518382L;

    // default values
    private static final String DEFAULT_NAME = null;
    private static final String DEFAULT_DESCRIPTION = "";
    private static final Boolean DEFAULT_ENABLED = true;
    private static final String DEFAULT_FILTER = "";
    private static final String DEFAULT_DIRECTIVES = "";

    /** Splitter to extract the individual predicates in the filter string */
    private static final Splitter PREDICATE_SPLITTER =
            Splitter.on(" AND ").trimResults().omitEmptyStrings();

    /** Splitter to extract the arguments of a predicate */
    private static final Splitter COMMA_SPLITTER =
            Splitter.on(',').trimResults().limit(2);

    /** The rule name */
    private String name;

    /** The rule description */
    private String description;

    /** Whether the rule is enabled */
    private Boolean enabled;

    /** The filter string for matching HTTP requests */
    private String filter;

    /** The Content-Security-Policy header directives */
    private String directives;

    /** The predicate objects parsed from the filter string */
    private transient List<CSPPredicate> predicates = null;

    /** Creates a new CSPRule object with default values. */
    public CSPRule() {
        this(DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_ENABLED, DEFAULT_FILTER, DEFAULT_DIRECTIVES);
    }

    /**
     * Creates a new CSPRule object with the provided values.
     *
     * @param name the rule name
     * @param description the rule description
     * @param enabled whether the rule is enabled
     * @param filter the filter string for matching HTTP requests
     * @param directives the Content-Security-Policy header directives
     */
    public CSPRule(String name, String description, boolean enabled, String filter, String directives) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.filter = filter;
        this.directives = directives;
    }

    /**
     * Creates a copy of the provided CSPRule object.
     *
     * @param other the rule to copy
     */
    public CSPRule(CSPRule other) {
        this(other.getName(), other.getDescription(), other.isEnabled(), other.getFilter(), other.getDirectives());
    }

    /** @return the rule name */
    public String getName() {
        return this.name;
    }

    /** @param name the rule name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the rule description */
    public String getDescription() {
        return this.description;
    }

    /** @param description the rule description */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return whether the rule is enabled */
    public boolean isEnabled() {
        return this.enabled;
    }

    /** @param enabled whether the rule is enabled */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @return the filter string for matching HTTP requests */
    public String getFilter() {
        return this.filter;
    }

    /** @param filter the filter string for matching HTTP requests */
    public void setFilter(String filter) {
        this.filter = filter;
    }

    /** @return the Content-Security-Policy header directives */
    public String getDirectives() {
        return this.directives;
    }

    /** @param directives the Content-Security-Policy header directives */
    public void setDirectives(String directives) {
        this.directives = directives;
    }

    /**
     * Parses the filter string into a list of CSPPredicate objects. The predicate will be in a fixed sort order based
     * on type but the order of predicates within each type will be preserved from the filter string.
     *
     * @throws IllegalArgumentException if the filter could not be parsed
     */
    public void parseFilter() {
        this.predicates = parseFilter(this.filter);
    }

    /**
     * Returns true if this rule is enabled and all of its predicates match the provided request. Otherwise, returns
     * false.
     *
     * @param request the HTTP request
     * @return whether this rule matches the request
     * @throws IllegalStateException if the filter has not been parsed
     */
    @Override
    public boolean test(CSPHttpRequestWrapper request) {
        Preconditions.checkState(this.predicates != null, "The filter has not been parsed yet");
        return this.enabled && this.predicates.stream().allMatch(p -> p.test(request));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CSPRule) {
            CSPRule other = (CSPRule) obj;
            return Objects.equals(this.name, other.name)
                    && Objects.equals(this.description, other.description)
                    && Objects.equals(this.enabled, other.enabled)
                    && Objects.equals(this.filter, other.filter)
                    && Objects.equals(this.directives, other.directives);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.description, this.enabled, this.filter, this.directives);
    }

    /** Initialize after XStream deserialization */
    private Object readResolve() {
        Preconditions.checkNotNull(this.name, "The rule name can not be null");
        this.description = firstNonNull(this.description, DEFAULT_DESCRIPTION);
        this.enabled = firstNonNull(this.enabled, DEFAULT_ENABLED);
        this.filter = firstNonNull(this.filter, DEFAULT_FILTER);
        this.directives = firstNonNull(this.directives, DEFAULT_DIRECTIVES);
        return this;
    }

    /**
     * Parses the filter string into a list of CSPPredicate objects. The predicate will be in a fixed sort order based
     * on type but the order of predicates within each type will be preserved from the filter string.
     *
     * @param filter the filter string
     * @return the parsed predicates
     * @throws IllegalArgumentException if the filter could not be parsed
     */
    public static List<CSPPredicate> parseFilter(String filter) {
        filter = CSPUtils.trimWhitespace(filter);
        try {
            Map<Integer, List<CSPPredicate>> map = new TreeMap<>();
            for (String pred : PREDICATE_SPLITTER.split(filter)) {
                int index = pred.indexOf('(');
                Preconditions.checkArgument(index > 0, "Unable to determine type of predicate: %s", pred);
                Preconditions.checkArgument(
                        pred.charAt(pred.length() - 1) == ')', "Unable to parse arguments in predicate: %s", pred);
                String type = pred.substring(0, index).trim().toUpperCase();
                String arg = pred.substring(index + 1, pred.length() - 1).trim();
                List<String> args = COMMA_SPLITTER.splitToList(arg);
                switch (type) {
                    case "PROP":
                        Preconditions.checkArgument(args.size() == 2, "Insuffient arguments in predicate: %s", pred);
                        addToMap(map, 1, new CSPPredicateProperty(args.get(0), args.get(1)));
                        break;
                    case "PATH":
                        addToMap(map, 2, new CSPPredicatePath(arg));
                        break;
                    case "PARAM":
                        Preconditions.checkArgument(args.size() == 2, "Insuffient arguments in predicate: %s", pred);
                        addToMap(map, 3, new CSPPredicateParameter(args.get(0), args.get(1)));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown type for predicate: " + pred);
                }
            }
            return Collections.unmodifiableList(map.entrySet().stream()
                    .map(Entry::getValue)
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse predicates from string '" + filter + "'", e);
        }
    }

    /**
     * Adds the predicate to the appropriate list in the map, initializing a new list if it doesn't already exist.
     *
     * @param map the map of predicates
     * @param key the map key
     * @param predicate the predicate
     */
    private static void addToMap(Map<Integer, List<CSPPredicate>> map, int key, CSPPredicate predicate) {
        map.computeIfAbsent(key, x -> new ArrayList<>()).add(predicate);
    }
}
