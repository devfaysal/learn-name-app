"""Assemble labeled contact sheets for visual QA of completed illustration groups."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from generate_content import DATA, ROOT, MASTERS

out = ROOT / 'artifacts/illustration-previews'
out.mkdir(parents=True, exist_ok=True)
font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 18)
title_font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 30)
for category, (_, english, _, entries) in DATA.items():
    if any(not (MASTERS / f'{key}.png').exists() for key, _, _ in entries):
        continue
    sheet = Image.new('RGB', (1400, 1320), '#f5f2e9')
    draw = ImageDraw.Draw(sheet)
    draw.text((24, 18), f'{english} — 20 illustrations', fill='#233c32', font=title_font)
    for i, (key, _, label) in enumerate(entries):
        x, y = (i % 5) * 280, 70 + (i // 5) * 310
        with Image.open(MASTERS / f'{key}.png') as original:
            thumb = original.convert('RGB').resize((264, 264), Image.Resampling.LANCZOS)
        sheet.paste(thumb, (x + 8, y))
        draw.text((x + 140, y + 276), label, fill='#233c32', font=font, anchor='mt')
    sheet.save(out / f'{category}.jpg', quality=92)
    print(out / f'{category}.jpg')

if all((out / f'{category}.jpg').exists() for category in DATA):
    overview = Image.new('RGB', (1400, ((len(DATA) + 1) // 2) * 660), '#f5f2e9')
    for i, category in enumerate(DATA):
        with Image.open(out / f'{category}.jpg') as sheet:
            overview.paste(sheet.resize((700, 660), Image.Resampling.LANCZOS),
                           ((i % 2) * 700, (i // 2) * 660))
    overview.save(ROOT / 'artifacts/collection-contact-sheet.jpg', quality=94)
