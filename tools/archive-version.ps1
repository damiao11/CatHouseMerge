[CmdletBinding()]
param([switch]$AllowDirty, [switch]$Offline)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
Push-Location -LiteralPath $root
try {
    $commit = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw '无法确定源码提交。' }
    $dirty = @(& git status --porcelain)
    if ($dirty.Count -gt 0 -and -not $AllowDirty) {
        throw '工作区有未提交内容。请检查并提交后再归档；仅保存本地预览时可显式使用 -AllowDirty。'
    }
    $config = Get-Content -LiteralPath (Join-Path $root 'app/build.gradle.kts') -Raw
    $version = [regex]::Match($config, 'versionName\s*=\s*"(\d+\.\d+\.\d+)"').Groups[1].Value
    $versionCode = [regex]::Match($config, 'versionCode\s*=\s*(\d+)').Groups[1].Value
    if (-not $version -or -not $versionCode) { throw '无法读取应用版本号。' }
    $suffix = if ($dirty.Count -gt 0) { '-preview-' + (Get-Date -Format 'yyyyMMdd-HHmmss') } else { '' }
    $output = Join-Path $root "artifacts/versions/v$version$suffix"
    if (Test-Path -LiteralPath $output) { throw "归档目录已存在，不覆盖：$output" }

    # Avoid Windows JDK Unix-domain sockets using an inaccessible/overlong default temp path.
    # This setting belongs only to this script process, not the machine or repository configuration.
    if ([System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT -and $env:JAVA_TOOL_OPTIONS -notmatch 'jdk.net.unixdomain.tmpdir') {
        $socketTemp = (Join-Path $root 'build/jtmp').Replace('\', '/')
        New-Item -ItemType Directory -Force -Path $socketTemp | Out-Null
        $env:JAVA_TOOL_OPTIONS = ($env:JAVA_TOOL_OPTIONS + ' -Djdk.net.unixdomain.tmpdir="' + $socketTemp + '"').Trim()
    }
    $buildArgs = @(':core:test', ':app:assembleDebug', '--console=plain', '--no-daemon')
    if ($Offline) { $buildArgs += '--offline' }
    & (Join-Path $root 'gradlew.bat') @buildArgs
    if ($LASTEXITCODE -ne 0) { throw '测试或构建失败，没有创建版本归档。' }
    if ((& git rev-parse HEAD).Trim() -ne $commit) { throw '构建期间源码提交发生变化，请重新归档。' }
    $after = @(& git status --porcelain)
    if (@(Compare-Object -ReferenceObject @('' + ($dirty -join "`n")) -DifferenceObject @('' + ($after -join "`n"))).Count -ne 0) {
        throw '构建期间工作区状态发生变化，请检查后重新归档。'
    }
    New-Item -ItemType Directory -Path $output | Out-Null
    $apk = Join-Path $output "CatHouseMerge-v$version$suffix.apk"
    Copy-Item -LiteralPath (Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk') -Destination $apk
    # A Git archive always identifies the exact committed sources, even when unrelated local edits remain.
    $sourceZip = Join-Path $output "CatHouseMerge-v$version$suffix-source-committed.zip"
    & git archive --format=zip --output $sourceZip $commit
    if ($LASTEXITCODE -ne 0) { throw '源码 ZIP 导出失败，请保留已生成的 APK 并检查归档目录。' }
    $dirtyPatch = $null
    if ($dirty.Count -gt 0) {
        $dirtyPatch = 'working-tree-tracked-changes.patch'
        & git diff --no-ext-diff --no-textconv --binary --output (Join-Path $output $dirtyPatch) $commit
        if ($LASTEXITCODE -ne 0) { throw '工作区差异留档失败，请检查归档目录。' }
    }
    $metadata = [ordered]@{
        version = $version; versionCode = [int]$versionCode; sourceCommit = $commit
        sourceDirty = ($dirty.Count -gt 0); status = $(if ($dirty.Count -gt 0) { 'preview-not-a-release' } else { 'local-verified-build-not-published' })
        recordedAt = [DateTimeOffset]::UtcNow.ToString('o'); sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $apk).Hash.ToLowerInvariant()
        sourceArchive = (Split-Path -Leaf $sourceZip); sourceArchiveSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourceZip).Hash.ToLowerInvariant()
        sourceArchiveKind = 'committed-files-at-sourceCommit'; workingTreeStatus = @($dirty); trackedChangesPatch = $dirtyPatch
        untrackedFilesNote = '未跟踪文件不包含在 Git 源码 ZIP 或 tracked changes patch 中。需要时另行保留。'
        apk = (Split-Path -Leaf $apk); tested = @(':core:test', ':app:assembleDebug')
        note = $(if ($dirty.Count -gt 0) { '工作区有未提交内容，APK 按预览保守标记；源码 ZIP 严格对应 sourceCommit，已跟踪差异另存 patch，不得当作正式发布。' } else { '尚未自动创建 Git 标签或 GitHub Release。' })
    }
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText((Join-Path $output 'metadata.json'), (($metadata | ConvertTo-Json -Depth 4) + "`n"), $utf8)
    Copy-Item -LiteralPath (Join-Path $root 'CHANGELOG.md') -Destination (Join-Path $output 'CHANGELOG.md')
    Write-Host "版本已留档：$output"
    Write-Host "SHA-256：$($metadata.sha256)"
    Write-Host '没有执行提交、打标签或推送。'
}
finally { $env:JAVA_TOOL_OPTIONS = $previousJavaOptions; Pop-Location }
