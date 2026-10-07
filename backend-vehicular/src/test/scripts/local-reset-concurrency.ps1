param(
    [string]$ApiBase = 'http://localhost:18082',
    [string]$MailpitBase = 'http://localhost:18025',
    [Parameter(Mandatory = $true)][string]$Email
)

$ErrorActionPreference = 'Stop'
foreach ($uri in @($ApiBase, $MailpitBase)) {
    $parsed = [uri]$uri
    if ($parsed.Scheme -ne 'http' -or $parsed.Host -notin @('localhost', '127.0.0.1')) {
        throw 'Esta prueba solo admite servicios HTTP locales y desechables.'
    }
}

$before = Invoke-RestMethod "$MailpitBase/api/v1/messages?limit=1"
$previousId = if ($before.messages.Count) { $before.messages[0].ID } else { '' }
$forgot = Invoke-WebRequest "$ApiBase/api/auth/forgot-password" -Method Post -ContentType 'application/json' -Body (@{ email = $Email } | ConvertTo-Json -Compress) -UseBasicParsing
if ($forgot.StatusCode -ne 200) { throw "Solicitud de recuperación: $($forgot.StatusCode)" }

$message = $null
for ($attempt = 0; $attempt -lt 20; $attempt++) {
    $list = Invoke-RestMethod "$MailpitBase/api/v1/messages?limit=1"
    if ($list.messages.Count -and $list.messages[0].ID -ne $previousId) {
        $message = Invoke-RestMethod "$MailpitBase/api/v1/message/$($list.messages[0].ID)"
        break
    }
    Start-Sleep -Milliseconds 300
}
if ($null -eq $message) { throw 'Mailpit no recibió un mensaje nuevo.' }
$match = [regex]::Match($message.Text, '\?token=([A-Za-z0-9_-]+)')
if (-not $match.Success) { throw 'El correo no contiene un token de recuperación.' }

$newPassword = 'LocalReset-' + [guid]::NewGuid().ToString('N')
$body = @{ token = $match.Groups[1].Value; nuevaContrasena = $newPassword } | ConvertTo-Json -Compress
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
try {
    $url = "$ApiBase/api/auth/reset-password"
    $first = $client.PostAsync($url, [System.Net.Http.StringContent]::new($body, [text.encoding]::UTF8, 'application/json'))
    $second = $client.PostAsync($url, [System.Net.Http.StringContent]::new($body, [text.encoding]::UTF8, 'application/json'))
    [System.Threading.Tasks.Task]::WaitAll(@($first, $second))
    $codes = @([int]$first.Result.StatusCode, [int]$second.Result.StatusCode) | Sort-Object
    if ($codes[0] -ne 200 -or $codes[1] -ne 400) {
        throw "Se esperaban 200 y 400; se recibieron $($codes -join ', ')."
    }
    Write-Output 'OK: dos resets concurrentes del mismo token devolvieron 200 y 400.'
} finally {
    $client.Dispose()
}
