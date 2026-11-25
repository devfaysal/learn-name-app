"""Verify the shipped bilingual collection without an Android device or network."""
from pathlib import Path
from collections import Counter
import argparse
import hashlib
import json
import xml.etree.ElementTree as ET
from zipfile import ZipFile
from PIL import Image
from generate_colors import COLORS

root=Path(__file__).resolve().parents[1]
assets=root/'app/src/main/assets'
data=json.loads((assets/'collection.json').read_text())
categories={c['id']:c for c in data['categories']}
entries=data['entries']
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--apk', type=Path, help='Also audit the assets inside a built APK.')
args=parser.parse_args()
expected_assets={'collection.json','IMAGE_CREDITS.txt','fonts/LICENSE.txt',
                 'fonts/bangla.ttf','fonts/bangla-bold.ttf'}
expected_assets.update(e['picture'] for e in entries)
expected_assets.update(f'audio/{language}/{e["id"]}.mp3' for language in ('bn','en') for e in entries)
actual_assets={p.relative_to(assets).as_posix() for p in assets.rglob('*') if p.is_file()}
assert actual_assets==expected_assets, f'Unexpected assets: {sorted(actual_assets-expected_assets)}; missing assets: {sorted(expected_assets-actual_assets)}'
if args.apk:
    with ZipFile(args.apk) as apk:
        packaged=[info.filename.removeprefix('assets/') for info in apk.infolist()
                  if info.filename.startswith('assets/') and not info.is_dir()]
        assert len(packaged)==len(set(packaged)), 'Duplicate APK asset paths'
        assert set(packaged)==expected_assets, f'APK has unexpected/missing assets: {sorted(set(packaged)^expected_assets)}'
        for name in packaged:
            assert apk.read('assets/'+name)==(assets/name).read_bytes(), f'Stale APK asset: {name}'
    print(f'PASS: APK contains exactly {len(expected_assets)} approved assets; {args.apk.stat().st_size / 1024**2:.2f} MiB total.')
assert len(categories)==10 and len(entries)==200
crop_entries={e['id']:e for e in entries if e['category']=='crops'}
assert len(crop_entries)==20
assert crop_entries['nigella']['bn']=='কালোজিরা'
assert crop_entries['coriander_seed']['bn']=='ধনে'
assert not {'oats','pearl_millet','bajra'} & set(crop_entries)
assert 'insects' in categories and 'body_parts' not in categories
assert 'spinach' not in {e['id'] for e in entries}
assert any(e['id']=='spine_gourd' and e['bn']=='কাঁকরোল' and e['category']=='vegetables' for e in entries)
assert {p.name for p in (assets/'pictures').glob('*.webp')}=={Path(e['picture']).name for e in entries}
counts=Counter(e['category'] for e in entries)
assert all(counts[c]==20 for c in categories)
assert len({e['id'] for e in entries})==len(entries)
# Orange is both a fruit and a color; names must be distinct within a category.
assert len({(e['category'],e['bn']) for e in entries})==len(entries)
assert [(e['id'],e['bn'],e['en']) for e in entries if e['category']=='colors']==[(key,bn,en) for key,bn,en,_ in COLORS]
sources=json.loads((root/'scripts/image_sources.json').read_text())
for language,voice,suffix,punctuation in [('bn','bn-BD-PradeepNeural','','।'),('en','en-GB-RyanNeural','_en','.')]:
    audio_sources=json.loads((root/f'scripts/audio_sources{suffix}.json').read_text())
    pronunciations=json.loads((root/f'scripts/audio_pronunciations{suffix}.json').read_text())
    assert set(audio_sources)=={e['id'] for e in entries}, f'Every entry needs a {language} recording'
    assert {p.stem for p in (assets/f'audio/{language}').glob('*.mp3')}==set(audio_sources)
    for entry in entries:
        source=audio_sources[entry['id']]
        assert source['file']==f'audio/{language}/{entry["id"]}.mp3'
        audio=assets/source['file']
        assert source['display_name']==entry[language], f'Stale pronunciation: {entry["id"]}'
        assert source['text']==pronunciations.get(entry['id'],entry[language])+punctuation, f'Stale spoken text: {entry["id"]}'
        assert source['voice']==voice
        assert audio.stat().st_size>1000 and 0.3<=source['duration_seconds']<=15
        assert hashlib.sha256(audio.read_bytes()).hexdigest()==source['sha256']
assert set(sources)=={e['id'] for e in entries}
hashes=set()
for e in entries:
    assert e['category'] in categories
    assert any('\u0980'<=c<='\u09ff' for c in e['bn']),e
    assert e['en'].strip() and e['en'].isascii(),e
    path=assets/e['picture']
    assert path.is_file() and '..' not in e['picture']
    with Image.open(path) as im:
        assert im.size==(480,480)
        im.verify()
    digest=hashlib.sha256(path.read_bytes()).hexdigest()
    assert digest not in hashes, f'Duplicate picture: {e["id"]}'
    hashes.add(digest)
    source=sources[e['id']]
    expected_type='procedural-color-swatch' if e['category']=='colors' else 'ai-generated-illustration'
    assert source['type']==expected_type,e['id']
    assert source['sha256']==digest,e['id']
    assert (root/source['master']).is_file(),e['id']
    metadata_key='metadata_file' if e['category']=='colors' else 'prompt_file'
    assert (root/source[metadata_key]).is_file(),e['id']
for c in categories.values():
    assert any(e['id']==c['cover'] and e['category']==c['id'] for e in entries)
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
assert not manifest.findall('uses-permission'), 'The app must not request permissions.'
assert (assets/'fonts/LICENSE.txt').is_file()
print('PASS: 10 categories, 200 bilingual names, 200 pictures, 400 male voice recordings (Bangla and English), licensed fonts, zero permissions.')
