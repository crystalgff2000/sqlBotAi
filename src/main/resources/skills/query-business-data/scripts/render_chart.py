#!/usr/bin/env python3
# Compatible with Python 3.6+
import argparse
import json
import math
import re
import sys
from collections import defaultdict
from decimal import Decimal, InvalidOperation
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


PALETTE = ["#2F6BFF", "#00A6A6", "#F59E0B", "#D9485F", "#6D5BD0", "#3B8C4A"]
BACKGROUND = "#F7FAFC"
PANEL = "#FFFFFF"
TEXT = "#172033"
MUTED = "#667085"
GRID = "#D9E2EC"


def replace_output_name(output, suffix):
    """Python 3.6 compatible replacement for Path.with_name()."""
    return output.parent / "{0}-{1}{2}".format(output.stem, suffix, output.suffix)


def text_width(draw, text, font):
    if hasattr(draw, "textbbox"):
        box = draw.textbbox((0, 0), text, font=font)
        return box[2] - box[0]
    return draw.textsize(text, font=font)[0]


def draw_panel(draw, xy, radius=12, fill=PANEL):
    if hasattr(draw, "rounded_rectangle"):
        draw.rounded_rectangle(xy, radius=radius, fill=fill)
    else:
        draw.rectangle(xy, fill=fill)


def draw_bar_segment(draw, xy, radius=4, fill=None):
    if hasattr(draw, "rounded_rectangle"):
        draw.rounded_rectangle(xy, radius=radius, fill=fill)
    else:
        draw.rectangle(xy, fill=fill)


def load_font(size, bold=False):
    candidates = [
        "/System/Library/Fonts/STHeiti Medium.ttc" if bold else "/System/Library/Fonts/STHeiti Light.ttc",
        "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc"
        if bold
        else "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
        if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size=size, index=0)
    return ImageFont.load_default()


def numeric(value):
    if value is None or value == "":
        return None
    try:
        return float(Decimal(str(value).replace(",", "")))
    except InvalidOperation:
        return None


def compact_number(value, percent=False):
    if percent:
        return f"{value * 100:.1f}%"
    absolute = abs(value)
    if absolute >= 100000000:
        return f"{value / 100000000:.2f}亿"
    if absolute >= 10000:
        return f"{value / 10000:.2f}万"
    if absolute >= 1000:
        return f"{value:,.0f}"
    if absolute >= 10:
        return f"{value:,.1f}"
    return f"{value:,.2f}"


def safe_name(value):
    cleaned = re.sub(r"[^0-9A-Za-z\u4e00-\u9fff_-]+", "_", str(value)).strip("_")
    return cleaned[:80] or "chart"


def draw_title(draw, title, subtitle, width):
    title_font = load_font(30, bold=True)
    subtitle_font = load_font(17)
    draw.text((64, 38), title, fill=TEXT, font=title_font)
    if subtitle:
        draw.text((64, 80), subtitle, fill=MUTED, font=subtitle_font)
    draw.line((64, 112, width - 64, 112), fill=GRID, width=2)


def render_bar(rows, category_index, value_index, title, subtitle, output, percent):
    values = [(str(row[category_index] or "未分类"), numeric(row[value_index])) for row in rows]
    values = [(label, value) for label, value in values if value is not None]
    if not values:
        raise ValueError("No numeric rows available for the chart")
    values = values[:30]
    width = 1400
    row_height = 42
    height = max(560, 170 + row_height * len(values))
    image = Image.new("RGB", (width, height), BACKGROUND)
    draw = ImageDraw.Draw(image)
    draw_panel(draw, (32, 24, width - 32, height - 24))
    draw_title(draw, title, subtitle, width)
    label_font = load_font(16)
    value_font = load_font(15, bold=True)
    left = 300
    right = width - 100
    top = 140
    max_value = max(value for _, value in values)
    min_value = min(value for _, value in values)
    baseline_value = min(0, min_value)
    span = max(max_value - baseline_value, 1e-12)
    baseline_x = left + (0 - baseline_value) / span * (right - left)
    for index, (label, value) in enumerate(values):
        y = top + index * row_height
        draw.line((left, y + 28, right, y + 28), fill="#EEF2F6", width=1)
        display_label = label if len(label) <= 22 else label[:21] + "…"
        draw.text((left - 18 - text_width(draw, display_label, label_font), y + 5), display_label, fill=TEXT, font=label_font)
        value_x = left + (value - baseline_value) / span * (right - left)
        x0, x1 = sorted((baseline_x, value_x))
        if abs(x1 - x0) < 2:
            x1 = x0 + 2
        draw_bar_segment(draw, (x0, y + 4, x1, y + 28), fill=PALETTE[index % len(PALETTE)])
        value_label = compact_number(value, percent)
        text_x = min(max(x1 + 8, left + 8), right - 90)
        draw.text((text_x, y + 5), value_label, fill=TEXT, font=value_font)
    image.save(output)


