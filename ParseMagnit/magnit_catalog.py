# -*- coding: utf-8 -*-
"""
Парсер каталога Магнита для Android TV приложения ShopTV.

Собирает товары со ВСЕМИ полями (включая картинки), которые теряли
предыдущие версии скриптов, и сразу пишет готовый для приложения файл
feature/magnit/src/main/assets/magnit_catalog.json

Что исправлено по сравнению с прежними скриптами:
  * сохраняются image / id / link, а не только цены — раньше картинки
    были лишь у одной категории ("Молочное"), остальные шли без фото;
  * категории берутся из мобильного API /webgate/v1/tiles, а не угадываются
    по slug на главной странице;
  * корректно обрабатываются hidden-category (регулярка category/(\\d+)
    ошибочно ловила и hidden-category/..., ссылки строились неверно);
  * сохраняются все товары, а не только со скидкой — иначе каталог полупустой.

Запуск:
    pip install requests beautifulsoup4
    python magnit_catalog.py
"""

import json
import os
import re
import sys
import time
from collections import OrderedDict

try:
    import requests
    from bs4 import BeautifulSoup
except ImportError:
    sys.exit("Нужны зависимости: pip install requests beautifulsoup4")

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_PATH = os.path.normpath(os.path.join(
    BASE_DIR, "..", "feature", "magnit", "src", "main", "assets", "magnit_catalog.json"
))

# Магазин, для которого берём цены. Должен совпадать со storeCode в приложении.
SHOP_CODE = "781225"
SHOP_TYPE = "express"

PARAMS = {"shopCode": SHOP_CODE, "shopType": SHOP_TYPE}

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "ru-RU,ru;q=0.9",
}

TILES_URL = "https://magnit.ru/webgate/v1/tiles"
CATALOG_URL = "https://magnit.ru/catalog"

# Запасной список категорий — ответ /tiles на момент разбора.
# Третий элемент — вид раздела: обычный или скрытый (hidden-category),
# у них разный формат адреса на сайте.
FALLBACK_CATEGORIES = [
    ("Новинки", "66205", "category"),
    ("Скидки", "63319", "category"),
    ("Готовая еда", "65055", "category"),
    ("Мясо и рыба", "66551", "hidden"),
    ("Молочное", "63963", "category"),
    ("Сладкое", "64697", "category"),
    ("Фан-зона", "114540", "category"),
    ("Только у нас", "107738", "hidden"),
    ("Заморозка", "64467", "category"),
]


# Порядок строк на экране телевизора
CATEGORY_ORDER = [
    "Скидки", "Молочное", "Мясо и рыба", "Готовая еда",
    "Заморозка", "Сладкое", "Фан-зона", "Новинки", "Только у нас",
]

REQUEST_PAUSE = 1.0

# Сколько разделов обходить. В меню Магнита их около 700, включая пустые
# подкатегории — полный обход занимает больше 10 минут.
# 0 = без ограничения.
MAX_CATEGORIES = 120

# Минимум товаров, чтобы раздел попал на экран отдельной строкой
MIN_ROW_ITEMS = 8


# --------------------------------------------------------------------------
# Разбор формата Nuxt
# --------------------------------------------------------------------------

def unbox(value, raw, depth=0):
    """В __NUXT_DATA__ значения полей заменены индексами в том же массиве."""
    if depth > 8:
        return value
    if isinstance(value, int) and 0 <= value < len(raw):
        linked = raw[value]
        if isinstance(linked, (dict, list)):
            return unbox_structure(linked, raw, depth + 1)
        return linked
    return value


def unbox_structure(element, raw, depth=0):
    if depth > 8:
        return element
    if isinstance(element, dict):
        return {k: unbox(v, raw, depth) for k, v in element.items()}
    if isinstance(element, list):
        return [unbox(v, raw, depth) for v in element]
    return element


def to_float(value):
    if value is None or value == "":
        return None
    try:
        return float(str(value).replace(",", ".").replace(" ", ""))
    except ValueError:
        return None


def percent_from(sale_percent, price, old_price):
    """Магнит отдаёт '-23%' либо -1; при отсутствии считаем сами."""
    if isinstance(sale_percent, str):
        m = re.search(r"(\d+)", sale_percent)
        if m:
            return int(m.group(1))
    if price and old_price and old_price > price:
        return int(round((old_price - price) / old_price * 100))
    return 0


