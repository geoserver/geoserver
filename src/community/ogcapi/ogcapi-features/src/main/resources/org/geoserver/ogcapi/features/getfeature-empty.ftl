    <#-- 
    Body section of the GetFeature template, it's provided with one feature collection, and
    will be called multiple times if there are various feature collections. Version used
    when there is no data in the collection.
    -->
    <#if collection??>
      <h2><a href="${serviceLink("/collections/${collection}")}">${collection}</a></h2>
    <#else>
      <h2>${collection}</h2>
    </#if>
    <p>No data could be found with the current request parameters</p>

