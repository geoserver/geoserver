/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.Serializable;

/**
 * Configuration object for WMS water marking.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface WatermarkInfo extends Serializable {

    /** The position of a watermark logo. */
    public static enum Position {
        TOP_LEFT {

            public int getCode() {
                return 0;
            }
        },
        TOP_CENTER {

            public int getCode() {
                return 1;
            }
        },
        TOP_RIGHT {

            public int getCode() {
                return 2;
            }
        },
        MID_LEFT {

            public int getCode() {
                return 3;
            }
        },
        MID_CENTER {

            public int getCode() {
                return 4;
            }
        },
        MID_RIGHT {

            public int getCode() {
                return 5;
            }
        },
        BOT_LEFT {

            public int getCode() {
                return 6;
            }
        },
        BOT_CENTER {

            public int getCode() {
                return 7;
            }
        },
        BOT_RIGHT {

            public int getCode() {
                return 8;
            }
        };

        public abstract int getCode();

        public static Position get(int code) {
            for (Position p : values()) {
                if (code == p.getCode()) {
                    return p;
                }
            }

            return null;
        }
    };

    /** Flag indicating if water marking is enabled. */
    boolean isEnabled();

    /** Sets flag indicating if water marking is enabled. */
    void setEnabled(boolean enabled);

    /**
     * The position of the watermark on resulting wms images.
     *
     * <p>
     *
     * <pre>
     * O -- O -- O      0 -- 1 -- 2
     * |    |    |      |    |    |
     * O -- O -- O  ==  3 -- 4 -- 5
     * |    |    |      |    |    |
     * O -- O -- O      6 -- 7 -- 8
     * </pre>
     */
    Position getPosition();

    /** Sets the watermark position. */
    void setPosition(Position position);

    /**
     * The url of the watermark.
     *
     * <p>This is usually the location of some logo.
     */
    String getURL();

    /** Sets the url of the watermark. */
    void setURL(String url);

    /** The transparency of the watermark logo, ranging from 0 to 255. */
    int getTransparency();

    /** Sets the transparanecy of the watermark logo. */
    void setTransparency(int transparency);
}
