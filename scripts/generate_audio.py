"""Generate/resume bundled pronunciation clips (requires edge-tts, mutagen).

Run with --only ID to regenerate one pronunciation after changing its override.
Network is used only by this development script; the Android app stays offline.
"""
import argparse
import asyncio
import hashlib
import json
from pathlib import Path

import edge_tts
from mutagen.mp3 import MP3

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
VOICES = {'bn': 'bn-BD-PradeepNeural', 'en': 'en-GB-RyanNeural'}
RATE = '-5%'


async def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--language', choices=VOICES, default='bn')
    parser.add_argument('--only', help='Regenerate just this entry ID')
    args = parser.parse_args()
    language = args.language
    voice = VOICES[language]
    output = ASSETS / 'audio' / language
    suffix = '_en' if language == 'en' else ''
    manifest = ROOT / f'scripts/audio_sources{suffix}.json'
    overrides_path = ROOT / f'scripts/audio_pronunciations{suffix}.json'
    entries = json.loads((ASSETS / 'collection.json').read_text())['entries']
    if args.only and args.only not in {entry['id'] for entry in entries}:
        parser.error('Unknown entry ID')
    overrides = json.loads(overrides_path.read_text())
    sources = json.loads(manifest.read_text()) if manifest.exists() else {}
    output.mkdir(parents=True, exist_ok=True)
    semaphore = asyncio.Semaphore(3)

    def save_manifest():
        temporary = manifest.with_suffix('.tmp')
        temporary.write_text(json.dumps(sources, ensure_ascii=False, indent=2) + '\n')
        temporary.replace(manifest)

    async def generate(entry):
        entry_id = entry['id']
        if args.only and entry_id != args.only:
            return
        text = overrides.get(entry_id, entry[language]) + ('।' if language == 'bn' else '.')
        path = output / f'{entry_id}.mp3'
        spec = dict(voice=voice, rate=RATE, text=text, display_name=entry[language])
        old = sources.get(entry_id, {})
        if not args.only and path.exists() and all(old.get(k) == v for k, v in spec.items()):
            if old.get('sha256') == hashlib.sha256(path.read_bytes()).hexdigest():
                return
        async with semaphore:
            temporary = path.with_suffix('.part')
            for attempt in range(3):
                try:
                    await edge_tts.Communicate(text, voice, rate=RATE).save(str(temporary))
                    info = MP3(temporary).info
                    if not 0.3 <= info.length <= 15 or temporary.stat().st_size < 1000:
                        raise ValueError(f'Invalid clip duration/size: {entry_id}')
                    temporary.replace(path)
                    sources[entry_id] = dict(**spec, provider='Microsoft Edge online speech via edge-tts',
                        file=f'audio/{language}/{entry_id}.mp3', duration_seconds=round(info.length, 3),
                        sha256=hashlib.sha256(path.read_bytes()).hexdigest())
                    save_manifest()
                    print(f'{entry_id}: {info.length:.2f}s ({len(sources)}/{len(entries)})', flush=True)
                    break
                except Exception:
                    temporary.unlink(missing_ok=True)
                    if attempt == 2:
                        raise
                    await asyncio.sleep(2 * (attempt + 1))
            await asyncio.sleep(0.25)

    await asyncio.gather(*(generate(entry) for entry in entries))
    print('Audio generation complete.', flush=True)


if __name__ == '__main__':
    asyncio.run(main())
