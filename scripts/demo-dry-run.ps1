param(
    [string]$BaseUrl = "http://192.168.40.1:8080",
    [string]$Email = "",
    [string]$Password = "DemoPass123!"
)

$ErrorActionPreference = "Stop"
$utf8 = New-Object System.Text.UTF8Encoding($false)
if ([string]::IsNullOrWhiteSpace($Email)) {
    $Email = "dry-run-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())@example.com"
}

function ConvertTo-Utf8Json([object]$Value) {
    $json = $Value | ConvertTo-Json -Depth 20 -Compress
    return ,$utf8.GetBytes($json)
}

function ConvertFrom-Utf8Base64([string]$Value) {
    return [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($Value))
}

function Repair-Utf8Text([string]$Value) {
    if ([string]::IsNullOrEmpty($Value)) { return $Value }
    return [Text.Encoding]::UTF8.GetString([Text.Encoding]::GetEncoding(28591).GetBytes($Value))
}

function Write-Step([int]$Current, [int]$Total, [string]$Name) {
    Write-Output "[$Current/$Total] $Name"
}

function Invoke-Get([string]$Path, $Session) {
    return Invoke-RestMethod -Uri "$BaseUrl$Path" -WebSession $Session -TimeoutSec 90
}

function Invoke-Post([string]$Path, [object]$Body, $Session, $Headers) {
    return Invoke-RestMethod -Method Post -Uri "$BaseUrl$Path" -WebSession $Session `
        -Headers $Headers -ContentType "application/json; charset=utf-8" `
        -Body (ConvertTo-Utf8Json $Body) -TimeoutSec 180
}

$placeQuery = ConvertFrom-Utf8Base64 "67aA7IKwIO2VtOyatOuMgA=="
$tourismKeyword = ConvertFrom-Utf8Base64 "7ZW07Jq064yA"
$mappingPrompt = ConvertFrom-Utf8Base64 "67aA7IKwIOq0keyViOumrOyXkOyEnCDrsJTri6Trpbwg67O06rOgIOq3vOyymCDrp5vsp5Hrj4Qg7LC+6rOgIOyLtuyWtOyalC4="
$chatPrompt = ConvertFrom-Utf8Base64 "7KeA6riIIOychOy5mOyXkOyEnCDtlbTsmrTrjIAg67CY64KY7KCIIOyXrO2WieydhCDstpTsspztlbTspJg="
$total = 14
$results = [ordered]@{}

Write-Step 1 $total "health"
$health = Invoke-RestMethod -Uri "$BaseUrl/api/system/health" -TimeoutSec 15
if ($health.status -ne "UP") { throw "Backend health is not UP." }
$results.health = $health.status

Write-Step 2 $total "signup"
$signupSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$signupCsrf = Invoke-RestMethod -Uri "$BaseUrl/api/auth/csrf" -WebSession $signupSession
$signupHeaders = @{ $signupCsrf.headerName = $signupCsrf.token }
Invoke-Post "/api/auth/signup" @{
    email = $Email
    password = $Password
    displayName = "ROAMATE dry-run"
} $signupSession $signupHeaders | Out-Null
$results.signup = "CREATED"

Write-Step 3 $total "login with a fresh session"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = Invoke-RestMethod -Uri "$BaseUrl/api/auth/csrf" -WebSession $session
$headers = @{ $csrf.headerName = $csrf.token }
$login = Invoke-Post "/api/auth/login" @{ email = $Email; password = $Password } $session $headers
$results.login = $login.email

Write-Step 4 $total "current user"
$me = Invoke-Get "/api/auth/me" $session
$results.me = $me.displayName

Write-Step 5 $total "Naver place search"
$places = Invoke-Get "/api/places/search?query=$([uri]::EscapeDataString($placeQuery))" $session
$results.places = @($places).Count

Write-Step 6 $total "tourism public data"
$tourism = Invoke-Get "/api/tourism/related/search?baseYm=202504&signguCd=26350&keyword=$([uri]::EscapeDataString($tourismKeyword))&pageNo=1&numOfRows=15" $session
$results.tourism = @($tourism.places).Count

Write-Step 7 $total "current-location weather"
$weather = Invoke-Get "/api/weather/current?latitude=35.1796&longitude=129.0756" $session
$results.weather = "$($weather.temperature)C / $(Repair-Utf8Text $weather.skyCondition)"

$routeQuery = "startLng=129.0756&startLat=35.1796&endLng=129.1603&endLat=35.1587"
Write-Step 8 $total "car route"
$car = Invoke-Get "/api/routes?mode=CAR&$routeQuery" $session
$results.car = "$($car.summary.distanceMeters)m / $($car.summary.durationSeconds)s"

Write-Step 9 $total "transit routes"
$transit = Invoke-Get "/api/routes?mode=TRANSIT&$routeQuery" $session
$results.transit = @($transit).Count

Write-Step 10 $total "walk route"
$walk = Invoke-Get "/api/routes?mode=WALK&$routeQuery" $session
$results.walk = "$($walk.summary.distanceMeters)m / $($walk.summary.durationSeconds)s"

$aiRequest = @{ message = $mappingPrompt }
Write-Step 11 $total "Spring AI mapping 1: search condition"
$condition = Invoke-Post "/api/ai/search-condition" $aiRequest $session $headers
$results.springAiSearchCondition = $condition.intent

Write-Step 12 $total "Spring AI mapping 2: travel brief"
$brief = Invoke-Post "/api/ai/travel-brief" $aiRequest $session $headers
$results.springAiTravelBrief = Repair-Utf8Text $brief.title

Write-Step 13 $total "context-aware AI chat"
$chat = Invoke-Post "/api/ai/chat" @{
    message = $chatPrompt
    history = @()
    context = @{ currentLocation = @{ latitude = 35.1796; longitude = 129.0756 } }
} $session $headers
$results.aiChat = Repair-Utf8Text $chat.message

Write-Step 14 $total "summary"
$results.status = "PASS"
$results | ConvertTo-Json -Depth 10
