.. _versioning_requirements:

WFS versioning requirement specification
=========================================

First off, we don't have a full requirements list, but a general set of wishes from the following pages:

    * http://docs.codehaus.org/display/GEOS/GeoCollaborator
    * http://lists.eogeo.org/pipermail/opensdi/2005-April/000071.html
    * http://lists.eogeo.org/pipermail/opensdi/2005-November/000277.html

From these pages we can gather the following requirements:

#.  "Wiki for geospatial data" and "for us the two defining features of a wiki are the attribution and the ability to 'diff' and 'roll back".
#. "The CVS of geospatial data"
#. Easier to use than commercial softwares (less maintenance I guess?)
#. Implemented as a set of plug-ins into the WFS transaction calls, among others, such as validation (so that bad data is not accepted) and change notification (mails, rss, and so on)
#. Some kind of delayed commit, "Another possible plug-in could be 'peer review' holding area, where some one must explicitly approve a change instead of checking after and rolling it back";
#. Patches, "it'd also be nice if there were an easy way to make a geospatial 'patch', based on specific constraints such as version numbers, area, user that provided them.
#. Checkouts, as in a classic version control system: caching and consuming of WFS. If you have a version table, that has timestamps as to the last updates, then the client really only needs to access the WFS once. And indeed in some cases you could even send out clients with the WFS layers pre-cached. When a client examines the layer again, they don't need to download the full WFS, they can just check the version table if there are updates since the last time they checked. If there are, then they can just download those directly, put them in the cache"and also "The version WFS would become the version control, and one could do diffs of ones locally modified files against the server. This also goes back to low and no bandwidth situations - files could be updated asynchronously, an update can come on a cd/dvd/firewire drive from the main office".

Extra requirements that do not appear to be cited in the above documents, but seem to be important to me as well:

* Object of versioning: I'd say, given the simple feature nature of our implementation, that we want to version multiple layers at the same time, since changes on one layer may raise incompatibilities on other layers. For example, what if you move a road so that it overlaps with a building? Moreover, standard WFS-T calls are designed to work against multiple feature types in each call.
* Branches: requirement number 5 could be turned into a request to handling branches, which is something that ArcSDE does, even if in SDE these are just called "versions".
* Tags, since we want to be able and extract maps in a verified state.
* Versioned tree space management: after a while every heavily used versioning system shows the need for cleanup, to reduce the load on the server, speed up searches and general functioning.
* Speed: we need to decide which operations should be speedy, which should be costly. Out of the box, I'd say we want the last revision of each "branch" to be quick to check out, almost as if it wasn't versioning around, and have to pay an extra cost for intermediate revisions and diffs.

The above should take into consideration space too. Hum, speed, space, ease of implementation, pick any two?
Other interesting questions are:

* Do we want to be able and *version schema changes* as well? What happens the day we need to add/remove/change an attribute in the feature?
* Do we want to *keep data structures untouched*, and allow people to keep on accessing them read only as if versioning was not around, or do we make it explicit that this is a different kind of beast and must be accessed accordingly?

**Comments**

* Cameron Shorter: To be effective, this functionality will require User Authentication and Authorization. This needs to be mixed with business rules. Group A is allowed to enter new features, Group B is allowed to edit existing features, Group C is allowed to set the "Reviewed" attribute to TRUE.
* Brent Owens: Branches? Branches are great for software because 'updating' software can take a while and they are a handy way to stay in sync and merge back into trunk. Data on the other hand, it often goes through QA before it gets thrown into the central repository. I'm just not seeing the need to make a branch of a dataset. A copy of a dataset? Yes, a branch, no.
* Brent Owens: Long transactions: I'm updating an area of a dataset, I have it locked so no one else can edit it while I am, but my update takes 2 days to process. Can we handle long transactions? Do we want to tackle it for this?
* Brent Owens: As Cameron stated, we need to allow for business rules to plug in. There is no way we can tackle this for all cases, but I think we can cover a few common/simple ones.

Spatial data versioning experiences
------------------------------------

The following list of article contain real world experiences about spatial data and versioning. If you know more, please contribute.

* `Using ArcSDE Versions in the Real World <http://gis.esri.com/library/userconf/proc01/professional/papers/pap232/p232.htm>`

**Comments**

* Brent Owens: Having the history queryable is a huge bonus. Often I had to go back and find out what features were changed during a certain time frame. Or I had to use a snapshot of the dataset from a certain time. The versioning was built into our system so this was easy to do, and very useful; we just kept copies of each feature with a flag stating if it is the "current working version".
* Brent Owens: Deep histories (storing all history from every version) was also a great way to see how the data was changing over the years and for generating some statistics on how it was changing: "this many roads became paved this year."

WFS-T extensions
-----------------

That is, how do we perform versioning operations, and eventually provide enough information to build a client using some form of checkout, with the WFS Transaction and GetFeature calls? Are these enough?

GetFeature call must be extended so that we can specify which branch/tag we want to checkout, eventually which revision, if we go for the svn revision model, or which date. GetFeature response should include at least revision and tag/branch information.

Transaction calls should be extended to handle branch/tag and revision number so that we can check the modification is going over an up to date feature (do we need that? guess so, unless we go for a reserved checkout model leveraging lock calls?).
Transaction responses should be extended to provide conflict informations (and eventually new revision numbers if we go the svn model?).

The same goes for lock calls, we need to specify what we do want to lock (at least the branch/tag, not sure we may want to lock a specific revision).

Most of the above requirements, beside the extended answers, come as natural if branch and revision are included in the feature type as extra attributes. We need those in order to build a checkout, but also to check for conflicts during transactional updates. As Jody suggested, the ranges of revision numbers could be provided through a DescribeFeatureType call, using a "choice" construct.

An alternative could be to have a separate WFS server (different getCapabilities addresses) for each branch of the server. This implies a vision of versioning that spans the entire server. The main drawback I see is that this way the versioning handling seems to be locked to a specific datastore, or to an extension that can perform versioning independent of the datastore. Moreover, it apparently implies that all feature types server by the server are versioned.

Versioning at the datastore level allows to have a single server with multiple and separate versioned sets of feature type, along with non versioned types as well. Versioning at the WFS level seems to deny it.

Plus, depending on the implementation, it may be hard to add versioning on top of a data store that does not support it on its own, the implications would be that:

* the original data set has to stay unchanged, so it cannot be served as is beyond GetFeatures. It also seems to me more common to have people ask for the last version or a specific tag.
* if versioned operations go on for enough time, most of the data volume is in version tables, so I guess it may be just sensible to start by putting the shapefile or whatever the format is in the versioned datastore itself.

Scope and scheduling
---------------------

Versioning will be implemented in two distinct phases:

* Basic versioning support: attribution, versions, rollback, possibly checkout support. No branches, no tags. Just get on par with OpenStreetMap functionality.
* Enterprise versioning support: add branches and tags, merges and eventually checkout support should it have been excluded from the first phase due to time constraints.
