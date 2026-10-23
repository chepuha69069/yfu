param([string]$McJar = "")
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

function Fail($m) { Write-Host ""; Write-Host "ОШИБКА: $m" -ForegroundColor Red; exit 1 }
function Fwd($p) { return $p.Replace('\', '/') }

# ---------- 1. JDK ----------
function Find-Tool($name) {
    $cands = @()
    if ($env:JAVA_HOME) { $cands += (Join-Path $env:JAVA_HOME "bin\$name.exe") }
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($cmd) { $cands += $cmd.Source }
    foreach ($base in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft",
                        "$env:ProgramFiles\Zulu", "$env:ProgramFiles\BellSoft", "$env:ProgramFiles\Amazon Corretto",
                        "$env:USERPROFILE\.jdks", "$env:LOCALAPPDATA\Programs\Eclipse Adoptium")) {
        if (Test-Path $base) {
            $cands += (Get-ChildItem $base -Recurse -Filter "$name.exe" -ErrorAction SilentlyContinue |
                       Sort-Object FullName -Descending | ForEach-Object FullName)
        }
    }
    foreach ($c in $cands) { if ($c -and (Test-Path $c)) { return $c } }
    return $null
}
$javac = Find-Tool "javac"
$jar = Find-Tool "jar"
if (-not $javac -or -not $jar) {
    Fail "Не найден JDK (нужен javac). Установите JDK 21: https://adoptium.net/temurin/releases/?version=21 и запустите build.bat снова."
}
Write-Host "javac: $javac"

# ---------- 3. Minecraft (intermediary) ----------
$mcRoots = @("$env:APPDATA\.minecraft")
if (-not $McJar) {
    $local = Join-Path $root "mc_intermediary.jar"
    if (Test-Path $local) { $McJar = $local }
}
if (-not $McJar) {
    foreach ($r in $mcRoots) {
        $fab = Join-Path $r ".fabric\remappedJars"
        if (Test-Path $fab) {
            $f = Get-ChildItem $fab -Recurse -Filter "*intermediary*.jar" -ErrorAction SilentlyContinue |
                 Sort-Object LastWriteTime -Descending | Select-Object -First 1
            if ($f) { $McJar = $f.FullName; break }
        }
    }
}
if (-not $McJar) {
    Write-Host "Не нашёл intermediary-jar Minecraft автоматически."
    Write-Host "Он появляется после первого запуска игры с Fabric: <папка игры>\.fabric\remappedJars\...\intermediary-client.jar"
    $McJar = (Read-Host "Перетащите сюда этот файл (или введите путь)").Trim('"', ' ')
}
if (-not (Test-Path $McJar)) { Fail "Файл Minecraft jar не найден: $McJar" }
Write-Host "minecraft: $McJar"

# ---------- 4. библиотеки ----------
$cp = New-Object System.Collections.Generic.List[string]
$cp.Add($McJar)
foreach ($r in $mcRoots) {
    $lib = Join-Path $r "libraries"
    if (Test-Path $lib) {
        Get-ChildItem $lib -Recurse -Filter "*.jar" -ErrorAction SilentlyContinue | ForEach-Object { $cp.Add($_.FullName) }
    }
}
$mods = Join-Path $mcRoots[0] "mods"
if (Test-Path $mods) {
    Get-ChildItem $mods -Filter "fabric-api*.jar" -ErrorAction SilentlyContinue | ForEach-Object { $cp.Add($_.FullName) }
}

# ---------- 5. сборка ----------
$work = Join-Path $root "build"
if (Test-Path $work) { Remove-Item $work -Recurse -Force }
$stubOut = Join-Path $work "stubs"; $out = Join-Path $work "classes"
New-Item -ItemType Directory -Force $stubOut, $out | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)

# заглушки Fabric API (только для компиляции, в jar не попадают)
$stubFiles = Get-ChildItem (Join-Path $root "stubs") -Recurse -Filter "*.java" | ForEach-Object { '"' + (Fwd $_.FullName) + '"' }
[IO.File]::WriteAllText("$work\stubs.args", ("-encoding UTF-8`n-d `"" + (Fwd $stubOut) + "`"`n" + ($stubFiles -join "`n")), $utf8)
& $javac -cp (Fwd $McJar) "@$work\stubs.args"
if ($LASTEXITCODE -ne 0) { Fail "Не скомпилировались заглушки." }

$srcFiles = Get-ChildItem (Join-Path $root "src") -Recurse -Filter "*.java" | ForEach-Object { '"' + (Fwd $_.FullName) + '"' }
$cpText = (($cp | ForEach-Object { Fwd $_ }) -join ";") + ";" + (Fwd $stubOut)
[IO.File]::WriteAllText("$work\src.args",
    ("-encoding UTF-8`n--release 21`n-proc:none`n-nowarn`n-d `"" + (Fwd $out) + "`"`n-cp `"" + $cpText + "`"`n" + ($srcFiles -join "`n")), $utf8)
Write-Host "Компиляция..."
& $javac "@$work\src.args"
if ($LASTEXITCODE -ne 0) { Fail "Ошибка компиляции (см. выше). Пришлите текст ошибки." }

# ---------- 6. упаковка ----------
$result = Join-Path $root "applefarm-1.0.0.jar"
if (Test-Path $result) { Remove-Item $result -Force }
& $jar --create --file $result -C $out . -C (Join-Path $root "resources") .
if ($LASTEXITCODE -ne 0) { Fail "Не удалось создать jar." }

Write-Host ""
Write-Host "ГОТОВО: $result" -ForegroundColor Green
Write-Host "Положите его в папку mods (нужен Fabric API). Releon Client для работы не нужен."