def render_line(rows, category_index, value_index, title, subtitle, output, percent):
    values = [(str(row[category_index] or ""), numeric(row[value_index])) for row in rows]
    values = [(label, value) for label, value in values if value is not None]
    if len(values) < 2:
        raise ValueError("Line chart requires at least two numeric rows")
    values = values[:50]
    width, height = 1400, 780
    image = Image.new("RGB", (width, height), BACKGROUND)
    draw = ImageDraw.Draw(image)
    draw_panel(draw, (32, 24, width - 32, height - 24))
    draw_title(draw, title, subtitle, width)
    left, right, top, bottom = 130, width - 90, 160, height - 110
    label_font = load_font(14)
    value_font = load_font(14, bold=True)
    raw_values = [value for _, value in values]
    minimum = min(raw_values)
    maximum = max(raw_values)
    padding = max((maximum - minimum) * 0.1, abs(maximum) * 0.03, 1e-9)
    y_min = min(0, minimum - padding)
    y_max = maximum + padding
    for tick in range(6):
        y = top + (bottom - top) * tick / 5
        tick_value = y_max - (y_max - y_min) * tick / 5
        draw.line((left, y, right, y), fill=GRID, width=1)
        label = compact_number(tick_value, percent)
        draw.text((left - 14 - text_width(draw, label, label_font), y - 8), label, fill=MUTED, font=label_font)
    points = []
    for index, (_, value) in enumerate(values):
        x = left + (right - left) * index / max(len(values) - 1, 1)
        y = bottom - (value - y_min) / (y_max - y_min) * (bottom - top)
        points.append((x, y))
    draw.line(points, fill=PALETTE[0], width=4)
    for index, ((label, value), point) in enumerate(zip(values, points)):
        x, y = point
        draw.ellipse((x - 5, y - 5, x + 5, y + 5), fill=PANEL, outline=PALETTE[0], width=3)
        if len(values) <= 16 or index in {0, len(values) - 1}:
            value_label = compact_number(value, percent)
            draw.text((x - 28, y - 30), value_label, fill=TEXT, font=value_font)
        if len(values) <= 12 or index % math.ceil(len(values) / 12) == 0 or index == len(values) - 1:
            display = label if len(label) <= 12 else label[:11] + "…"
            draw.text((x - text_width(draw, display, label_font) / 2, bottom + 18), display, fill=MUTED, font=label_font)
    image.save(output)


def render_one(rows, columns, args, output_path, title_suffix=None):
    try:
        category_index = columns.index(args.category)
        value_index = columns.index(args.value)
    except ValueError as error:
        raise ValueError("Unknown chart column: {0}".format(error))
    chart_type = args.chart_type
    if chart_type == "auto":
        labels = [str(row[category_index] or "") for row in rows]
        looks_temporal = all(re.fullmatch(r"\d{6,8}|\d{4}-\d{2}(?:-\d{2})?", label) for label in labels if label)
        chart_type = "line" if looks_temporal and len(rows) > 1 else "bar"
    title = args.title if title_suffix is None else f"{args.title} - {title_suffix}"
    subtitle = args.subtitle or f"{args.value}，共 {len(rows)} 项"
    if chart_type == "line":
        render_line(rows, category_index, value_index, title, subtitle, output_path, args.percent)
    else:
        render_bar(rows, category_index, value_index, title, subtitle, output_path, args.percent)


def main():
    parser = argparse.ArgumentParser(description="Render a compact PNG chart from query result JSON.")
    parser.add_argument("--input", required=True)
    parser.add_argument("--category", required=True)
    parser.add_argument("--value", required=True)
    parser.add_argument("--title", required=True)
    parser.add_argument("--subtitle")
    parser.add_argument("--chart-type", choices=["auto", "bar", "line"], default="auto")
    parser.add_argument("--facet")
    parser.add_argument("--percent", action="store_true")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    payload = json.loads(Path(args.input).read_text(encoding="utf-8"))
    columns = payload["columns"]
    rows = payload["rows"]
    if not rows:
        raise ValueError("Cannot render a chart for an empty result")
    output = Path(args.output).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    generated = []
    if args.facet:
        if args.facet not in columns:
            raise ValueError(f"Unknown facet column: {args.facet}")
        facet_index = columns.index(args.facet)
        groups = defaultdict(list)
        for row in rows:
            groups[str(row[facet_index] or "未分类")].append(row)
        for facet_value, group_rows in groups.items():
            facet_output = replace_output_name(output, safe_name(facet_value))
            render_one(group_rows, columns, args, facet_output, facet_value)
            generated.append(str(facet_output))
    else:
        render_one(rows, columns, args, output)
        generated.append(str(output))
    print(json.dumps({"charts": generated}, ensure_ascii=False))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError) as error:
        print(json.dumps({"error": str(error)}, ensure_ascii=False), file=sys.stderr)
        sys.exit(2)
