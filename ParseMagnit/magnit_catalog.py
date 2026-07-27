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
# Используется, если API недоступен.
FALLBACK_CATEGORIES = [
    ("Новинки", "66205"),
    ("Скидки", "63319"),
    ("Готовая еда", "65055"),
    ("Мясо и рыба", "66551"),
    ("Молочное", "63963"),
    ("Сладкое", "64697"),
    ("Фан-зона", "114540"),
    ("Только у нас", "107738"),
    ("Заморозка", "64467"),
]

# Порядок строк на экране телевизора
CATEGORY_ORDER = [
    "Скидки", "Молочное", "Мясо и рыба", "Готовая еда",
    "Заморозка", "Сладкое", "Фан-зона", "Новинки", "Только у нас",
]

REQUEST_PAUSE = 1.5


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

def fetch_categories(session):
    """Категории из мобильного API. При сбое — запасной список."""
    print("Шаг 1. Категории каталога")
    # мобильному API нужны свои заголовки, иначе отвечает 400
    api_headers = dict(HEADERS)
    api_headers.update({
        "Accept": "application/json",
        "x-client-name": "magnit",
        "x-device-platform": "Web",
        "x-device-id": "shoptv-parser",
        "x-app-version": "2026.6.24-17.37",
        "x-new-magnit": "true",
        "Referer": "https://magnit.ru/catalog",
    })
    try:
        r = session.get(TILES_URL, params=PARAMS, headers=api_headers, timeout=20)
        if r.status_code == 200:
            services = r.json().get("services", [])
            result = []
            for s in services:
                action = s.get("action", "")
                name = s.get("text")
                # ловим и category/123, и hidden-category/123
                m = re.search(r"(?:hidden-)?category/(\d+)", action)
                if name and m:
                    result.append((name, m.group(1)))
            if result:
                print("  получено из API: %d категорий" % len(result))
                return result
        print("  API вернул %s, берём запасной список" % r.status_code)
    except Exception as e:
        print("  API недоступен (%s), берём запасной список" % e)
    return list(FALLBACK_CATEGORIES)


def fetch_slugs(session):
    """
    Карта {id_категории: slug} со страницы каталога.

    Адрес категории имеет вид /catalog/63963-testmmmolochnyy_prilavok.
    Заглушка "-category" срабатывает не всегда: часть разделов отдаёт 404,
    поэтому берём настоящие slug'и из вёрстки каталога.
    """
    print("Шаг 1b. Адреса категорий")
    try:
        r = session.get(CATALOG_URL, params=PARAMS, headers=HEADERS, timeout=20)
        if r.status_code != 200:
            print("  HTTP %s — обойдёмся заглушкой" % r.status_code)
            return {}
        found = re.findall(r"/catalog/(\d+)-([a-z0-9_]+)", r.text)
        slugs = {}
        for cat_id, slug in found:
            slugs.setdefault(cat_id, slug)
        print("  найдено адресов: %d" % len(slugs))
        return slugs
    except Exception as e:
        print("  не удалось получить (%s) — обойдёмся заглушкой" % e)
        return {}


def parse_category(session, name, cat_id, slugs):
    """Возвращает список товаров категории со всеми полями."""
    # сначала настоящий адрес, затем заглушка как запасной вариант
    candidates = []
    slug = slugs.get(cat_id)
    if slug:
        candidates.append("https://magnit.ru/catalog/%s-%s" % (cat_id, slug))
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
        print("       HTTP %s на %s" % (resp.status_code, url.split("/")[-1]))

    if r is None:
        print("       категория недоступна — пропускаем")
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

    ordered = [c for c in CATEGORY_ORDER if c in by_category]
    ordered += [c for c in by_category if c not in CATEGORY_ORDER]

    for name in ordered:
        items = sorted(by_category[name], key=lambda p: (-p["discountPercent"], p["title"]))
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
    slugs = fetch_slugs(session)

    print("\nШаг 2. Товары по категориям")
    all_products = []
    seen_titles = set()

    for i, (name, cat_id) in enumerate(categories, 1):
        print("  [%d/%d] %s" % (i, len(categories), name))
        items = parse_category(session, name, cat_id, slugs)

        added = 0
        for p in items:
            key = p["title"].lower()
            if key in seen_titles:
                continue
            seen_titles.add(key)
            all_products.append(p)
            added += 1

        with_img = len([p for p in items if p["imageUrl"]])
        print("       найдено %d, новых %d, с фото %d" % (len(items), added, with_img))
        time.sleep(REQUEST_PAUSE)

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
