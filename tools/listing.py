"""Apothic Pots CurseForge / README art: icon, banner, gallery, perf chart.
python3 tools/listing.py <shots_dir> <out_dir> <ascii.png> <perf results.json> <block textures dir>\nascii.png and block textures come from the vanilla client jar (assets/minecraft/textures).
Text uses the vanilla Minecraft font (like the Apothic Spawners page), demonic crimson theme."""
import json, math, os, random, statistics, sys
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter

SH, OUT, ASCII, PERF, TEX = sys.argv[1:6]
os.makedirs(OUT, exist_ok=True)
W, H = 1280, 720
CRIMSON, CRIMSON_HI = (200, 24, 32), (255, 92, 70)
EMBER, SOUL = (255, 150, 40), (80, 220, 230)
GOLD, GOLD_DIM = (232, 178, 58), (140, 100, 24)
TEXT, MUTED = (238, 226, 222), (178, 160, 156)
CHIPS = [CRIMSON, EMBER, SOUL]

# ---------- Minecraft bitmap font ----------
_atlas = Image.open(ASCII).convert('RGBA')
_glyph, _adv = {}, {}
for code in range(256):
    g = _atlas.crop(((code % 16) * 8, (code // 16) * 8, (code % 16) * 8 + 8, (code // 16) * 8 + 8))
    a = g.split()[3]
    cols = [x for x in range(8) if any(a.getpixel((x, y)) for y in range(8))]
    _glyph[code] = a
    _adv[code] = (max(cols) + 2) if cols else 4


def mc_width(text, scale, bold=False):
    return sum(_adv.get(ord(ch), 6) + (1 if bold else 0) for ch in text) * scale - scale


def mc_mask(text, scale, bold=False):
    w = mc_width(text, 1, bold) + 2
    m = Image.new('L', (w, 9), 0)
    x = 0
    for ch in text:
        g = _glyph.get(ord(ch))
        if g is not None and ch != ' ':
            m.paste(255, (x, 0), g)
            if bold:
                m.paste(255, (x + 1, 0), g)
        x += _adv.get(ord(ch), 6) + (1 if bold else 0)
    return m.resize((w * scale, 9 * scale), Image.NEAREST)


def mc_text(canvas, xy, text, scale, fill=TEXT, bold=False, shadow=True, glow=None, radius=12, strength=200):
    m = mc_mask(text, scale, bold)
    x, y = xy
    if glow:
        layer = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
        col = Image.new('RGBA', m.size, glow + (strength,))
        layer.paste(col, (x, y), m)
        canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(radius)))
    if shadow:
        sc = tuple(int(c * 0.25) for c in fill)
        o = min(scale, 4)
        canvas.paste(Image.new('RGBA', m.size, sc + (255,)), (x + o, y + o), m)
    canvas.paste(Image.new('RGBA', m.size, fill + (255,)), (x, y), m)
    return x + m.width


def shot(name):
    return Image.open(os.path.join(SH, name)).convert('RGB')


def embers(canvas, n, seed, area=None):
    rnd = random.Random(seed)
    x0, y0, x1, y1 = area or (0, 0) + canvas.size
    layer = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for _ in range(n):
        x, y = rnd.uniform(x0, x1), rnd.uniform(y0, y1)
        s = rnd.choice([2, 3, 3, 4, 5])
        col = rnd.choice([EMBER, CRIMSON_HI, (255, 200, 90)])
        d.rectangle([x, y, x + s, y + s], fill=col + (rnd.randint(90, 230),))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(0.6)))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(6)))


def bg(img, size=(W, H)):
    w, h = size
    im = img.resize((w, h), Image.LANCZOS).filter(ImageFilter.GaussianBlur(16))
    im = ImageEnhance.Brightness(im).enhance(0.42)
    im = Image.blend(im, Image.new('RGB', (w, h), (40, 4, 8)), 0.5)
    # crimson underglow from the bottom, near-black at the top
    grad = Image.linear_gradient('L').resize((w, h))
    under = Image.new('RGB', (w, h), (120, 10, 14))
    im = Image.composite(under, im, grad.point(lambda v: int(max(0, v - 150) * 0.9)))
    top = Image.new('RGB', (w, h), (6, 2, 4))
    im = Image.composite(top, im, grad.point(lambda v: int(max(0, 120 - v) * 1.4)).transpose(Image.FLIP_TOP_BOTTOM).transpose(Image.FLIP_TOP_BOTTOM))
    c = im.convert('RGBA')
    embers(c, 70, w * 7 + h, (0, h * 0.45, w, h))
    return c


