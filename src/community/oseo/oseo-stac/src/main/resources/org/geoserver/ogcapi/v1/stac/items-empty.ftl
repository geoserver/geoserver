    <#-- 
    Body section of the STAC items template. Version used when there is no data in the collection.
    -->
    <#if collection??>
      <h2><a href="${serviceLink("/collections/${collection}")}">${collection}</a></h2>
    <#else>
      <h2>${collection}</h2>
    </#if>
    <p>No data could be found with the current request parameters</p>

