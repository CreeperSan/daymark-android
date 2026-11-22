"""
应用图标生成脚本（唯一来源，不要手改 png）。

图标必须和 Android 平台一致，所以这里不重新画，而是直接复用 Android 工程的素材：
    daymark/launcher/orange-background.png             背景层原图（1254px）
    daymark/launcher/star-light-apricot-foreground.png 前景星形原图（1254px）

Android 侧的 drawable-nodpi/daymark_launcher_{background,foreground}.png 就是把这两张
按「星形宽度 = 画布 0.44」缩放后的结果（432px 画布、190px 星形）。这里用同样的比例
在 1024 画布上还原，两个平台的图标看起来就是同一个。

输出：
1. AppScope/resources/base/media/{background,foreground}.png  —— 分层图标（系统裁形状）
2. entry/src/main/resources/base/media/{background,foreground}.png —— 模块内副本
3. entry/src/main/resources/base/media/startIcon.png —— 启动页图标（自带圆角，原样显示）

用法：python .tools/gen_icon.py
"""

import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = os.path.dirname(ROOT)

SRC_BG = os.path.join(REPO, 'daymark/launcher/orange-background.png')
SRC_FG = os.path.join(REPO, 'daymark/launcher/star-light-apricot-foreground.png')

SIZE = 1024
# 星形宽度占画布的比例，与 Android 的 432 画布 / 190px 星形一致（190 / 432 ≈ 0.44）
STAR_RATIO = 0.44


def alpha_bbox(img, threshold=128, step=2):
    """只按 alpha 通道求包围盒：原图透明区域的 RGB 仍有颜色值，直接 getbbox 会得到整张画布。"""
    alpha = img.split()[3]
    width, height = alpha.size
    px = alpha.load()
    min_x, min_y, max_x, max_y = width, height, 0, 0
    for y in range(0, height, step):
        for x in range(0, width, step):
            if px[x, y] > threshold:
                if x < min_x:
                    min_x = x
                if x > max_x:
                    max_x = x
                if y < min_y:
                    min_y = y
                if y > max_y:
                    max_y = y
    return min_x, min_y, max_x, max_y


def build_foreground(source):
    """把星形缩放到和 Android 相同的比例，并居中放进 1024 画布。"""
    box = alpha_bbox(source)
    star_width = box[2] - box[0]
    if star_width <= 0:
        raise RuntimeError('前景素材里没有找到不透明内容')
    scale = SIZE * STAR_RATIO / star_width

    scaled = source.resize((round(source.width * scale), round(source.height * scale)), Image.LANCZOS)
    center_x = (box[0] + box[2]) / 2 * scale
    center_y = (box[1] + box[3]) / 2 * scale
    offset = (round(SIZE / 2 - center_x), round(SIZE / 2 - center_y))

    canvas = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    canvas.alpha_composite(scaled, offset)
    return canvas


def rounded_mask(size, radius_ratio=0.24):
    ss = 4
    mask = Image.new('L', (size * ss, size * ss), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, size * ss - 1, size * ss - 1], radius=int(size * ss * radius_ratio), fill=255)
    return mask.resize((size, size), Image.LANCZOS)


def save(img, rel):
    img.save(os.path.join(ROOT, rel), 'PNG')
    print('wrote', rel, img.size)


def main():
    for path in (SRC_BG, SRC_FG):
        if not os.path.exists(path):
            print('缺少素材：' + path + '\n请确认 Android 工程（daymark/launcher）在同一仓库内。')
            return 1

    background = Image.open(SRC_BG).convert('RGB').resize((SIZE, SIZE), Image.LANCZOS)
    foreground = build_foreground(Image.open(SRC_FG).convert('RGBA'))

    for base in ('AppScope/resources/base/media', 'entry/src/main/resources/base/media'):
        save(background.convert('RGBA'), base + '/background.png')
        save(foreground, base + '/foreground.png')

    # 启动页图标：与桌面图标同样的构图，但自带圆角，会按原图显示
    tile = background.convert('RGBA')
    tile.putalpha(rounded_mask(SIZE))
    tile.alpha_composite(foreground)
    save(tile.resize((456, 456), Image.LANCZOS), 'entry/src/main/resources/base/media/startIcon.png')
    return 0


if __name__ == '__main__':
    sys.exit(main())
