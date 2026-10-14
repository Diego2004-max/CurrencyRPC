$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$javaExecutable = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    Join-Path $env:JAVA_HOME 'bin/java.exe'
} else {
    throw 'Set JAVA_HOME to your JDK 21 directory before running this verification script.'
}
New-Item -ItemType Directory -Force docs/evidencias | Out-Null
$evidence = Join-Path (Get-Location) 'docs/evidencias'
$testLog = Join-Path $evidence 'tests.txt'
$localLog = Join-Path $evidence 'ejecucion-local.txt'
$stamp = [TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTime]::UtcNow, 'SA Pacific Standard Time').ToString('yyyy-MM-dd HH:mm:ss')
"Verification: $stamp America/Bogota`nCOMMAND: mvn test" | Set-Content $testLog -Encoding UTF8
& mvn -B test 2>&1 | Out-File $testLog -Append -Encoding UTF8
if ($LASTEXITCODE -ne 0) { throw "mvn test failed: $testLog" }
"Verification: $stamp America/Bogota`nCOMMAND: mvn clean package" | Set-Content $localLog -Encoding UTF8
& mvn -B clean package 2>&1 | Out-File $localLog -Append -Encoding UTF8
if ($LASTEXITCODE -ne 0) { throw "mvn clean package failed: $localLog" }

$server = $null
$savedEnv = @{}
foreach ($name in @('PORT', 'GRPC_HOST', 'GRPC_PORT', 'GRPC_TLS')) {
    $savedEnv[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
try {
    if (Get-NetTCPConnection -LocalPort 9090 -State Listen -ErrorAction SilentlyContinue) {
        throw 'Port 9090 is already occupied. Stop your existing server before verifying.'
    }
    $env:PORT = '9090'
    $env:GRPC_HOST = 'localhost'
    $env:GRPC_PORT = '9090'
    $env:GRPC_TLS = 'false'
    $stdout = Join-Path (Get-Location) 'target/server-stdout.log'
    $stderr = Join-Path (Get-Location) 'target/server-stderr.log'
    $server = Start-Process -FilePath $javaExecutable -ArgumentList '-jar', 'target/currency-rpc.jar' -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    $ready = $false
    for ($attempt = 0; $attempt -lt 100; $attempt++) {
        if ($server.HasExited) { throw 'Server exited before startup.' }
        if ((Test-Path $stdout) -and (Select-String -Path $stdout -Pattern 'CurrencyRPC listening' -Quiet)) {
            $ready = $true
            break
        }
        Start-Sleep -Milliseconds 100
    }
    if (!$ready) { throw 'Server did not start within 10 seconds.' }
    "`nCOMMAND: PORT=9090 java -jar target/currency-rpc.jar" | Out-File $localLog -Append -Encoding UTF8
    Get-Content $stdout | Out-File $localLog -Append -Encoding UTF8
    "`nENV: GRPC_HOST=localhost GRPC_PORT=9090 GRPC_TLS=false`nCOMMAND: java -cp target/currency-rpc.jar com.currencyrpc.client.CurrencyClient" | Out-File $localLog -Append -Encoding UTF8
    $clientOutput = & java -cp target/currency-rpc.jar com.currencyrpc.client.CurrencyClient 2>&1
    $clientExit = $LASTEXITCODE
    $clientOutput | Out-File $localLog -Append -Encoding UTF8
    "Client exit code: $clientExit" | Out-File $localLog -Append -Encoding UTF8
    if ($clientExit -ne 0) { throw 'Client demo failed.' }
    $outputText = $clientOutput -join "`n"
    foreach ($expected in @('100 USD -> 400000.00 COP', '500000 COP -> 125.00 USD', '[USD, COP, EUR, GBP, JPY, MXN]', 'Validation 0 USD -> COP | INVALID_ARGUMENT', 'Validation 100 XYZ -> COP | INVALID_ARGUMENT')) {
        if (!$outputText.Contains($expected)) { throw "Missing expected output: $expected" }
    }
    if ([regex]::Matches($outputText, '(?m)^Update [1-5] \|').Count -ne 5) { throw 'Expected five stream updates.' }
    Get-Content $stderr | Out-File $localLog -Append -Encoding UTF8
    Write-Output 'VERIFIED: tests, package, separate server/client, conversions, validation and five updates.'
    Select-String -Path $testLog,$localLog -Pattern 'Tests run: 29','BUILD SUCCESS','Validation ','Update [1-5]','Client exit code'
} finally {
    if ($null -ne $server -and !$server.HasExited) {
        Stop-Process -Id $server.Id
        $server.WaitForExit()
    }
    foreach ($name in $savedEnv.Keys) {
        [Environment]::SetEnvironmentVariable($name, $savedEnv[$name], 'Process')
    }
}
