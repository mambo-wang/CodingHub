Set-Location D:\repos\CodingHub
$env:Path = 'C:\Users\Administrator\AppData\Roaming\npm;' + $env:Path
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=== status ==="
openspec status --change 'add-post-delete' 2>&1
Write-Host ""
Write-Host "=== instructions apply ==="
openspec instructions apply --change 'add-post-delete' --json 2>&1
