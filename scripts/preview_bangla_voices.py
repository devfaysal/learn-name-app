"""Generate two short Bangla voice auditions, outside the app's assets.

Requires edge-tts and mutagen. Sends only the six public dictionary names
to Microsoft's online speech service; saves MP3s for listening comparison.
"""
import asyncio
import json
from pathlib import Path

import edge_tts
from mutagen.mp3 import MP3

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'artifacts/voice-samples'
ENTRY_IDS = ['tiger', 'elephant', 'magpie_robin', 'jackfruit', 'grasshopper', 'nigella']
VOICES = [('nabanita', 'bn-BD-NabanitaNeural'), ('pradeep', 'bn-BD-PradeepNeural')]


async def main():
    catalog = json.loads((ROOT / 'app/src/main/assets/collection.json').read_text())
    entries = {entry['id']: entry for entry in catalog['entries']}
    names = [entries[key]['bn'] for key in ENTRY_IDS]
    text = '। '.join(names) + '।'
    available = {voice['ShortName'] for voice in await edge_tts.list_voices()}
    for _, voice in VOICES:
        if voice not in available:
            raise RuntimeError(f'Voice unavailable: {voice}')
    OUTPUT.mkdir(parents=True, exist_ok=True)

    async def generate(slug, voice):
        path = OUTPUT / f'{slug}-bangla.mp3'
        await edge_tts.Communicate(text, voice, rate='-5%').save(str(path))
        duration = MP3(path).info.length
        if duration < 3 or path.stat().st_size < 1000:
            raise RuntimeError(f'Incomplete audio: {path}')
        print(f'{voice}: {duration:.1f}s, {path.stat().st_size} bytes', flush=True)
        return dict(voice=voice, file=path.name, duration_seconds=round(duration, 2))

    results = await asyncio.gather(*(generate(*voice) for voice in VOICES))
    metadata = dict(provider='Microsoft Edge online speech via edge-tts',
                    rate='-5%', text=text, entries=ENTRY_IDS, samples=results)
    (OUTPUT / 'samples.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    asyncio.run(main())
