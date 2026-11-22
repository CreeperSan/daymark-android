#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""按控件文字/类型查找界面元素并输出其中心坐标。

用法：
    python .tools/find.py "下一步"        # 打印中心坐标 "x y"
    python .tools/find.py --all           # 打印所有带文字的节点

依赖 uitest 生成的 layout.json（由 dev.sh tapText 负责生成）。
"""
import io
import json
import sys


def walk(node, out, depth=0):
    attrs = node.get('attributes', {})
    text = attrs.get('text', '') or ''
    bounds = attrs.get('bounds', '')
    if text.strip():
        out.append((text, bounds, attrs.get('type', ''), depth))
    for child in node.get('children', []):
        walk(child, out, depth + 1)


def parse_bounds(bounds):
    # 形如 "[873,129][1012,230]"
    try:
        left = bounds.split(']')[0].lstrip('[').split(',')
        right = bounds.split('[')[-1].rstrip(']').split(',')
        x1, y1 = int(left[0]), int(left[1])
        x2, y2 = int(right[0]), int(right[1])
        return x1, y1, x2, y2
    except Exception:
        return None


def main():
    path = '.shots/layout.json'
    data = json.load(io.open(path, encoding='utf-8'))
    nodes = []
    walk(data, nodes)

    if len(sys.argv) < 2:
        print('usage: find.py <text> | --all')
        return 1

    if sys.argv[1] == '--all':
        for text, bounds, kind, depth in nodes:
            print('{}{} {} {}'.format('  ' * depth, kind, repr(text), bounds))
        return 0

    target = sys.argv[1]
    # 优先完全匹配，其次包含匹配
    exact = [n for n in nodes if n[0] == target]
    fuzzy = [n for n in nodes if target in n[0]]
    pick = exact[0] if exact else (fuzzy[0] if fuzzy else None)
    if pick is None:
        print('NOT_FOUND')
        return 2
    rect = parse_bounds(pick[1])
    if rect is None:
        print('BAD_BOUNDS ' + pick[1])
        return 3
    x1, y1, x2, y2 = rect
    print('{} {}'.format((x1 + x2) // 2, (y1 + y2) // 2))
    return 0


if __name__ == '__main__':
    sys.exit(main())