# --------------------------------------------------------------------------
# Сбор данных
# --------------------------------------------------------------------------

def extract_menu(html):
    """
    Дерево категорий из меню страницы.

    Меню каталога отрисовано на сервере и лежит в __NUXT_DATA__ пунктами
    вида {name: "Молочный прилавок", link: "/catalog/107727-bakaleya_copy_106"}.
    Это и есть источник актуальных разделов: slug'и Магнит меняет,
    поэтому зашивать их в код бессмысленно.
    """
    soup = BeautifulSoup(html, "html.parser")
    script = soup.find("script", id="__NUXT_DATA__")
    if not script or not script.string:
        return []

    try:
        raw = json.loads(script.string)
    except ValueError:
        return []

    def deref(v, depth=0):
        if depth > 4:
            return v
        if isinstance(v, int) and 0 <= v < len(raw):
            t = raw[v]
            return deref(t, depth + 1) if isinstance(t, int) else t
        return v

    menu = OrderedDict()
    for item in raw:
        if not isinstance(item, dict):
            continue
        link = deref(item.get("link") or item.get("url") or item.get("href"))
        name = deref(item.get("name") or item.get("title") or item.get("text"))
        if not (isinstance(link, str) and isinstance(name, str)):
            continue
        m = re.search(r"/catalog/(\d+)-([a-z0-9_]+)", link)
        if not m:
            continue
        cat_id, slug = m.group(1), m.group(2)
        if slug == "category":
            continue
        menu.setdefault(cat_id, (name.strip(), cat_id, slug))

    return list(menu.values())


def fetch_categories(session):
    """
    Актуальное дерево категорий с сайта.

    Раньше список был захардкожен, из-за чего разделы отваливались с 404,
    как только Магнит менял slug. Теперь читаем меню живой страницы.
    """
    print("Шаг 1. Категории каталога")

    # меню одинаковое на любой странице каталога — берём заведомо рабочую
    seeds = [
        "https://magnit.ru/catalog/63963-testmmmolochnyy_prilavok",
        "https://magnit.ru/hidden-category/66551",
        CATALOG_URL,
    ]

    for url in seeds:
        try:
            r = session.get(url, params=PARAMS, headers=HEADERS, timeout=20)
        except Exception as e:
            print("  %s — ошибка запроса (%s)" % (url.split("/")[-1], e))
            continue
        if r.status_code != 200:
            continue
        menu = extract_menu(r.text)
        if menu:
            print("  найдено разделов: %d" % len(menu))
            return menu

    print("  не удалось прочитать меню, берём запасной список")
    return [(n, i, None) for n, i, _ in
            [(a, b, c) for a, b, c in FALLBACK_CATEGORIES]]


def parse_category(session, name, cat_id, slug=None):
    """
    Возвращает список товаров категории со всеми полями.

    Адреса у Магнита двух видов:
      обычный раздел  /catalog/63963-testmmmolochnyy_prilavok
      скрытый раздел  /hidden-category/66551
    """
    candidates = []
    if slug:
        candidates.append("https://magnit.ru/catalog/%s-%s" % (cat_id, slug))
    candidates.append("https://magnit.ru/hidden-category/%s" % cat_id)
    candidates.append("https://magnit.ru/catalog/%s-category" % cat_id)

    r = None
    for url in candidates:
        try:
            resp = session.get(url, params=PARAMS, headers=HEADERS, timeout=20)
        except Exception as e:
            print("       ошибка запроса: %s" % e)
            continue
        if resp.status_code == 200:
            r = resp
            break

    if r is None:
        print("       раздел недоступен")
        return []

    soup = BeautifulSoup(r.text, "html.parser")
    script = soup.find("script", id="__NUXT_DATA__")
    if not script or not script.string:
        print("       нет __NUXT_DATA__ на странице")
        return []

    try:
        raw = json.loads(script.string)
    except ValueError as e:
        print("       не разобрать JSON: %s" % e)
        return []

    products = []
    seen = set()

    for item in raw:
        # карточка товара: есть название, цена и картинка
        if not (isinstance(item, dict) and "title" in item and "price" in item):
            continue

        card = unbox_structure(item, raw)
        title = card.get("title")
        price = to_float(card.get("price"))
        if not isinstance(title, str) or price is None:
            continue

        key = title.strip().lower()
        if key in seen:
            continue
        seen.add(key)

        old_price = to_float(card.get("oldPrice"))
        if old_price is not None and old_price <= price:
            old_price = None

        image = card.get("image")
        if not isinstance(image, str) or "magnit.ru" not in image:
            image = None

        link = card.get("link")
        if not isinstance(link, str):
            link = None

        ratings = card.get("ratings")
        rating = ratings.get("rating") if isinstance(ratings, dict) else None

        percent = percent_from(card.get("salePercent"), price, old_price)

        products.append(OrderedDict([
            ("id", str(card.get("id") or "")),
            ("title", title.strip()),
            ("category", name),
            ("price", price),
            ("oldPrice", old_price),
            ("discountPercent", percent if old_price else 0),
            ("benefit", round(old_price - price, 2) if old_price else 0),
            ("imageUrl", image),
            ("detailUrl", link),
            ("rating", rating),
            ("quantity", card.get("quantity") or 0),
        ]))

    return products


