"""
应用图标生成脚本（唯一来源，不要手改 png）。

生成内容：
1. AppScope/resources/base/media/{background,foreground}.png  —— 分层图标（系统负责裁形状）
2. entry/src/main/resources/base/media/{background,foreground}.png —— 同上，模块内副本
3. entry/src/main/resources/base/media/startIcon.png —— 启动页图标（自带圆角，会原样显示）

设计：品牌暖橙渐变底 + 白色心形。
心形直接复用 ic_heart.svg 的路径点，保证与应用内图标形状完全一致。

用法：python .tools/gen_icon.py
"""

import io
import os

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

SIZE = 1024
# 品牌渐变：与 Theme.accentStart / accentEnd 保持一致
STOP_START = (255, 148, 89)
STOP_END = (255, 107, 82)
# 左上角柔光，让纯色底有一点体积感
HIGHLIGHT = (255, 196, 150)


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def diagonal_gradient(size):
    """从左上到右下的线性渐变（在低分辨率上算完再放大，边缘不会出现色阶）。"""
    small = 256
    img = Image.new('RGB', (small, small))
    px = img.load()
    for y in range(small):
        for x in range(small):
            # 对角线投影，保证左上最亮、右下最深
            t = (x + y) / (2 * (small - 1))
            px[x, y] = lerp(STOP_START, STOP_END, t)
    return img.resize((size, size), Image.BICUBIC)


def add_highlight(img, size):
    """左上角叠一层柔和的高光。"""
    glow = Image.new('L', (size, size), 0)
    d = ImageDraw.Draw(glow)
    r = int(size * 0.62)
    d.ellipse([-r // 2, -r // 2, r, r], fill=90)
    glow = glow.filter(ImageFilter.GaussianBlur(int(size * 0.09)))
    light = Image.new('RGB', (size, size), HIGHLIGHT)
    return Image.composite(light, img, glow.point(lambda v: int(v * 0.55)))


def heart_points(size, scale, offset_y=0.0):
    """把 ic_heart.svg 的路径点（24x24 视图）换算成位图坐标。"""
    svg = io.open(os.path.join(ROOT, 'entry/src/main/resources/base/media/ic_heart.svg'),
                  encoding='utf-8').read()
    start = svg.index('d="') + 3
    end = svg.index('"', start)
    d = svg[start:end]

    numbers = []
    current = ''
    for ch in d:
        if ch in 'MLZ':
            if current.strip():
                numbers.append(current.strip())
            current = ''
            continue
        current += ch
    # 每组两个数字是一个点
    raw = []
    buf = ''
    for ch in d.replace('M', ' ').replace('L', ' ').replace('Z', ' ').split():
        raw.append(ch)

    points = []
    for token in raw:
        if ',' in token:
            a, b = token.split(',')
        else:
            a, b = token, None
        try:
            points.append(float(a))
            if b is not None:
                points.append(float(b))
        except ValueError:
            continue

    pairs = [(points[i], points[i + 1]) for i in range(0, len(points) - 1, 2)]

    # 24 视图 -> 目标尺寸，整体居中
    span = 24.0
    k = size * scale / span
    cx = size / 2.0
    cy = size / 2.0
    out = []
    for x, y in pairs:
        out.append((cx + (x - 12.0) * k, cy + (y - 12.0) * k + offset_y * size))
    return out


def draw_heart(size, scale, shadow=True):
    ss = 4  # 超采样倍数，保证斜边平滑
    canvas = Image.new('RGBA', (size * ss, size * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(canvas)
    pts = heart_points(size * ss, scale, 0.0)
    d.polygon(pts, fill=(255, 255, 255, 255))
    img = canvas.resize((size, size), Image.LANCZOS)

    if shadow:
        alpha = img.split()[3].filter(ImageFilter.GaussianBlur(size * 0.03))
        shade = Image.new('RGBA', (size, size), (150, 48, 20, 70))
        shade.putalpha(alpha.point(lambda v: int(v * 0.55)))
        base = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        base.alpha_composite(shade, (0, int(size * 0.012)))
        base.alpha_composite(img)
        return base
    return img


def save(img, rel):
    path = os.path.join(ROOT, rel)
    img.save(path, 'PNG')
    print('wrote', rel, img.size)


def rounded_mask(size, radius_ratio=0.24):
    ss = 4
    mask = Image.new('L', (size * ss, size * ss), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0, 0, size * ss - 1, size * ss - 1], radius=int(size * ss * radius_ratio), fill=255)
    return mask.resize((size, size), Image.LANCZOS)


def main():
    # 1) 分层图标：背景层（不透明，铺满）+ 前景层（透明，只放心形）
    bg = add_highlight(diagonal_gradient(SIZE), SIZE).convert('RGBA')
    fg = draw_heart(SIZE, 0.46)

    for base in ('AppScope/resources/base/media', 'entry/src/main/resources/base/media'):
        save(bg, base + '/background.png')
        save(fg, base + '/foreground.png')

    # 2) 启动图标：自带圆角，会原样显示在启动页上
    tile = add_highlight(diagonal_gradient(SIZE), SIZE).convert('RGBA')
    tile.putalpha(rounded_mask(SIZE))
    tile.alpha_composite(draw_heart(SIZE, 0.5))
    save(tile.resize((456, 456), Image.LANCZOS), 'entry/src/main/resources/base/media/startIcon.png')


if __name__ == '__main__':
    main()
