.. _versioning_implementation_chris:

Early implementation proposal
=======================================================

Chris already provided his own vision about an implementation on the OpenSDI mailing list in April 2005.

It's a nice starting point, and also works in a way different than ArcSDE, so it's an interesting read.

What I'm envisioning at the moment is a table in a database that records every single update/insert action:

* It holds the diff, ie the changes between what the feature was and what the new one is.
* It holds the username of the person who modified, and their comments on the change (like a cvs commit comment, why they made the change, what their source was, ect.).
* It holds the featureid of the feature changed (which is a foreign key to the tiger-an db).
* And it holds a location, or perhaps a bounding box, of the change. I suppose this could just follow the foreign key, but I think I may prefer the actual location. The nice part about this last
* thing is that it allows you to get the list of diffs on a zoomed in part of the map, that are the ones actually relevant for the part of the map you are looking at. I'm thinking something similar to
* subversion, where there is just a global revision number, for all changes, and those changes then affect certain features.
* So if you were super zoomed in, and hit the edit button, then the 'history' column for the page would just be the numbers of the revisions that affected that area, instead of the normal wiki way where all the
* numbers are present. As for implementation, to retrieve the 'history' column for any map you're zoomed in on, you use WFS.
* You would expose the diff table as WFS, and since it has locations of all the diffs, and feature ids (which in this case are the global revision numbers). To get the history for a given map, you would just do a WFS query on the bounding box (and indeed this makes it trivial to do things like 'give me all commits in this bounding box made by 'cholmes', and the like, as it's just a WFS query). And exposing the diff history as WFS would make it easier to implement in different clients, and indeed to maybe even use another WFS. We would just need to come up with a 'spatial wiki history' gml application schema, and specify how to fill the fields on each commit (or perhaps make a special 'wiki transaction' xml request? Though if the diff history was wfs-t then it could just be a second wfs insert transaction (Ah! Way too meta!)).

Interesting points in my opinion:

* single version table for full data
* commit bbox
* versioning a la svn (thought it seems to me hard to implement with decent performance)
* serving the diff table with wfs again

