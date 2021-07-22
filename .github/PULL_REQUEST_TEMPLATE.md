
<!--Include a few sentences describing the overall goals for this Pull Request-->
  
<!-- Please help our volunteers reviewing this PR by completing the following items. 
Ask in a comment if you have troubles with any of them. -->

# Checklist

- [ ] I have read the [contribution guidelines](https://github.com/geoserver/geoserver/blob/main/CONTRIBUTING.md).
- [ ] I have sent a [Contribution Licence Agreement](https://docs.geoserver.org/latest/en/developer/policies/committing.html) (not required for small changes, e.g., fixing typos in documentation).
- [ ] First PR targets the `main` branch (backports managed later; ignore for branch specific issues).
- [ ] All the build checks are green ([see automated QA checks](https://docs.geoserver.org/latest/en/developer/qa-guide/index.html)).

For core and extension modules:

- [ ] New unit tests have been added covering the changes.
- [ ] [Documentation](https://github.com/geoserver/geoserver/tree/main/doc/en) has been updated (if change is visible to end users).
- [ ] The [REST API docs](https://github.com/geoserver/geoserver/tree/main/doc/en/api/1.0.0) have been updated (when changing configuration objects or the REST controllers).
- [ ] There is an issue in the [GeoServer Jira](https://osgeo-org.atlassian.net/projects/GEOS) (except for changes not visible to end users). 
- [ ] Commit message(s) must be in the form ``[GEOS-XYZW] Title of the Jira ticket``.
- [ ] Bug fixes and small new features are presented as a single commit.
- [ ] The commit targets a single objective (if multiple focuses cannot be avoided, each one is in its own commit, and has a separate ticket describing it).

<!--Submitting the PR does not require you to check all items, but by the time it gets merged, they should be either satisfied or not applicable.-->
