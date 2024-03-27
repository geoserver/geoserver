This module is shared with GeoNetwork and contains the basic (shared) code for handling the headers.

Package `username` handles the three ways to extract the username from the header (STRING, JSON, JWT).

Package `roles` handles the two ways to extract the roles list from the header (JSON and JWT) as well as RoleConversion.

Package `token` handles validating an Access Token (Signature, Expiry, Endpoint, Audience, and Subject).