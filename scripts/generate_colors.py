"""Draw consistent color-learning swatches with exact, reproducible colors."""
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageColor

ROOT = Path(__file__).resolve().parents[1]
COLORS = [
    ('color_red', 'লাল', 'Red', '#E53935'),
    ('color_blue', 'নীল', 'Blue', '#2355E8'),
    ('color_yellow', 'হলুদ', 'Yellow', '#FFE100'),
    ('color_green', 'সবুজ', 'Green', '#28A745'),
    ('color_orange', 'কমলা', 'Orange', '#FF8A19'),
    ('color_purple', 'বেগুনি', 'Purple', '#883DC0'),
    ('color_pink', 'গোলাপি', 'Pink', '#F48CB4'),
    ('color_black', 'কালো', 'Black', '#202124'),
    ('color_white', 'সাদা', 'White', '#FFFFFF'),
    ('color_brown', 'বাদামি', 'Brown', '#88532F'),
    ('color_gray', 'ধূসর', 'Gray', '#92979D'),
    ('color_sky_blue', 'আকাশি', 'Sky blue', '#79CFFF'),
    ('color_navy_blue', 'গাঢ় নীল', 'Navy blue', '#162C65'),
    ('color_light_green', 'হালকা সবুজ', 'Light green', '#9CDD67'),
    ('color_dark_green', 'গাঢ় সবুজ', 'Dark green', '#0C5B32'),
    ('color_maroon', 'মেরুন', 'Maroon', '#800020'),
    ('color_turquoise', 'ফিরোজা', 'Turquoise', '#28C6C0'),
    ('color_gold', 'সোনালি', 'Gold', '#D4A72C'),
    ('color_silver', 'রুপালি', 'Silver', '#BCC3CB'),
    ('color_cream', 'ঘিয়ে', 'Cream', '#FFF1BC'),
]
METALS = {
    'color_gold': ['#8C651E', '#D4A72C', '#F5D76C', '#FFF6CA', '#C39727', '#E8C45B', '#A77A20'],
    'color_silver': ['#64707D', '#A6AFBA', '#D7DDE3', '#FFFFFF', '#8B97A4', '#D1D8DF', '#788592'],
}


def main():
    folder = ROOT / 'artifacts/illustrations'
    folder.mkdir(parents=True, exist_ok=True)
    size = 1024
    bounds = (112, 112, 912, 912)
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).ellipse(bounds, fill=255)
    for key, bn, en, color in COLORS:
        art = Image.new('RGB', (size, size), '#FFFAF0')
        fill = Image.new('RGB', (size, size), color)
        if key in METALS:
            stops = [ImageColor.getrgb(c) for c in METALS[key]]
            # Broad diagonal reflections distinguish metals from flat yellow/gray.
            draw = ImageDraw.Draw(fill)
            for diagonal in range(2 * size - 1):
                position = max(0, min(1, (diagonal - 300) / 1448)) * (len(stops) - 1)
                index = min(int(position), len(stops) - 2)
                fraction = position - index
                rgb = tuple(round(a + (b - a) * fraction) for a, b in zip(stops[index], stops[index + 1]))
                draw.line((diagonal, 0, 0, diagonal), fill=rgb)
        art.paste(fill, (0, 0), mask)
        # A common fine edge keeps white visible against the ivory background.
        ImageDraw.Draw(art).ellipse(bounds, outline='#C5C9C7', width=3)
        art.save(folder / f'{key}.png')
        metadata = dict(id=key, type='procedural-color-swatch', tool='Pillow',
                        generator='scripts/generate_colors.py', bn=bn, en=en, color=color,
                        shape='circle', background='#FFFAF0',
                        finish='metallic' if key in METALS else 'solid')
        if key in METALS:
            metadata['gradient_stops'] = METALS[key]
        (folder / f'{key}.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n')
    print('Created 20 consistent color swatches, including metallic gold and silver.')


if __name__ == '__main__':
    main()
