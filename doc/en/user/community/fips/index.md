# FIPS 140-3

The FIPS module runs GeoServer against a FIPS 140-3 validated cryptography module, so every
cryptographic operation GeoServer performs happens inside a validated boundary. It replaces the
regular BouncyCastle library with the FIPS validated one, registers it ahead of the Java providers,
and sets it in approved-only mode. It also stores passwords with AES-GCM, which the validated module
supports.

Deployments that need this are the ones bound by a policy such as FIPS 140-3, usually United States
federal ones or their suppliers.

<div class="grid cards" markdown>

- [Installing the FIPS module](installing.md)
- [Running GeoServer under FIPS](running.md)

</div>

!!! warning
    A data directory written by a normal GeoServer cannot be opened by a FIPS one. Passwords and the
    keystore use algorithms that approved-only mode refuses. Start from a new data directory; there
    is no migration tool yet. See [Running GeoServer under FIPS](running.md).

!!! warning
    The FIPS module works on Linux only.
