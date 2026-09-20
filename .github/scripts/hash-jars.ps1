# Records size, modification time and SHA1 of the snapshot jars in the local Maven
# repository, so a run can tell whether one was replaced while the tests were running.
param([Parameter(Mandatory=$true)][string]$Output)

$roots = "it\geosolutions", "org\geotools" | ForEach-Object { "$env:USERPROFILE\.m2\repository\$_" }

Get-ChildItem -Recurse -File -Filter *.jar -Path $roots -ErrorAction SilentlyContinue |
  ForEach-Object {
    [pscustomobject]@{
      Path     = $_.FullName
      Length   = $_.Length
      Modified = $_.LastWriteTimeUtc.ToString("o")
      Sha1     = (Get-FileHash -Algorithm SHA1 -LiteralPath $_.FullName).Hash
    }
  } | Sort-Object Path | Export-Csv -NoTypeInformation -Path $Output
