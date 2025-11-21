/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.components;

import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Setter;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.geoserver.acl.plugin.web.support.SerializablePredicate;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.springframework.dao.DuplicateKeyException;

@SuppressWarnings("serial")
public abstract class RulesDataProvider<R> extends GeoServerDataProvider<R> {

    private final List<R> _rules = new ArrayList<>();

    private Class<R> modelClass;

    private @Setter SerializablePredicate<R> filter;

    protected RulesDataProvider(Class<R> modelClass) {
        this.modelClass = modelClass;
        setSort("priority", SortOrder.ASCENDING);
    }

    public Class<R> getModelClass() {
        return modelClass;
    }

    @Override
    protected Comparator<R> getComparator(SortParam<?> sort) {
        return null;
    }

    @Override
    public void setSort(SortParam<Object> param) {
        super.setSort(param);
        Collections.sort(getItems(), super.getComparator(param));
    }

    @Override
    public List<R> getItems() {
        if (_rules.isEmpty()) reload();
        SerializablePredicate<R> predicate = filter;
        if (predicate == null) return _rules;
        return _rules.stream().filter(predicate).collect(Collectors.toList());
    }

    private final void reload() {
        List<R> fresh = doReload();
        _rules.clear();
        _rules.addAll(fresh);
    }

    public void remove(Collection<R> selected) {
        if (selected.isEmpty()) return;
        for (R rule : selected) {
            delete(rule);
        }
        reload();
    }

    public void save(R rule) throws DuplicateKeyException {
        update(rule);
        reload();
    }

    public boolean canUp(R rule) {
        return filter == null && getItems().indexOf(rule) > 0;
    }

    public void moveUp(R rule) {
        List<R> rules = getItems();
        int index = rules.indexOf(rule);
        if (index > 0) {
            swap(rule, rules.get(index - 1));
            rules.remove(index);
            rules.add(index - 1, rule);
        }
    }

    public boolean canDown(R rule) {
        List<R> rules = getItems();
        return filter == null && rules.indexOf(rule) < rules.size() - 1;
    }

    public void moveDown(R rule) {
        List<R> rules = getItems();
        int index = rules.indexOf(rule);
        if (index < rules.size() - 1) {
            swap(rule, rules.get(index + 1));
            rules.remove(index);
            rules.add(index + 1, rule);
        }
    }

    protected abstract R update(R rule) throws DuplicateKeyException;

    public void onDrop(R movedRule, R targetRule) {
        if (Objects.equal(getId(movedRule), getId(targetRule))) {
            return;
        }
        final long pmoved = getPriority(movedRule);
        long ptarget = getPriority(targetRule);
        if (pmoved < ptarget) {
            setPriority(movedRule, ptarget + 1);
        } else {
            setPriority(movedRule, ptarget);
        }
        save(movedRule);
    }

    @Override
    public abstract List<Property<R>> getProperties();

    protected abstract void delete(R rule);

    protected abstract void swap(R rule, R otherRule);

    protected abstract List<R> doReload();

    protected abstract String getId(R movedRule);

    protected abstract long getPriority(R movedRule);

    protected abstract void setPriority(R movedRule, long l);
}
