/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cog;

import it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader;
import it.geosolutions.imageioimpl.plugins.cog.S3RangeReader;
import java.io.Serializable;

/** Basic Cog Settings such as RangeReader and caching capabilities */
public class CogSettings implements Serializable {

    public static final String COG_SCHEMA = "cog";

    public static final String COG_PREFIX = COG_SCHEMA + ":";

    public static final String COG_URL_HEADER = "cog://";

    public CogSettings(CogSettings settings) {
        this.rangeReaderSettings = settings.rangeReaderSettings;
        this.useCachingStream = settings.useCachingStream;
    }

    public CogSettings() {}

    public enum RangeReaderType {
        HTTP {

            @Override
            String getRangeReaderClassName() {
                return HttpRangeReader.class.getName();
            }
        },
        S3 {

            @Override
            String getRangeReaderClassName() {
                return S3RangeReader.class.getName();
            }
        };

        abstract String getRangeReaderClassName();
    };

    public static final String COG_SETTINGS_KEY = "CogSettings.Key";

    public static final boolean DEFAULT_USE_CACHING_STREAM = false;

    protected boolean useCachingStream = DEFAULT_USE_CACHING_STREAM;

    protected RangeReaderType rangeReaderSettings = RangeReaderType.HTTP;

    public RangeReaderType getRangeReaderSettings() {
        return rangeReaderSettings;
    }

    public void setRangeReaderSettings(RangeReaderType rangeReaderSettings) {
        this.rangeReaderSettings = rangeReaderSettings;
    }

    public boolean isUseCachingStream() {
        return useCachingStream;
    }

    public void setUseCachingStream(boolean useCachingStream) {
        this.useCachingStream = useCachingStream;
    }
}
