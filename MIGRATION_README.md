# GeoServer Security Migration Guide

This guide explains how to migrate GeoServer from legacy JKS/JCEKS keystores to BCFKS (Bouncy Castle FIPS Keystore) or PKCS#11-backed formats, and from legacy encryption algorithms to AES-GCM with NIST-approved algorithms.

## Overview

The migration includes:

1. **Keystore Migration**: From JKS/JCEKS to BCFKS or PKCS#11
2. **Password Encryption Migration**: From legacy algorithms to AES-GCM
3. **Enhanced Security**: FIPS compliance and NIST-approved algorithms

## Prerequisites

- Java 8 or higher
- Bouncy Castle FIPS provider (already included in dependencies)
- For PKCS#11: Proper HSM configuration and drivers

## Migration Steps

### 1. Backup Your Current Configuration

Before starting the migration, backup your current GeoServer data directory:

```bash
cp -r /path/to/geoserver/data /path/to/geoserver/data.backup
```

### 2. Keystore Migration

#### Option A: Migrate to BCFKS (Recommended)

```bash
# Using the migration utility
java -cp geoserver.jar org.geoserver.security.MigrateKeystore \
  /path/to/old/geoserver.jceks \
  old_password \
  /path/to/new/geoserver.bcfks \
  new_password \
  BCFKS \
  BCFIPS
```

#### Option B: Migrate to PKCS#11

```bash
# Using the migration utility with PKCS#11
java -cp geoserver.jar org.geoserver.security.MigrateKeystore \
  /path/to/old/geoserver.jceks \
  old_password \
  /path/to/new/geoserver.pkcs11 \
  new_password \
  PKCS11 \
  SunPKCS11
```

### 3. Password Encryption Migration

The new AES-GCM password encoder is automatically available as `crypt3`. To migrate existing passwords:

1. **For Configuration Passwords**: Update the security configuration to use the new encoder
2. **For User Passwords**: Users will need to reset their passwords to use the new encryption

### 4. Configuration Updates

#### Update Security Configuration

In your `applicationSecurityContext.xml`, the new password encoders are already configured:

```xml
<!-- Standard PBE with BCFIPS -->
<bean id="pbePasswordEncoder"    
  class="org.geoserver.security.password.GeoServerPBEPasswordEncoder" scope="prototype">
  <property name="prefix" value="crypt1" />
  <property name="providerName" value="BCFIPS" />
  <property name="algorithm" value="PBEWITHSHA256ANDAES_256" />
</bean>

<!-- Strong PBE with BCFIPS -->
<bean id="strongPbePasswordEncoder"    
  class="org.geoserver.security.password.GeoServerPBEPasswordEncoder" scope="prototype">
  <property name="prefix" value="crypt2" />
  <property name="providerName" value="BCFIPS" />
  <property name="algorithm" value="PBEWITHSHA256ANDAES_256" />
  <property name="availableWithoutStrongCryptogaphy" value="false" />
</bean>

<!-- AES-GCM PBE (Recommended) -->
<bean id="aesGcmPasswordEncoder"    
  class="org.geoserver.security.password.GeoServerAESGCMPasswordEncoder" scope="prototype">
  <property name="prefix" value="crypt3" />
</bean>
```

#### For PKCS#11 Configuration

If using PKCS#11, create a custom KeyStore provider:

```xml
<bean id="pkcs11KeyStoreProvider" 
  class="org.geoserver.security.PKCS11KeyStoreProvider">
  <property name="pkcs11ConfigPath" value="/path/to/pkcs11.conf" />
  <property name="slotId" value="0" />
</bean>
```

### 5. Validation

Use the migration script to validate your keystore:

```bash
java -cp geoserver.jar org.geoserver.security.MigrationScript \
  validate-keystore \
  /path/to/geoserver.bcfks \
  your_password
```

## Security Improvements

### Keystore Security

- **BCFKS**: FIPS-compliant keystore format with enhanced security
- **PKCS#11**: Hardware-backed security with HSM support
- **AES-256**: 256-bit encryption keys
- **SHA-256**: Secure hash algorithms

### Password Encryption

- **AES-GCM**: Authenticated encryption with NIST approval
- **PBKDF2**: Secure key derivation with 100,000 iterations
- **256-bit keys**: Maximum security strength
- **Random IVs**: Unique initialization vectors for each encryption

## Troubleshooting

### Common Issues

1. **BCFIPS Provider Not Found**
   - Ensure Bouncy Castle FIPS is in the classpath
   - Check that the provider is properly registered

2. **PKCS#11 Configuration**
   - Verify HSM drivers are installed
   - Check PKCS#11 configuration file
   - Ensure proper permissions for HSM access

3. **Password Migration**
   - Old passwords encrypted with legacy algorithms will need to be re-encrypted
   - Users may need to reset passwords to use new encryption

### Logging

Enable debug logging for security operations:

```properties
# In logging.properties
org.geoserver.security.level=FINE
```

## Rollback Plan

If migration fails, you can rollback:

1. Restore the backup data directory
2. Revert to the previous GeoServer version
3. Use the old keystore and encryption settings

## Support

For issues with the migration:

1. Check the GeoServer logs for detailed error messages
2. Verify all prerequisites are met
3. Test the migration in a development environment first
4. Contact the GeoServer community for assistance

## Compliance

This migration ensures:

- **FIPS 140-2 Compliance**: Through BCFKS and BCFIPS provider
- **NIST Standards**: AES-GCM and PBKDF2 algorithms
- **Enhanced Security**: 256-bit keys and secure random generation
- **Hardware Security**: Optional PKCS#11 HSM support 