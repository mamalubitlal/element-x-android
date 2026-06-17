import sys

# Patch the mjs source (before webpack bundles it)
filepath = r'C:\chtor\compose-web\build\compileSync\wasmJs\main\developmentExecutable\kotlin\chator-web-wasm-js.mjs'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

if '}, false)).exports' in content:
    print('Already patched')
    sys.exit(0)

# The source mjs has actual newlines (not \n), so we can use real strings
old_instantiate = "instantiate({\n    './skiko.mjs': _skiko_mjs__WEBPACK_IMPORTED_MODULE_0__,\n    '@js-joda/core': _js_joda_core__WEBPACK_IMPORTED_MODULE_1__\n})).exports"
new_instantiate = "instantiate({\n    './skiko.mjs': _skiko_mjs__WEBPACK_IMPORTED_MODULE_0__,\n    '@js-joda/core': _js_joda_core__WEBPACK_IMPORTED_MODULE_1__\n}, false)).exports"

if old_instantiate in content:
    content = content.replace(old_instantiate, new_instantiate)
    # Also add setTimeout after __webpack_async_result__()
    content = content.replace(
        "__webpack_async_result__();",
        "__webpack_async_result__();\nsetTimeout(() => { _initialize(); }, 0);"
    )
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print('SUCCESS: Patched source mjs')
else:
    print('FAILED: Pattern not found in mjs')
    idx = content.find('instantiate')
    if idx >= 0:
        print('Context:', repr(content[idx:idx+350]))
    sys.exit(1)