def window(img, w, crop=None):
    if crop:
        img = img.crop(crop)
    h = round(img.height * w / img.width)
    return img.resize((w, h), Image.LANCZOS).convert('RGBA')


def place(canvas, win, x, y, glow=CRIMSON):
    pad, off = 70, 14
    sh = Image.new('RGBA', (win.width + pad * 2, win.height + pad * 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(sh)
    if glow:
        d.rectangle([pad - 6, pad - 6, pad + win.width + 6, pad + win.height + 6], fill=glow + (150,))
    d.rectangle([pad + off, pad + off, pad + off + win.width, pad + off + win.height], fill=(0, 0, 0, 230))
    canvas.alpha_composite(sh.filter(ImageFilter.GaussianBlur(22)), (x - pad, y - pad))
    canvas.alpha_composite(win, (x, y))
    ImageDraw.Draw(canvas).rectangle([x - 1, y - 1, x + win.width, y + win.height], outline=(90, 16, 20, 255), width=2)


def chips(canvas, x, y):
    d = ImageDraw.Draw(canvas)
    for i, c in enumerate(CHIPS):
        d.rectangle([x + i * 46, y, x + i * 46 + 38, y + 12], fill=c + (255,))


def brand(canvas, xy, scale, radius=18, strength=210):
    x, y = xy
    x = mc_text(canvas, (x, y), 'APOTHIC', scale, fill=CRIMSON_HI, bold=True, glow=CRIMSON, radius=radius, strength=strength)
    mc_text(canvas, (x + 4 * scale, y), 'POTS', scale, fill=(255, 255, 255), bold=True, glow=CRIMSON, radius=radius, strength=strength)


def text_block(canvas, x, y, title, lines, tscale=7, bscale=3):
    if title == 'APOTHIC POTS':
        brand(canvas, (x, y), tscale)
    else:
        mc_text(canvas, (x, y), title, tscale, fill=(255, 255, 255), bold=True, glow=CRIMSON, radius=14, strength=190)
    y += 9 * tscale + 30
    for ln in lines:
        mc_text(canvas, (x, y), ln, bscale, fill=TEXT)
        y += 11 * bscale + 6
    return y


def save(canvas, name):
    im = canvas.convert(canvas.mode if canvas.mode == 'RGBA' and name.startswith('icon') else 'RGB')
    Image.frombytes(im.mode, im.size, im.tobytes()).save(os.path.join(OUT, name), optimize=True)


# ---------- icon: vanilla spawner cage in a terracotta pot, hellfire behind, demon eyes inside ----------
def tex(name):
    return Image.open(os.path.join(TEX, name + '.png')).convert('RGBA')


def icon_art():
    n = 32
    a = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    fire = tex('fire_0').crop((0, 0, 16, 16))
    for fx, fy in [(5, 2), (11, 0)]:
        a.alpha_composite(fire, (fx, fy))
    # spawner: dark-red lit interior behind the cage bars, two eyes
    sp = tex('spawner')
    inner = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 8.5) / 9
            inner.putpixel((x, y), (int(120 * (1 - r) + 20), int(8 * (1 - r)), int(10 * (1 - r) + 4), 255))
    for (x, y), c in {(4, 5): (255, 90, 50), (5, 5): (255, 230, 120), (10, 5): (255, 230, 120), (11, 5): (255, 90, 50), (4, 4): (150, 20, 24), (5, 4): (150, 20, 24), (10, 4): (150, 20, 24), (11, 4): (150, 20, 24)}.items():
        inner.putpixel((x, y), c + (255,))
    inner.alpha_composite(sp)
    a.alpha_composite(inner, (8, 7))
    # pot: rim + tapered body in terracotta, soul soil showing at the rim
    tc = tex('terracotta')
    ss = tex('soul_soil')
    for x in range(7, 25):
        a.putpixel((x, 23), ss.getpixel((x % 16, 2)))
    for y in range(23, 32):
        inset = 0 if y <= 24 else 1 if y <= 28 else 2
        for x in range(6 + inset, 26 - inset):
            if y == 23 and 7 <= x <= 24:
                continue
            c = tc.getpixel((x % 16, y % 16))
            k = 1.25 if y <= 24 else 1.0 - (0.18 if x in (6 + inset, 25 - inset) else 0)
            a.putpixel((x, y), tuple(min(255, int(v * k)) for v in c[:3]) + (255,))
    return a


def icon():
    S = 1024
    art = icon_art().resize((28 * 32, 28 * 32), Image.NEAREST)
    c = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    x, y = (S - art.width) // 2, (S - art.height) // 2 - 10
    mask = Image.new('L', (S, S), 0)
    mask.paste(art.split()[3], (x, y))
    for r, k, col in [(110, 1.6, CRIMSON), (50, 1.5, CRIMSON), (18, 1.1, EMBER)]:
        al = mask.filter(ImageFilter.GaussianBlur(r)).point(lambda v, k=k: min(255, int(v * k)))
        layer = Image.new('RGBA', (S, S), col + (0,))
        layer.putalpha(al)
        c.alpha_composite(layer)
    edge = Image.new('L', (S, S), 0)
    ImageDraw.Draw(edge).ellipse([40, 40, S - 40, S - 40], fill=255)
    c.putalpha(Image.composite(c.split()[3], Image.new('L', (S, S), 0), edge.filter(ImageFilter.GaussianBlur(50))))
    c.alpha_composite(art, (x, y))
    Image.frombytes('RGBA', c.size, c.tobytes()).save(os.path.join(OUT, 'icon-1024.png'))
    Image.frombytes('RGBA', c.size, c.tobytes()).resize((400, 400), Image.LANCZOS).save(os.path.join(OUT, 'icon.png'))
    return c


# ---------- banner ----------
def banner(ic):
    BW, BH = 1600, 400
    src = shot('room_hrbs.png')
    c = bg(src, (BW, BH * 2)).crop((0, BH // 2, BW, BH + BH // 2))
    st = src.resize((760, round(760 * src.height / src.width)), Image.LANCZOS)
    st = ImageEnhance.Brightness(st).enhance(0.5).convert('RGBA')
    st = Image.blend(st, Image.new('RGBA', st.size, (60, 6, 10, 255)), 0.35)
    m = Image.linear_gradient('L').rotate(90).resize(st.size)
    st.putalpha(m.point(lambda v: int(v * 0.85)))
    c.alpha_composite(st, (BW - st.width, (BH - st.height) // 2))
    embers(c, 90, 5)
    # APOTHIC (logo) POTS, centred
    sc, gap, isz = 11, 26, 300
    w1, w2 = mc_width('APOTHIC', sc, True), mc_width('POTS', sc, True)
    total = w1 + gap + isz + gap + w2
    x0 = (BW - total) // 2
    ty = (BH - 9 * sc) // 2 + 4
    mc_text(c, (x0, ty), 'APOTHIC', sc, fill=CRIMSON_HI, bold=True, glow=CRIMSON, radius=24, strength=220)
    c.alpha_composite(ic.resize((isz, isz), Image.LANCZOS), (x0 + w1 + gap, (BH - isz) // 2))
    mc_text(c, (x0 + w1 + gap + isz + gap, ty), 'POTS', sc, fill=(255, 255, 255), bold=True, glow=CRIMSON, radius=24, strength=220)
    save(c, 'banner.png')


# ---------- perf chart, drawn in the listing style ----------
def perf_chart():
    d = json.load(open(PERF))
    m = {(r['kind'], r['n']): statistics.mean(r['ms']) for r in d}
    base = m[('baseline', 0)]
    series = [('spawner_idle', 'Spawners, mobs left alive', (57, 135, 229)),
              ('spawner_kill', 'Spawners, mobs killed instantly', (217, 89, 38)),
              ('pots', 'Apothic Pots', (25, 158, 112))]
    ns = [16, 64, 256]
    c = bg(shot('room_hrbs.png'), (W, 820))
    mc_text(c, (70, 46), 'Performance', 6, fill=(255, 255, 255), bold=True, glow=CRIMSON, radius=14, strength=190)
    mc_text(c, (72, 112), 'Average server tick time (ms), lower is better. Zombie spawners, stock stats.', 2, fill=MUTED)
    px0, py0, px1, py1 = 150, 190, 1210, 680
    panel = Image.new('RGBA', c.size, (0, 0, 0, 0))
    ImageDraw.Draw(panel).rectangle([px0 - 70, py0 - 30, px1 + 30, py1 + 70], fill=(10, 4, 6, 200))
    c.alpha_composite(panel)
    d2 = ImageDraw.Draw(c)
    top = 22.0
    yv = lambda v: py1 - (py1 - py0) * v / top
    for t in range(0, 21, 5):
        d2.line([px0, yv(t), px1, yv(t)], fill=(70, 46, 46, 255), width=1)
        lab = str(t)
        mc_text(c, (px0 - 14 - mc_width(lab, 2), int(yv(t)) - 8), lab, 2, fill=MUTED, shadow=False)
    gw = (px1 - px0) / len(ns)
    bw = 70
    for gi, n in enumerate(ns):
        gx = px0 + gw * gi + gw / 2
        for si, (k, _, col) in enumerate(series):
            v = m[(k, n)]
            x = gx + (si - 1) * (bw + 8) - bw / 2
            d2.rectangle([x, yv(v), x + bw, py1], fill=col + (255,))
            lab = f'{v:.1f}'
            mc_text(c, (int(x + bw / 2 - mc_width(lab, 2) / 2), int(yv(v)) - 26), lab, 2, fill=TEXT)
        lab = f'{n} spawners / pots'
        mc_text(c, (int(gx - mc_width(lab, 2) / 2), py1 + 16), lab, 2, fill=TEXT)
    for x in range(px0, px1, 14):
        d2.line([x, yv(base), x + 7, yv(base)], fill=(170, 160, 150, 255), width=2)
    d2.line([px0, py1, px1, py1], fill=(150, 120, 110, 255), width=2)
    lx, ly = px0 + 30, py0 + 6
    for k, label, col in series:
        d2.rectangle([lx, ly + 3, lx + 26, ly + 17], fill=col + (255,))
        mc_text(c, (lx + 38, ly), label, 2, fill=TEXT)
        ly += 30
    d2.line([lx, ly + 10, lx + 26, ly + 10], fill=(170, 160, 150, 255), width=2)
    mc_text(c, (lx + 38, ly), f'Empty world ({base:.1f} ms)', 2, fill=TEXT)
    mc_text(c, (72, 772), 'Dedicated NeoForge 1.21.1 server, 12 samples over 60 s. Kill farm = /kill every tick, drops deleted (best case).', 2, fill=MUTED, shadow=False)
    save(c, 'performance.png')
    return c


# ---------- gallery ----------
def gallery():
    pages = [
        ('01-apothic-pots.png', 'room_hrbs.png', 'APOTHIC POTS', ['Grow mob loot from Apothic', 'spawners in Botany Pots.'], None),
        ('02-spawner-in-a-pot.png', 'closeup.png', 'Spawner in a pot', ['Silk touch a spawner and', 'place it in a pot on soul', 'sand or soul soil.'], None),
        ('03-spawner-stats.png', 'tooltip.png', 'Spawner stats', ['Delay sets the grow time.', 'Spawn count and echoing', 'add loot rolls. Burning', 'cooks the drops. Redstone', 'control needs power.'], (150, 60, 760, 330)),
        ('04-hopper-pots.png', 'chest.png', 'Hopper pots', ['Hopper pots move the loot', 'into a chest below. Player', 'kill drops still drop.'], (150, 40, 760, 260)),
    ]
    # one title size for every page: the largest that fits beside every screenshot
    TS = 6
    while TS > 3 and any(mc_width(t, TS, True) > W - (560 if cr else 580) - 60 - 110 for _, _, t, _, cr in pages):
        TS -= 1
    for name, src, title, lines, crop in pages:
        img = shot(src)
        c = bg(img)
        win = window(img, 560 if crop else 580, crop)
        wx = W - win.width - 60
        # title shrinks until it fits left of the screenshot; body lines must fit too
        room = wx - 70 - 40
        ts = TS
        assert mc_width(title, ts, True) <= room, (name, room)
        assert all(mc_width(ln, 3) <= room for ln in lines), (name, room)
        y = text_block(c, 70, 200 if len(lines) < 4 else 160, title, lines, ts, 3)
        if name.startswith('01'):
            chips(c, 72, y + 16)
        place(c, win, wx, (H - win.height) // 2)
        save(c, name)
    p = perf_chart().resize((W, round(820 * W / W)))
    c = bg(shot('room_hrbs.png'))
    win = window(p.convert('RGB'), 1120)
    place(c, win, (W - win.width) // 2, (H - win.height) // 2, glow=None)
    save(c, '05-performance.png')


ic = icon()
banner(ic)
gallery()
print('done')
