# BiliMusic 一键构建脚本（Windows PowerShell）
# 用法: .\build.ps1            -> Debug 构建
#       .\build.ps1 release    -> Release 构建（不签名）
#       .\build.ps1 clean      -> 清理
#       .\build.ps1 wrapper    -> 生成 Gradle Wrapper

$env:JAVA_HOME = "D:\Android\Android Studio\jbr"
$env:GRADLE_USER_HOME = "D:\Android\AndroidGradle"

$gradle = "D:\Android\AndroidGradle\wrapper\dists\gradle-9.4.1-bin\arn2x92ynaizyzdaamcbpbhtj\gradle-9.4.1\bin\gradle.bat"

if (-not (Test-Path $gradle)) {
    Write-Host "ERROR: 本地 Gradle 9.4.1 未找到: $gradle" -ForegroundColor Red
    exit 1
}

$target = if ($args.Count -gt 0) { $args[0] } else { "assembleDebug" }

switch ($target) {
    "clean"   { & $gradle --no-daemon clean }
    "wrapper" { & $gradle --no-daemon wrapper --gradle-version 9.4.1 }
    "release" { & $gradle --no-daemon assembleRelease }
    default   { & $gradle --no-daemon assembleDebug }
}
exit $LASTEXITCODE
