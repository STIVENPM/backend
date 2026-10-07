param([string]$ApiBase = 'http://localhost:18081')

$ErrorActionPreference = 'Stop'
if (-not $env:LOCAL_TEST_ADMIN_EMAIL -or -not $env:LOCAL_TEST_ADMIN_PASSWORD) {
    throw 'Configure LOCAL_TEST_ADMIN_EMAIL y LOCAL_TEST_ADMIN_PASSWORD para una cuenta ADMIN de la base DESECHABLE.'
}

function Send-Request([string]$Method, [string]$Path, $Body, [string]$Token) {
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $args = @{ Method = $Method; Uri = "$ApiBase$Path"; Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $Body) {
        $args['ContentType'] = 'application/json'
        $args['Body'] = ($Body | ConvertTo-Json -Depth 6 -Compress)
    }
    try {
        $response = Invoke-WebRequest @args
        return @{ Status = [int]$response.StatusCode; Data = ($response.Content | ConvertFrom-Json) }
    } catch {
        if (-not $_.Exception.Response) { throw }
        $response = $_.Exception.Response
        return @{ Status = [int]$response.StatusCode; Data = $null }
    }
}

function Assert-Status($Response, [int]$Expected, [string]$Name) {
    if ($Response.Status -ne $Expected) { throw "$Name devolvió $($Response.Status); se esperaba $Expected" }
    Write-Output "PASS $Name ($Expected)"
}

$suffix = [Guid]::NewGuid().ToString('N').Substring(0, 10)
$email = "integration-$suffix@example.invalid"
$document = "98$((Get-Random -Minimum 10000000 -Maximum 99999999))"
$password = 'TemporaryLocalPass123!'
$registration = @{ email = $email; firstName = 'Local'; lastName = 'Test'; phoneNumber = "3$((Get-Random -Minimum 100000000 -Maximum 999999999))"; documentType = 'CC'; documentNumber = $document; password = $password; role = 'ADMIN' }

Assert-Status (Send-Request 'GET' '/actuator/health' $null $null) 200 'health'
$registered = Send-Request 'POST' '/api/users/register' $registration $null
Assert-Status $registered 201 'registro con rol ADMIN inyectado'
if (($registered.Data | ConvertTo-Json) -match 'password|documentNumber|token') { throw 'El registro expuso datos sensibles.' }

$userLogin = Send-Request 'POST' '/api/users/login' @{ email = $email; password = $password } $null
Assert-Status $userLogin 200 'login USER'
if ($userLogin.Data.user.role -ne 'USER') { throw 'La inyección del rol alteró el registro.' }
$userToken = $userLogin.Data.token
Assert-Status (Send-Request 'POST' '/api/operadores' @{ email = $email; documentNumber = $document; role = 'ADMIN' } $userToken) 403 'USER no asigna roles'

$adminLogin = Send-Request 'POST' '/api/users/login' @{ email = $env:LOCAL_TEST_ADMIN_EMAIL; password = $env:LOCAL_TEST_ADMIN_PASSWORD } $null
Assert-Status $adminLogin 200 'login ADMIN local desechable'
if ($adminLogin.Data.user.role -ne 'ADMIN') { throw 'La cuenta local no tiene ADMIN efectivo.' }
$adminToken = $adminLogin.Data.token

$operator = Send-Request 'POST' '/api/operadores' @{ email = $email; documentNumber = $document; role = 'ADMIN' } $adminToken
Assert-Status $operator 201 'ADMIN asigna OPERATOR ignorando rol inyectado'
$operatorId = $operator.Data.idOperador
$roleNow = Send-Request 'GET' '/api/users/me' $null $userToken
Assert-Status $roleNow 200 'sesión tras ascenso'
if ($roleNow.Data.role -ne 'OPERATOR') { throw 'El rol vigente no es OPERATOR.' }
Assert-Status (Send-Request 'POST' '/api/operadores' @{ email = $email; documentNumber = $document; role = 'ADMIN' } $userToken) 403 'OPERATOR no asigna roles'

Assert-Status (Send-Request 'PATCH' "/api/operadores/$operatorId/estado" @{ estado = $false } $adminToken) 200 'ADMIN revoca OPERATOR'
$roleAfter = Send-Request 'GET' '/api/users/me' $null $userToken
Assert-Status $roleAfter 200 'sesión tras revocación'
if ($roleAfter.Data.role -ne 'USER') { throw 'El rol revocado sigue vigente.' }
Assert-Status (Send-Request 'GET' '/api/asignaciones/mis-asignaciones' $null $userToken) 403 'token antiguo sin acceso OPERATOR'

Write-Output 'Integración HTTP de roles completada.'
