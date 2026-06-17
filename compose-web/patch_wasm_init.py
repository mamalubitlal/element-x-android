import sys

filepath = r'C:\chtor\compose-web\build\dist\wasmJs\developmentExecutable\chator-web.js'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# The eval string uses literal \n (backslash-n) not actual newlines
old = ("instantiate)({\\n    './skiko.mjs': _skiko_mjs__WEBPACK_IMPORTED_MODULE_0__,\\n"
       "    '@js-joda/core': _js_joda_core__WEBPACK_IMPORTED_MODULE_1__\\n"
       "})).exports;\\n\\nconst {\\n    memory,\\n    _initialize,\\n    startUnitTests\\n} = exports;\\n\\n\\n__webpack_async_result__();")

new = ("instantiate)({\\n    './skiko.mjs': _skiko_mjs__WEBPACK_IMPORTED_MODULE_0__,\\n"
       "    '@js-joda/core': _js_joda_core__WEBPACK_IMPORTED_MODULE_1__\\n"
       "}, false)).exports;\\n\\nconst {\\n    memory,\\n    _initialize,\\n    startUnitTests\\n} = exports;\\n\\n\\n__webpack_async_result__();\\n"
       "setTimeout(() => { _initialize(); }, 0);")

if old in content:
    content = content.replace(old, new)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print('SUCCESS: Patched chator-web.js to defer _initialize')
else:
    print('FAILED: Pattern not found')
    idx = content.find('instantiate)')
    if idx >= 0:
        print('Context:', repr(content[idx:idx+400]))
    sys.exit(1)
