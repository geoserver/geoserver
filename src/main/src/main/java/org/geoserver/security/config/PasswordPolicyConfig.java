/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

/**
 * Password Policy configuration object.
 *
 * @author christian
 */
public class PasswordPolicyConfig extends BaseSecurityNamedServiceConfig {

    private static final long serialVersionUID = 1L;
    private boolean uppercaseRequired, lowercaseRequired, digitRequired;
    private int minLength, maxLength;

    public PasswordPolicyConfig() {
        maxLength = -1;
    }

    public PasswordPolicyConfig(PasswordPolicyConfig other) {
        super(other);
        uppercaseRequired = other.isUppercaseRequired();
        lowercaseRequired = other.isLowercaseRequired();
        digitRequired = other.isDigitRequired();
        minLength = other.getMinLength();
        maxLength = other.getMaxLength();
    }

    /** Is an upper case letter required {@link Character#isUpperCase(char)} */
    public boolean isUppercaseRequired() {
        return uppercaseRequired;
    }

    public void setUppercaseRequired(boolean uppercaseRequired) {
        this.uppercaseRequired = uppercaseRequired;
    }

    /** Is lower case letter required {@link Character#isLowerCase(char)} */
    public boolean isLowercaseRequired() {
        return lowercaseRequired;
    }

    public void setLowercaseRequired(boolean lowercaseRequired) {
        this.lowercaseRequired = lowercaseRequired;
    }

    /** Is digit required {@link Character#isDigit(char)} */
    public boolean isDigitRequired() {
        return digitRequired;
    }

    public void setDigitRequired(boolean digitRequired) {
        this.digitRequired = digitRequired;
    }

    /** The minimal length of a password */
    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    /** The maximal length of a password -1 means no restriction */
    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
}
