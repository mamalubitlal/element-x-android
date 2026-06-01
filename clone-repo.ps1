$env:CI = 'true'
$env:DEBIAN_FRONTEND = 'noninteractive'
$env:GIT_TERMINAL_PROMPT = '0'
$env:GCM_INTERACTIVE = 'never'
$env:HOMEBREW_NO_AUTO_UPDATE = '1'
$env:GIT_EDITOR = ':'
$env:EDITOR = ':'
$env:VISUAL = ''
$env:GIT_SEQUENCE_EDITOR = ':'
$env:GIT_MERGE_AUTOEDIT = 'no'
$env:GIT_PAGER = 'cat'
$env:PAGER = 'cat'
$env:npm_config_yes = 'true'
$env:PIP_NO_INPUT = '1'
$env:YARN_ENABLE_IMMUTABLE_INSTALLS = 'false'

if (Test-Path C:/chtor-server/chator/matrix/.git) {
    Write-Host "Repo exists, fetching..."
    git -C C:/chtor-server/chator/matrix fetch --all
} else {
    Write-Host "Cloning fresh..."
    git clone --depth 1 https://github.com/element-hq/synapse.git C:/chtor-server/chator/matrix
}