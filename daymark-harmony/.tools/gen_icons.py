#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""图标资源生成脚本（输出纯填充 SVG）。

为什么要自己拼路径：
    ArkUI 里 Image.fillColor 只能覆盖 SVG 的 fill，改不了 stroke；
    renderMode(Template) 对 SVG 不生效，对 PNG 只会输出纯白（无法染色）；
    colorBlend 会把整个矩形染色。所以唯一能换色的方案是：
    图标全部用「填充路径」表达，再交给 fillColor 上色。

做法：
    线条 = 每段线两侧各偏移半个线宽拼成四边形，端点和拐点补圆，
           同色图形重叠后视觉等价于一条圆头折线；
    描边圆环 = 两个同心圆叠加，用 fill-rule="evenodd" 挖空。

用法：
    python .tools/gen_icons.py

输出：
    entry/src/main/resources/base/media/ic_*.svg
"""

import math
import os

OUT_DIR = os.path.join('entry', 'src', 'main', 'resources', 'base', 'media')


def round2(value):
    return round(value, 2)


def disc(cx, cy, r):
    """整圆路径：从圆心左移一个半径，用两段半圆弧闭合。"""
    return 'M{} {} m{} 0 a{} {} 0 1 0 {} 0 a{} {} 0 1 0 {} 0 Z'.format(
        round2(cx), round2(cy), round2(-r), round2(r), round2(r), round2(2 * r),
        round2(r), round2(r), round2(-2 * r))


def ring(cx, cy, r, width):
    """圆环：外圆 + 内圆，配合 evenodd 挖空。"""
    return disc(cx, cy, r + width / 2.0) + ' ' + disc(cx, cy, r - width / 2.0)


def polygon(points):
    head = 'M{} {}'.format(round2(points[0][0]), round2(points[0][1]))
    tail = ''.join(' L{} {}'.format(round2(x), round2(y)) for x, y in points[1:])
    return head + tail + ' Z'


def segment(x1, y1, x2, y2, width):
    """线段对应的矩形填充，两端由圆补头。"""
    dx = x2 - x1
    dy = y2 - y1
    length = math.hypot(dx, dy)
    if length == 0:
        return ''
    nx = -dy / length * width / 2.0
    ny = dx / length * width / 2.0
    return polygon([
        (x1 + nx, y1 + ny), (x2 + nx, y2 + ny),
        (x2 - nx, y2 - ny), (x1 - nx, y1 - ny),
    ])


def polyline(points, width):
    """圆头圆角折线。"""
    parts = []
    for i in range(len(points) - 1):
        parts.append(segment(points[i][0], points[i][1], points[i + 1][0], points[i + 1][1], width))
    for x, y in points:
        parts.append(disc(x, y, width / 2.0))
    return ' '.join(part for part in parts if part)


def arc_band(cx, cy, r, width, start_deg, end_deg):
    """圆弧带：内外半径各采样一条折线，围成闭合带状，两端补圆。"""
    steps = max(10, int(abs(end_deg - start_deg) / 4))
    outer = []
    inner = []
    for i in range(steps + 1):
        angle = math.radians(start_deg + (end_deg - start_deg) * i / steps)
        cos_a = math.cos(angle)
        sin_a = math.sin(angle)
        outer.append((cx + cos_a * (r + width / 2.0), cy + sin_a * (r + width / 2.0)))
        inner.append((cx + cos_a * (r - width / 2.0), cy + sin_a * (r - width / 2.0)))
    return (polygon(outer + list(reversed(inner))) + ' ' +
            disc(outer[0][0], outer[0][1], width / 2.0) + ' ' +
            disc(outer[-1][0], outer[-1][1], width / 2.0))


def rounded_rect(x1, y1, x2, y2, radius):
    """圆角矩形填充。"""
    return ('M{l} {y1} L{r2} {y1} A{rad} {rad} 0 0 1 {x2} {t} L{x2} {b} A{rad} {rad} 0 0 1 {r2} {y2} '
            'L{l} {y2} A{rad} {rad} 0 0 1 {x1} {b} L{x1} {t} A{rad} {rad} 0 0 1 {l} {y1} Z').format(
        x1=round2(x1), y1=round2(y1), x2=round2(x2), y2=round2(y2), rad=round2(radius),
        l=round2(x1 + radius), t=round2(y1 + radius),
        b=round2(y2 - radius), r2=round2(x2 - radius))


def ring_rect(x1, y1, x2, y2, radius, width):
    """圆角矩形描边：外框 + 内框，配合 evenodd 挖空。"""
    return (rounded_rect(x1, y1, x2, y2, radius) + ' ' +
            rounded_rect(x1 + width, y1 + width, x2 - width, y2 - width, max(0.5, radius - width)))


def heart_path():
    """心形：四段三次贝塞尔采样成多边形。"""
    segments = [
        ((12, 20.6), (3.6, 15.2), (1.6, 9.6), (4.8, 6.6)),
        ((4.8, 6.6), (7.2, 4.4), (10.4, 5.2), (12, 7.6)),
        ((12, 7.6), (13.6, 5.2), (16.8, 4.4), (19.2, 6.6)),
        ((19.2, 6.6), (22.4, 9.6), (20.4, 15.2), (12, 20.6)),
    ]
    points = []
    for p0, p1, p2, p3 in segments:
        for i in range(20):
            t = i / 20.0
            mt = 1 - t
            x = (mt ** 3) * p0[0] + 3 * (mt ** 2) * t * p1[0] + 3 * mt * (t ** 2) * p2[0] + (t ** 3) * p3[0]
            y = (mt ** 3) * p0[1] + 3 * (mt ** 2) * t * p1[1] + 3 * mt * (t ** 2) * p2[1] + (t ** 3) * p3[1]
            points.append((x, y))
    return polygon(points)


def build(name, layers):
    """layers: [(path_data, fill_rule)]。统一白色填充，运行时由 fillColor 覆盖。"""
    body = []
    for data, rule in layers:
        body.append('  <path fill="#FFFFFF" fill-rule="{}" d="{}"/>'.format(rule, data.strip()))
    # width/height 用百分比：交给外层组件尺寸决定，避免被当成固有像素尺寸
    svg = ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="100%" height="100%">\n' +
           '\n'.join(body) + '\n</svg>\n')
    path = os.path.join(OUT_DIR, 'ic_%s.svg' % name)
    with open(path, 'w', encoding='utf-8') as handle:
        handle.write(svg)
    print('generated', path)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    build('plus', [(polyline([(12, 5.6), (12, 18.4)], 2.0) + ' ' +
                    polyline([(5.6, 12), (18.4, 12)], 2.0), 'nonzero')])
    build('check', [(polyline([(5.4, 12.6), (9.8, 17), (18.6, 7)], 2.6), 'nonzero')])
    build('chevron', [(polyline([(10, 5), (16.6, 12), (10, 19)], 2.4), 'nonzero')])
    build('back', [(polyline([(15.4, 5), (8.4, 12), (15.4, 19)], 2.2), 'nonzero')])
    build('square', [(ring_rect(4.6, 4.6, 19.4, 19.4, 4.2, 1.9), 'evenodd')])
    build('calendar', [
        (ring_rect(4, 6.2, 20, 20, 3.2, 1.7), 'evenodd'),
        (polyline([(4.6, 10.6), (19.4, 10.6)], 1.6), 'nonzero'),
        (polyline([(8, 3.6), (8, 7.2)], 1.9), 'nonzero'),
        (polyline([(16, 3.6), (16, 7.2)], 1.9), 'nonzero'),
    ])
    build('lock', [
        (ring_rect(5.6, 10.6, 18.4, 19.8, 2.8, 1.9), 'evenodd'),
        (arc_band(12, 8.4, 3.4, 1.9, 180, 360), 'nonzero'),
    ])
    eye_layers = [
        (arc_band(12, 17.33, 11.33, 1.8, 208, 332), 'nonzero'),
        (arc_band(12, 6.67, 11.33, 1.8, 28, 152), 'nonzero'),
        (ring(12, 12, 3.2, 1.7), 'evenodd'),
    ]
    build('eye', eye_layers)
    build('eye_off', eye_layers + [(polyline([(4.6, 4.6), (19.4, 19.4)], 1.8), 'nonzero')])
    build('gear', [
        (polyline([(4, 7), (13, 7)], 1.8) + ' ' + polyline([(17.4, 7), (20, 7)], 1.8) + ' ' +
         polyline([(4, 12), (7.4, 12)], 1.8) + ' ' + polyline([(10.6, 12), (20, 12)], 1.8) + ' ' +
         polyline([(4, 17), (13, 17)], 1.8) + ' ' + polyline([(17.4, 17), (20, 17)], 1.8) + ' ' +
         polyline([(15.4, 5.2), (15.4, 8.8)], 1.8) + ' ' +
         polyline([(8.6, 10.2), (8.6, 13.8)], 1.8) + ' ' +
         polyline([(15.4, 15.2), (15.4, 18.8)], 1.8), 'nonzero')])
    build('info', [
        (ring(12, 12, 8.6, 1.8), 'evenodd'),
        (polyline([(12, 11.2), (12, 16.4)], 1.8), 'nonzero'),
        (disc(12, 8, 1.1), 'nonzero'),
    ])
    wifi_layers = [
        (arc_band(12, 17.6, 8.6, 1.9, 205, 335), 'nonzero'),
        (arc_band(12, 17.6, 5.4, 1.9, 205, 335), 'nonzero'),
        (arc_band(12, 17.6, 2.4, 2.0, 205, 335), 'nonzero'),
    ]
    build('wifi', wifi_layers)
    # 断网图标 = wifi + 一条斜杠
    build('wifi_off', wifi_layers + [(polyline([(4.4, 4.4), (19.6, 19.6)], 1.9), 'nonzero')])
    build('heart', [(heart_path(), 'nonzero')])


if __name__ == '__main__':
    main()
