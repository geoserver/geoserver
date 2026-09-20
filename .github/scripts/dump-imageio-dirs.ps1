# Prints every file in the imageio-ext artifact directories with its timestamp and
# hash, plus the _remote.repositories entries naming the repository each came from.
param([Parameter(Mandatory=$true)][string]$Label)

Write-Host "===== imageio-ext local repository, $Label ====="
Get-ChildItem -Directory -Recurse -Path "$env:USERPROFILE\.m2\repository\it\geosolutions\imageio-ext" -ErrorAction SilentlyContinue |
  Where-Object { Get-ChildItem -File -Path $_.FullName -ErrorAction SilentlyContinue } |
  ForEach-Object {
    Write-Host "--- $($_.FullName)"
    Get-ChildItem -File -Path $_.FullName | ForEach-Object {
      $sha = if ($_.Extension -eq ".jar") { (Get-FileHash -Algorithm SHA1 -LiteralPath $_.FullName).Hash } else { "" }
      "{0,-12} {1} {2} {3}" -f $_.Length, $_.LastWriteTimeUtc.ToString("o"), $sha, $_.Name
    }
    $remote = Join-Path $_.FullName "_remote.repositories"
    if (Test-Path $remote) { Get-Content $remote | Where-Object { $_ -notmatch '^#' -and $_.Trim() } }
  }
