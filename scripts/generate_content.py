"""Package the approved pictures and bilingual catalog, fully offline.

Illustrations use built-in image_gen; color swatches use generate_colors.py.
This script only resizes saved masters and encodes WebP.
"""
from pathlib import Path
import argparse
import hashlib
import json
from PIL import Image
from generate_colors import COLORS

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
MASTERS = ROOT / 'artifacts/illustrations'

DATA = {
    'animals': ('প্রাণী', 'Animals', '#EDE8D9', [
        ('tiger', 'বাঘ', 'Tiger'), ('elephant', 'হাতি', 'Elephant'),
        ('deer', 'হরিণ', 'Deer'), ('monkey', 'বানর', 'Monkey'),
        ('cow', 'গরু', 'Cow'), ('goat', 'ছাগল', 'Goat'), ('sheep', 'ভেড়া', 'Sheep'),
        ('buffalo', 'মহিষ', 'Buffalo'), ('horse', 'ঘোড়া', 'Horse'),
        ('cat', 'বিড়াল', 'Cat'), ('dog', 'কুকুর', 'Dog'), ('lion', 'সিংহ', 'Lion'),
        ('rabbit', 'খরগোশ', 'Rabbit'), ('squirrel', 'কাঠবিড়ালি', 'Squirrel'),
        ('mongoose', 'বেজি', 'Mongoose'), ('otter', 'ভোঁদড়', 'Otter'),
        ('jackal', 'শিয়াল', 'Jackal'), ('donkey', 'গাধা', 'Donkey'),
        ('porcupine', 'সজারু', 'Porcupine'), ('giraffe', 'জিরাফ', 'Giraffe')]),
    'birds': ('পাখি', 'Birds', '#E5EEDC', [
        ('magpie_robin', 'দোয়েল', 'Magpie-robin'), ('parakeet', 'টিয়া', 'Parakeet'),
        ('myna', 'শালিক', 'Myna'), ('sparrow', 'চড়ুই', 'Sparrow'),
        ('crow', 'কাক', 'Crow'), ('pigeon', 'কবুতর', 'Pigeon'),
        ('kingfisher', 'মাছরাঙা', 'Kingfisher'), ('bulbul', 'বুলবুলি', 'Bulbul'),
        ('koel', 'কোকিল', 'Koel'), ('egret', 'বক', 'Egret'),
        ('duck', 'হাঁস', 'Duck'), ('chicken', 'মুরগি', 'Chicken'),
        ('peacock', 'ময়ূর', 'Peacock'), ('woodpecker', 'কাঠঠোকরা', 'Woodpecker'),
        ('owl', 'পেঁচা', 'Owl'), ('kite', 'চিল', 'Kite'),
        ('vulture', 'শকুন', 'Vulture'), ('dove', 'ঘুঘু', 'Dove'),
        ('tailorbird', 'টুনটুনি', 'Tailorbird'), ('oriole', 'হলদে পাখি', 'Oriole')]),
    'fruits': ('ফল', 'Fruits', '#F6EBD4', [
        ('mango', 'আম', 'Mango'), ('jackfruit', 'কাঁঠাল', 'Jackfruit'),
        ('banana', 'কলা', 'Banana'), ('lychee', 'লিচু', 'Lychee'),
        ('guava', 'পেয়ারা', 'Guava'), ('papaya', 'পেঁপে', 'Papaya'),
        ('pineapple', 'আনারস', 'Pineapple'), ('watermelon', 'তরমুজ', 'Watermelon'),
        ('coconut', 'নারকেল', 'Coconut'), ('jujube', 'বরই', 'Jujube'),
        ('starfruit', 'কামরাঙা', 'Star fruit'), ('wood_apple', 'বেল', 'Bael'),
        ('apple', 'আপেল', 'Apple'), ('orange', 'কমলা', 'Orange'),
        ('lemon', 'লেবু', 'Lemon'), ('pomegranate', 'ডালিম', 'Pomegranate'),
        ('grapes', 'আঙুর', 'Grapes'), ('tamarind', 'তেঁতুল', 'Tamarind'),
        ('custard_apple', 'আতা', 'Custard apple'), ('wax_apple', 'জামরুল', 'Wax apple')]),
    'vegetables': ('সবজি', 'Vegetables', '#E8EDDA', [
        ('eggplant', 'বেগুন', 'Eggplant'), ('potato', 'আলু', 'Potato'),
        ('tomato', 'টমেটো', 'Tomato'), ('bottle_gourd', 'লাউ', 'Bottle gourd'),
        ('pumpkin', 'মিষ্টি কুমড়া', 'Pumpkin'), ('bitter_gourd', 'করলা', 'Bitter gourd'),
        ('okra', 'ঢেঁড়স', 'Okra'), ('cauliflower', 'ফুলকপি', 'Cauliflower'),
        ('cabbage', 'বাঁধাকপি', 'Cabbage'), ('radish', 'মুলা', 'Radish'),
        ('cucumber', 'শসা', 'Cucumber'), ('bean', 'শিম', 'Bean'),
        ('carrot', 'গাজর', 'Carrot'), ('spine_gourd', 'কাঁকরোল', 'Spine gourd'),
        ('onion', 'পেঁয়াজ', 'Onion'), ('garlic', 'রসুন', 'Garlic'),
        ('ginger', 'আদা', 'Ginger'), ('chili', 'মরিচ', 'Chili'),
        ('ridge_gourd', 'ঝিঙে', 'Ridge gourd'), ('pointed_gourd', 'পটল', 'Pointed gourd')]),
    'crops': ('শস্য', 'Crops', '#F2E8CF', [
        ('paddy', 'ধান', 'Paddy'), ('wheat', 'গম', 'Wheat'),
        ('maize', 'ভুট্টা', 'Maize'), ('barley', 'যব', 'Barley'),
        ('nigella', 'কালোজিরা', 'Nigella seeds'), ('coriander_seed', 'ধনে', 'Coriander seeds'),
        ('foxtail_millet', 'কাউন', 'Foxtail millet'), ('lentil', 'মসুর ডাল', 'Lentil'),
        ('mung_bean', 'মুগ ডাল', 'Mung bean'), ('black_gram', 'মাসকলাই', 'Black gram'),
        ('chickpea', 'ছোলা', 'Chickpea'), ('pea', 'মটর', 'Pea'),
        ('grass_pea', 'খেসারি', 'Grass pea'), ('pigeon_pea', 'অড়হর', 'Pigeon pea'),
        ('mustard_seed', 'সরিষা', 'Mustard'), ('sesame_seed', 'তিল', 'Sesame'),
        ('soybean', 'সয়াবিন', 'Soybean'), ('peanut', 'চিনাবাদাম', 'Peanut'),
        ('flaxseed', 'তিসি', 'Flaxseed'), ('sunflower_seed', 'সূর্যমুখীর বীজ', 'Sunflower seed')]),
    'flowers': ('ফুল', 'Flowers', '#F5E4DC', [
        ('water_lily', 'শাপলা', 'Water lily'), ('lotus', 'পদ্ম', 'Lotus'),
        ('rose', 'গোলাপ', 'Rose'), ('hibiscus', 'জবা', 'Hibiscus'),
        ('marigold', 'গাঁদা', 'Marigold'), ('sunflower', 'সূর্যমুখী', 'Sunflower'),
        ('jasmine', 'বেলি', 'Jasmine'), ('tuberose', 'রজনীগন্ধা', 'Tuberose'),
        ('ixora', 'রঙ্গন', 'Ixora'), ('periwinkle', 'নয়নতারা', 'Periwinkle'),
        ('hasnahena', 'হাসনাহেনা', 'Night jessamine'), ('tulip', 'টিউলিপ', 'Tulip'),
        ('bougainvillea', 'বাগানবিলাস', 'Bougainvillea'), ('dahlia', 'ডালিয়া', 'Dahlia'),
        ('chrysanthemum', 'চন্দ্রমল্লিকা', 'Chrysanthemum'), ('zinnia', 'জিনিয়া', 'Zinnia'),
        ('cosmos', 'কসমস', 'Cosmos'), ('frangipani', 'কাঠগোলাপ', 'Frangipani'),
        ('butterfly_pea', 'অপরাজিতা', 'Butterfly pea'), ('kadam', 'কদম', 'Kadam')]),
    'fish': ('মাছ', 'Fish', '#E0EDF0', [
        ('hilsa', 'ইলিশ', 'Hilsa'), ('rohu', 'রুই', 'Rohu'), ('catla', 'কাতলা', 'Catla'),
        ('mrigal', 'মৃগেল', 'Mrigal'), ('climbing_perch', 'কৈ', 'Climbing perch'),
        ('walking_catfish', 'মাগুর', 'Walking catfish'), ('stinging_catfish', 'শিং', 'Stinging catfish'),
        ('snakehead', 'শোল', 'Striped snakehead'), ('shrimp', 'চিংড়ি', 'Shrimp'),
        ('pabda', 'পাবদা', 'Pabda'), ('mola', 'মলা', 'Mola'), ('punti', 'পুঁটি', 'Punti'),
        ('tilapia', 'তেলাপিয়া', 'Tilapia'), ('pangasius', 'পাঙাশ', 'Pangas'),
        ('silver_carp', 'সিলভার কার্প', 'Silver carp'), ('grass_carp', 'গ্রাস কার্প', 'Grass carp'),
        ('common_carp', 'কার্প', 'Carp'), ('chital', 'চিতল', 'Chital'),
        ('boal', 'বোয়াল', 'Boal'), ('pomfret', 'রূপচাঁদা', 'Pomfret')]),
    'insects': ('পোকামাকড়', 'Insects', '#E9EDDA', [
        ('ant', 'পিঁপড়া', 'Ant'), ('butterfly', 'প্রজাপতি', 'Butterfly'),
        ('honeybee', 'মৌমাছি', 'Bee'), ('mosquito', 'মশা', 'Mosquito'),
        ('fly', 'মাছি', 'Fly'), ('dragonfly', 'ফড়িং', 'Dragonfly'),
        ('grasshopper', 'ঘাসফড়িং', 'Grasshopper'), ('cockroach', 'তেলাপোকা', 'Cockroach'),
        ('cricket', 'ঝিঁঝিঁ পোকা', 'Cricket'), ('firefly', 'জোনাকি', 'Firefly'),
        ('beetle', 'গুবরে পোকা', 'Beetle'), ('ladybird', 'লেডিবার্ড', 'Ladybird'),
        ('caterpillar', 'শুঁয়োপোকা', 'Caterpillar'), ('moth', 'মথ', 'Moth'),
        ('termite', 'উইপোকা', 'Termite'), ('bedbug', 'ছারপোকা', 'Bedbug'),
        ('louse', 'উকুন', 'Louse'), ('rice_bug', 'গান্ধি পোকা', 'Rice bug'),
        ('silkworm', 'রেশমপোকা', 'Silkworm'), ('hornet', 'ভিমরুল', 'Hornet')]),
    'vehicles': ('যানবাহন', 'Vehicles', '#E3EAF3', [
        ('car', 'গাড়ি', 'Car'),
        ('bus', 'বাস', 'Bus'),
        ('truck', 'ট্রাক', 'Truck'),
        ('train', 'ট্রেন', 'Train'),
        ('bicycle', 'সাইকেল', 'Bicycle'),
        ('motorcycle', 'মোটরসাইকেল', 'Motorcycle'),
        ('rickshaw', 'রিকশা', 'Rickshaw'),
        ('auto_rickshaw', 'অটোরিকশা', 'Auto rickshaw'),
        ('van', 'ভ্যান', 'Van'),
        ('ambulance', 'অ্যাম্বুলেন্স', 'Ambulance'),
        ('fire_engine', 'দমকলের গাড়ি', 'Fire engine'),
        ('police_car', 'পুলিশের গাড়ি', 'Police car'),
        ('tractor', 'ট্রাক্টর', 'Tractor'),
        ('bulldozer', 'বুলডোজার', 'Bulldozer'),
        ('boat', 'নৌকা', 'Boat'),
        ('launch', 'লঞ্চ', 'Launch'),
        ('ship', 'জাহাজ', 'Ship'),
        ('ferry', 'ফেরি', 'Ferry'),
        ('airplane', 'বিমান', 'Airplane'),
        ('helicopter', 'হেলিকপ্টার', 'Helicopter')]),
    'colors': ('রং', 'Colors', '#F2E5E8', [(key, bn, en) for key, bn, en, _ in COLORS]),
}

