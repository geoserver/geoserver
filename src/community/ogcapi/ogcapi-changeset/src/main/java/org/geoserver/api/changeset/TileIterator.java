/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.geowebcache.grid.GridSubset;

/** Iterates over the tiles covered by a given set of grid subsets */
class TileIterator implements Iterator<long[]> {

    private long minY;
    private int zoomStart;
    private int zoomEnd;
    private List<GridSubset> subsets;
    private long x;
    private long y;
    private int z;
    // cached to avoid reallocation, but also returned to the caller of next, so can't contain the
    // current position
    private long[] resultPosition = new long[3];
    private long[] tmpPosition = new long[3];
    private List<GridSubset> rowSubsets = new ArrayList<>();
    private long rowMaxX;

    TileIterator(List<GridSubset> subsets, int zoomStart, int zoomEnd) {
        this.zoomStart = zoomStart;
        this.zoomEnd = zoomEnd;

        if (subsets.isEmpty()) {
            // force the iterator to be at its end already, hasNext() will return false
            this.z = zoomEnd + 1;

        } else {
            this.subsets = subsets;

            this.z = zoomStart;
            // place y just before
            minY =
                    subsets.stream()
                            .map(ss -> ss.getCoverage(z)[1])
                            .min((l1, l2) -> Long.compare(l1, l2))
                            .get();
            this.y = minY - 1;
            nextRow();
        }
    }

    private boolean nextRow() {
        // move to next row
        y++;
        rowSubsets = getRowSubsets(y);

        // if no grid subset in the next row, try to see if there are grisets intersecting
        // rows below
        if (rowSubsets.isEmpty()) {
            // get the next min y higher than y

            Optional<Long> nextY =
                    subsets.stream()
                            .filter(
                                    ss -> {
                                        long[] coverage = ss.getCoverage(z);
                                        return coverage[1] > y;
                                    })
                            .map(ss -> ss.getCoverage(z)[1])
                            .min((l1, l2) -> Long.compare(l1, l2));
            if (nextY.isPresent()) {
                y = nextY.get();
                rowSubsets = getRowSubsets(y);
            } else {
                return false;
            }
        }

        // compute the starting column and end column for this row
        this.x =
                rowSubsets
                        .stream()
                        .map(ss -> ss.getCoverage(z)[0])
                        .min((l1, l2) -> Long.compare(l1, l2))
                        .get();
        this.rowMaxX =
                rowSubsets
                        .stream()
                        .map(ss -> ss.getCoverage(z)[2])
                        .max((l1, l2) -> Long.compare(l1, l2))
                        .get();

        return true;
    }

    private List<GridSubset> getRowSubsets(long y) {
        return subsets.stream()
                .filter(
                        ss -> {
                            long[] coverage = ss.getCoverage(z);
                            return this.y >= coverage[1] && y <= coverage[3];
                        })
                .sorted((ss1, ss2) -> Long.signum(ss1.getCoverage(z)[0] - ss1.getCoverage(z)[0]))
                .collect(Collectors.toList());
    }

    private boolean nextColumn() {
        while (x <= rowMaxX && !rowSubsets.isEmpty()) {
            x++;
            tmpPosition[0] = x;
            tmpPosition[1] = y;
            tmpPosition[2] = z;
            if (rowSubsets.get(0).covers(tmpPosition)) {
                return true;
            }

            while (!rowSubsets.isEmpty()) {
                rowSubsets.remove(0);
                if (!rowSubsets.isEmpty()) {
                    if (rowSubsets.get(0).covers(tmpPosition)) {
                        return true;
                    } else {
                        long temp = rowSubsets.get(0).getCoverage(z)[0];
                        if (temp > x) {
                            x = temp;
                            return true;
                        }
                    }
                }
            }
        }

        return !rowSubsets.isEmpty() && x <= rowMaxX;
    }

    @Override
    public boolean hasNext() {
        return z <= zoomEnd;
    }

    @Override
    public long[] next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        resultPosition[0] = x;
        resultPosition[1] = y;
        resultPosition[2] = z;

        if (z <= zoomEnd) {
            if (!nextColumn() && !nextRow()) {
                // start the next level, nextrow will initialize the data structure
                z++;
                if (z <= zoomEnd) {
                    y = minY - 1;
                    nextRow();
                }
            }
        }

        return resultPosition;
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + "," + z + "]. Over " + subsets;
    }
}
