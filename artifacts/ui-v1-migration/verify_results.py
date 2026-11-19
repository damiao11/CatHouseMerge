from pathlib import Path
from datetime import datetime, timezone, timedelta
import hashlib, json, re, subprocess, xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
folder = root / 'artifacts/ui-v1-migration'
log = (folder / 'device-tests-final.log').read_text(encoding='utf-8-sig', errors='replace')
build = (folder / 'final-build.log').read_text(encoding='utf-8-sig', errors='replace')
match = re.search(r'OK \((\d+) tests?\)', log)
assert match and not any(word in log for word in ['FAILURES!!!', 'INSTRUMENTATION_FAILED', 'Process crashed']), 'Device test run has not passed'
assert 'BUILD SUCCESSFUL' in build, 'Build has not passed'
core = []
for file in sorted((root / 'core/build/test-results/test').glob('TEST-*.xml')):
    data = ET.parse(file).getroot().attrib
    core.append({'suite': data['name'], 'tests': int(data['tests']), 'failures': int(data['failures']), 'errors': int(data.get('errors', '0'))})
assert core and sum(x['failures'] + x['errors'] for x in core) == 0
apk = root / 'app/build/outputs/apk/debug/app-debug.apk'
assert (folder / 'device-tests-final.log').stat().st_mtime >= apk.stat().st_mtime, 'Device log predates this APK; rerun instrumentation'
metadata = json.loads((apk.parent / 'output-metadata.json').read_text(encoding='utf-8'))['elements'][0]
def sha(file): return hashlib.sha256(file.read_bytes()).hexdigest().upper()
source_files = [root / 'app/build.gradle.kts', root / 'app/src/main/res/values/styles.xml']
source_files += list((root / 'app/src/main/java/com/example/cathousemerge/ui').glob('*.kt'))
source_files += [root / 'app/src/main/java/com/example/cathousemerge/GameViewModel.kt', root / 'app/src/main/java/com/example/cathousemerge/MainActivity.kt']
source_files += list((root / 'app/src/androidTest/java/com/example/cathousemerge').glob('*.kt'))
core_untouched = subprocess.run(['git', 'diff', '--quiet', '--', 'core', 'app/src/main/java/com/example/cathousemerge/data'], cwd=root).returncode == 0
white_check = subprocess.run(['git', 'diff', '--check', '--', 'app'], cwd=root, capture_output=True, text=True)
assert white_check.returncode == 0, white_check.stdout + white_check.stderr
record = {
    'verified_at': datetime.now(timezone(timedelta(hours=8))).isoformat(),
    'design_source': 'UI-v1 (approved local prototype)',
    'design_source_sha256': {name: sha(root / 'UI-v1' / name) for name in ['index.html', 'prototype.css', 'prototype.js']},
    'apk': {'path': str(apk), 'sha256': sha(apk), 'bytes': apk.stat().st_size, 'version_name': metadata['versionName'], 'version_code': metadata['versionCode'], 'type': 'debug, not a release'},
    'core_tests': {'tests': sum(x['tests'] for x in core), 'failures': 0, 'run': 'offline, --rerun-tasks', 'suites': core},
    'device_tests': {'tests': int(match.group(1)), 'failures': 0, 'log': 'device-tests-final.log', 'runner': 'AndroidJUnitRunner', 'serial': 'emulator-5554', 'android': '16 / API 36', 'display': '1080x2400, density 420', 'modes': ['360x780 normal', '320x640 1.3x font', '320x640 1.4x font', '320x568 1.4x font']},
    'screenshots': {'directory': 'screenshots', 'provenance': 'Actual Jetpack Compose instrumentation captures, not browser or AI mockups. Screens use independent test fixture states; system bars are not part of component captures.'},
    'order_cat_artwork': {'source': 'UI-v1/source-mastergo.html (user-provided SVGs)', 'conversion': 'SVG to transparent PNG, without AI image generation'},
    'preserved': {'core_and_save_repository_unchanged': core_untouched, 'save_format_changed': False, 'documentation_merged': False, 'committed_or_published': False, 'whitespace_check_passed': True},
    'source_sha256': {p.relative_to(root).as_posix(): sha(p) for p in source_files},
}
(folder / 'verification.json').write_bytes((json.dumps(record, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))
print(json.dumps({'core_tests': record['core_tests']['tests'], 'device_tests': record['device_tests']['tests'], 'apk_sha256': record['apk']['sha256'], 'core_and_saves_unchanged': core_untouched}, ensure_ascii=False))
