/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cog;

import it.geosolutions.imageioimpl.plugins.cog.AzureRangeReader;
import it.geosolutions.imageioimpl.plugins.cog.GSRangeReader;
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
        /** Generic HTTP range reader */
        HTTP {

            @Override
            String getRangeReaderClassName() {
                return HttpRangeReader.class.getName();
            }
        },
        /** Reader using the S3 API (with security support and whatnot) */
        S3 {

            @Override
            String getRangeReaderClassName() {
                return S3RangeReader.class.getName();
            }
        },
        /** Reader using the Google Storage API (again, with security support) */
        GS {

            @Override
            String getRangeReaderClassName() {
                return GSRangeReader.class.getName();
            }
        },
        /** Reader using the Azure API (again, with security support) */
        Azure {

            @Override
            String getRangeReaderClassName() {
                return AzureRangeReader.class.getName();
            }
        };
        ;

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