FINAL = 480


def main():
    parser = argparse.ArgumentParser(description='Package the offline illustration collection.')
    parser.add_argument('--masters-dir', type=Path, default=MASTERS)
    args = parser.parse_args()
    expected = [key for _, _, _, rows in DATA.values() for key, _, _ in rows]
    missing = [key for key in expected if not (args.masters_dir / f'{key}.png').is_file()]
    if missing:
        raise SystemExit('Missing illustration masters: ' + ', '.join(missing))
    categories, entries, sources = [], [], {}
    ASSETS.joinpath('pictures').mkdir(parents=True, exist_ok=True)
    total_bytes = 0
    for group, (bn, en, bg, items) in DATA.items():
        assert len(items) == 20, group
        categories.append(dict(id=group, bn=bn, en=en, color=bg, cover=items[0][0]))
        for key, item_bn, item_en in items:
            entries.append(dict(id=key, category=group, bn=item_bn, en=item_en, picture=f'pictures/{key}.webp'))
            src = args.masters_dir / f'{key}.png'
            with Image.open(src) as original:
                if original.width != original.height:
                    raise ValueError(f'Expected square illustration: {key}')
                # Resize only: preserve the generated composition, colors and safe margins.
                art = original.convert('RGB').resize((FINAL, FINAL), Image.Resampling.LANCZOS)
                out = ASSETS / 'pictures' / f'{key}.webp'
                art.save(out, 'WEBP', quality=90, method=6, lossless=group == 'colors')
            total_bytes += out.stat().st_size
            meta = json.loads(src.with_suffix('.json').read_text())
            sources[key] = dict(type=meta.get('type', 'ai-generated-illustration'), tool=meta['tool'],
                                master=f'artifacts/illustrations/{key}.png',
                                sha256=hashlib.sha256(out.read_bytes()).hexdigest())
            metadata_key = 'metadata_file' if group == 'colors' else 'prompt_file'
            sources[key][metadata_key] = f'artifacts/illustrations/{key}.json'
            for field in ('reference', 'license', 'license_url'):
                if field in meta:
                    sources[key][field] = meta[field]
    ASSETS.joinpath('collection.json').write_text(
        json.dumps(dict(categories=categories, entries=entries), ensure_ascii=False, indent=2) + '\n')
    ROOT.joinpath('scripts/image_sources.json').write_text(json.dumps(sources, indent=2) + '\n')
    # Removed catalog entries must not leave unused artwork inside the APK.
    for picture in ASSETS.joinpath('pictures').glob('*.webp'):
        if picture.stem not in sources:
            picture.unlink()
    print(f'Built {len(entries)} distinct illustrations ({total_bytes / 1024:.1f} KB) across {len(categories)} categories.')


if __name__ == '__main__':
    main()
