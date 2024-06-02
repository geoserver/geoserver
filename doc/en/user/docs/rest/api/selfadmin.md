# Self admin

Self admin operations allow a user to perform actions on the user's own info.

Calls to the self admin operations are disabled by default. You'll have to edit the `rest.properties` file (more info at the [REST services](../../security/service.md#security_service_rest) page) and add the line:

    /rest/security/self/**;GET,POST,PUT,DELETE=ROLE_AUTHENTICATED

## `/security/self/password`

Allows a user to change own password

!!! warning

    The use of HTTPS is recommended, otherwise all password are sent in plain text.

| Method | Action                    | Status code | Formats   | Default Format |
|--------|---------------------------|-------------|-----------|----------------|
| PUT    | Changes the user password | 200,400,424 | XML, JSON |                |

Formats for PUT (password change).

**XML**

``` xml
<userPassword>
   <newPassword>newPassword</newPassword>
</userPassword>
```

**JSON**

``` json
{ "newPassword":"newPassword" }
```

### Exceptions

| Exception                                       | Status code | Error string (payload)                      |
|-------------------------------------------------|-------------|---------------------------------------------|
| PUT with an invalid `newPassword` or bad params | 400         | `Missing 'newPassword'`                     |
| PUT for user not updatable                      | 424         | `User service does not support changing pw` |
