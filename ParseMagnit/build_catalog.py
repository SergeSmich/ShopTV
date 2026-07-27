# -*- coding: utf-8 -*-
"""
Сборка единого каталога Магнита для Android TV приложения.

Объединяет разрозненные выгрузки парсеров в один нормализованный файл
app/src/main/assets/magnit_catalog.json, который читает приложение.

Источники:
  magnit_real_cards.json    - 32 карточки с картинками, id, рейтингом (категория "Молочное")
  magnit_live_discounts.json- 41 скидка по категориям Молочное / Заморозка / Фан-зона
  magnit_all_discounts.json - 23 скидки по категориям Молочное / Птица, мясо, рыба

Товары дедуплицируются по названию, картинки подтягиваются из real_cards.
"""

import json
import os
import re
from collections import OrderedDict

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_PATH = os.path.join(
    BASE_DIR, "..", "app", "src", "main", "assets", "magnit_catalog.json"
)

# Категория, к которой относится слепок real_cards (подтверждено пересечением
# со скидками из magnit_live_discounts.json)
REAL_CARDS_CATEGORY = "Молочное"

# Порядок строк на экране телевизора
CATEGORY_ORDER = ["Молочное", "Заморозка", "Фан-зона", "Птица, мясо, рыба"]


def load(name):
    path = os.path.join(BASE_DIR, name)
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except (IOError, ValueError) as e:
        print("  [пропуск] %s: %s" % (name, e))
        return []


def to_float(value):
    """'104.99' / 104.99 / '' -> float | None"""
    if value is None or value == "":
        return None
    try:
        return float(str(value).replace(",", "."))
    except ValueError:
        return None


def percent_from(sale_percent, price, old_price):
    """Магнит отдаёт '-23%' или -1. Иначе считаем сами."""
    if isinstance(sale_percent, str):
        m = re.search(r"(\d+)", sale_percent)
        if m:
            return int(m.group(1))
    if price and old_price and old_price > price:
        return int(round((old_price - price) / old_price * 100))
    return 0


def normalize_key(title):
    return re.sub(r"\s+", " ", title).strip().lower()


def make_product(title, price, old_price, category, **extra):
    """Единый вид товара для приложения."""
    percent = percent_from(extra.get("sale_percent"), price, old_price)
    if not old_price or not price or old_price <= price:
        old_price = None
        percent = 0
    return OrderedDict([
        ("id", extra.get("id") or ""),
        ("title", title.strip()),
        ("category", category),
        ("price", price),
        ("oldPrice", old_price),
        ("discountPercent", percent),
        ("benefit", round(old_price - price, 2) if old_price else 0),
        ("imageUrl", extra.get("image") or None),
        ("detailUrl", extra.get("link") or None),
        ("rating", extra.get("rating")),
        ("quantity", extra.get("quantity") or 0),
    ])


def main():
    products = OrderedDict()   # ключ -> товар
    images = {}                # ключ -> данные карточки с картинкой

    print("=== Сборка каталога Магнита ===")

    # 1. real_cards - единственный источник картинок, кладём первым
    real_cards = load("magnit_real_cards.json")
    for c in real_cards:
        title = c.get("title")
        if not title:
            continue
        key = normalize_key(title)
        price = to_float(c.get("price"))
        if price is None:
            continue
        ratings = c.get("ratings") or {}
        images[key] = c
        products[key] = make_product(
            title, price, to_float(c.get("oldPrice")), REAL_CARDS_CATEGORY,
            id=c.get("id"),
            image=c.get("image"),
            link=c.get("link"),
            rating=ratings.get("rating"),
            quantity=c.get("quantity"),
            sale_percent=c.get("salePercent"),
        )
    print("  real_cards:        %2d товаров (с картинками)" % len(real_cards))

    # 2. live_discounts - категории Заморозка / Фан-зона / Молочное
    live = load("magnit_live_discounts.json")
    added = 0
    for d in live:
        title = d.get("Товар")
        price = to_float(d.get("Цена со скидкой"))
        if not title or price is None:
            continue
        key = normalize_key(title)
        category = d.get("Категория поиска") or REAL_CARDS_CATEGORY
        if key in products:
            # товар уже есть - только уточняем категорию, картинку не теряем
            if products[key]["category"] == REAL_CARDS_CATEGORY:
                products[key]["category"] = category
            continue
        src = images.get(key, {})
        products[key] = make_product(
            title, price, to_float(d.get("Старая цена")), category,
            image=src.get("image"), link=src.get("link"), id=src.get("id"),
            quantity=d.get("Остаток"),
            sale_percent=d.get("Процент скидки"),
        )
        added += 1
    print("  live_discounts:    %2d записей, новых %d" % (len(live), added))

    # 3. all_discounts - категория Птица, мясо, рыба
    alld = load("magnit_all_discounts.json")
    added = 0
    for d in alld:
        title = d.get("Товар")
        price = to_float(d.get("Цена со скидкой"))
        if not title or price is None:
            continue
        key = normalize_key(title)
        if key in products:
            continue
        src = images.get(key, {})
        products[key] = make_product(
            title, price, to_float(d.get("Старая цена")), d.get("Категория"),
            image=src.get("image"), link=src.get("link"), id=src.get("id"),
            quantity=d.get("Остаток"),
            sale_percent=d.get("Скидка (%)"),
        )
        added += 1
    print("  all_discounts:     %2d записей, новых %d" % (len(alld), added))

    items = list(products.values())

    # Группировка по категориям в заданном порядке
    by_category = OrderedDict()
    for name in CATEGORY_ORDER:
        group = [p for p in items if p["category"] == name]
        if group:
            # сначала со скидкой, внутри - по величине скидки
            group.sort(key=lambda p: (-p["discountPercent"], p["title"]))
            by_category[name] = group
    # категории, не попавшие в список порядка
    for p in items:
        if p["category"] in by_category:
            continue
        rest = [q for q in items if q["category"] == p["category"]]
        rest.sort(key=lambda q: (-q["discountPercent"], q["title"]))
        by_category[p["category"]] = rest

    # Отдельная строка "Скидки дня"
    discounts = sorted(
        [p for p in items if p["discountPercent"] > 0],
        key=lambda p: -p["discountPercent"],
    )

    rows = [OrderedDict([("title", "Скидки дня"), ("items", discounts)])]
    for name, group in by_category.items():
        rows.append(OrderedDict([("title", name), ("items", group)]))

    catalog = OrderedDict([
        ("shop", "Магнит"),
        ("shopCode", "781225"),
        ("totalItems", len(items)),
        ("rows", rows),
    ])

    out = os.path.normpath(OUT_PATH)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=1)

    print("\n=== Готово ===")
    print("Всего уникальных товаров: %d" % len(items))
    print("С картинками:             %d" % len([p for p in items if p["imageUrl"]]))
    print("Со скидкой:               %d" % len(discounts))
    print("\nСтроки каталога:")
    for r in rows:
        with_img = len([p for p in r["items"] if p["imageUrl"]])
        print("  %-20s %3d товаров (с картинками %d)"
              % (r["title"], len(r["items"]), with_img))
    print("\nФайл: %s" % out)


if __name__ == "__main__":
    main()
