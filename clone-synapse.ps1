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

# Remove existing and clone fresh with depth 1 and specific tag
if (Test-Path C:/chtor-server/chator/matrix) {
    Remove-Item C:/chtor-server/chator/matrix -Recurse -Force
}

Write-Host "Cloning synapse with depth 1..."
git clone --depth 1 --branch v1.151.0 https://github.com/element-hq/synapse.git C:/chtor-server/chator/matrix