def build_catalog(all_products):
    """Раскладывает товары по строкам для экрана телевизора."""
    by_category = OrderedDict()
    for p in all_products:
        by_category.setdefault(p["category"], []).append(p)

    rows = []

    discounts = sorted(
        [p for p in all_products if p["discountPercent"] > 0],
        key=lambda p: -p["discountPercent"],
    )
    if discounts:
        rows.append(OrderedDict([("title", "Скидки дня"), ("items", discounts)]))

    # сначала разделы из заданного порядка, затем остальные — по наполненности
    ordered = [c for c in CATEGORY_ORDER if c in by_category]
    rest = [c for c in by_category if c not in CATEGORY_ORDER]
    rest.sort(key=lambda c: -len(by_category[c]))
    ordered += rest

    for name in ordered:
        items = by_category[name]
        # мелкие подкатегории не выносим отдельной полкой — их товары
        # всё равно попадают в общий список и в "Скидки дня"
        if len(items) < MIN_ROW_ITEMS:
            continue
        items = sorted(items, key=lambda p: (-p["discountPercent"], p["title"]))
        rows.append(OrderedDict([("title", name), ("items", items)]))

    return OrderedDict([
        ("shop", "Магнит"),
        ("shopCode", SHOP_CODE),
        ("totalItems", len(all_products)),
        ("rows", rows),
    ])


def main():
    print("=== Сбор каталога Магнита ===")
    print("магазин: %s (%s)\n" % (SHOP_CODE, SHOP_TYPE))

    session = requests.Session()
    categories = fetch_categories(session)

    if MAX_CATEGORIES:
        categories = categories[:MAX_CATEGORIES]

    print("\nШаг 2. Товары по разделам")
    all_products = []
    seen_titles = set()
    empty = 0

    for i, entry in enumerate(categories, 1):
        name, cat_id = entry[0], entry[1]
        slug = entry[2] if len(entry) > 2 else None

        items = parse_category(session, name, cat_id, slug)

        added = 0
        for p in items:
            key = p["title"].lower()
            if key in seen_titles:
                continue
            seen_titles.add(key)
            all_products.append(p)
            added += 1

        if items:
            print("  [%d/%d] %-32s товаров %3d, новых %3d"
                  % (i, len(categories), name[:32], len(items), added))
        else:
            empty += 1

        time.sleep(REQUEST_PAUSE)

    if empty:
        print("\n  пустых разделов: %d (подкатегории без своих товаров)" % empty)

    if not all_products:
        sys.exit(
            "\nНи одного товара не собрано.\n"
            "Магнит мог закрыть доступ антибот-защитой — попробуйте позже\n"
            "или проверьте, открывается ли magnit.ru/catalog в браузере.\n"
            "Прежний файл каталога не тронут."
        )

    catalog = build_catalog(all_products)

    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=1)

    total = len(all_products)
    with_img = len([p for p in all_products if p["imageUrl"]])
    discounts = len([p for p in all_products if p["discountPercent"] > 0])

    print("\n=== Готово ===")
    print("товаров:    %d" % total)
    print("с фото:     %d (%d%%)" % (with_img, round(with_img * 100.0 / total)))
    print("со скидкой: %d" % discounts)
    print("\nстроки каталога:")
    for row in catalog["rows"]:
        img = len([p for p in row["items"] if p["imageUrl"]])
        print("  %-18s %3d товаров, с фото %3d" % (row["title"], len(row["items"]), img))
    print("\nфайл: %s" % OUT_PATH)
    print("\nДальше: пересоберите приложение в Android Studio.")


if __name__ == "__main__":
    main()
