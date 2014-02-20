/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Juha Hyvärinen / Cyberlightning Ltd
 * 
 */
package org.geoserver.w3ds.types;

import java.text.DecimalFormat;

public class Vector3 {
public double x, y, z;

private Vector3 Normal = null;

/*
 * Construct and initialize new Vector3 with given values
 */
public Vector3(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
}

/*
 * Construct and initialize new Vector3 with default values of 0.0
 */
public Vector3() {
    x = 0.0;
    y = 0.0;
    z = 0.0;
}

/*
 * Copy constructor from Vector3 class
 */
public Vector3(Vector3 v) {
    x = v.x;
    y = v.y;
    z = v.z;
}

public static Vector3 Cross(Vector3 v1, Vector3 v2) {
    Vector3 result = new Vector3();
    result.x = (v1.y * v2.z) - (v1.z * v2.y);
    result.y = (v1.z * v2.x) - (v1.x * v2.z);
    result.z = (v1.x * v2.y) - (v1.y * v2.x);
    return result;
}

public static Vector3 Normalize(Vector3 v1) {
    double length = v1.x * v1.x + v1.y * v1.y + v1.z * v1.z;
    length = java.lang.Math.sqrt(length);

    v1.x /= length;
    v1.y /= length;
    v1.z /= length;

    return v1;
}

public Vector3 Minus(Vector3 v2) {
    Vector3 v1 = new Vector3(this.x, this.y, this.z);
    v1.x -= v2.x;
    v1.y -= v2.y;
    v1.z -= v2.z;

    return v1;
}

public Vector3 Add(Vector3 v1) {
    this.x += v1.x;
    this.y += v1.y;
    this.z += v1.z;

    return this;
}

public void addNormal(Vector3 normal) {
    if (Normal == null) {
        Normal = new Vector3();
    }

    Normal.x += normal.x;
    Normal.y += normal.y;
    Normal.z += normal.z;
}

public Vector3 getNormal() {
    if (Normal == null) {
        Normal = new Vector3();
    }
    return Normal;
}

public void setNormal(Vector3 normal) {
    Normal = normal;
}

@Override
public boolean equals(Object obj) {
    if (obj != null) {
        if (obj instanceof Vector3) {
            Vector3 other = (Vector3) obj;
            if (other.x == this.x) {
                if (other.y == this.y) {
                    if (other.z == this.z) {
                        return true;
                    }
                }
            }
        }
    }
    return false;
}

@Override
public int hashCode() {
    int hashCode = 0;

    hashCode = hashCode * 37 + (int) this.x;
    hashCode = hashCode * 37 + (int) this.y;
    hashCode = hashCode * 37 + (int) this.z;

    return hashCode;
}

@Override
public String toString() {
    // Formatter for double values, since we don't want more than 6 decimals
    DecimalFormat decimalFormat = new DecimalFormat("0.0#####");
    return decimalFormat.format(x) + " " + decimalFormat.format(y) + " " + decimalFormat.format(z);
}
}
