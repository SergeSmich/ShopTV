import json
import requests
from bs4 import BeautifulSoup
import time
import csv

PARAMS = {"shopCode": "781225", "shopType": "express"}
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "ru-RU,ru;q=0.9"
}

def unbox(value, raw_data):
    if isinstance(value, int) and 0 <= value < len(raw_data):
        linked = raw_data[value]
        if isinstance(linked, (dict, list)):
            return unbox_structure(linked, raw_data)
        return linked
    return value

def unbox_structure(element, raw_data):
    if isinstance(element, dict):
        return {k: unbox(v, raw_data) for k, v in element.items()}
    elif isinstance(element, list):
        return [unbox(x, raw_data) for x in element]
    return element

print("=== ШАГ 1: Автоматический сбор всех категорий каталога ===")
main_url = "https://magnit.ru/catalog"
web_categories = {}

try:
    res = requests.get(main_url, params=PARAMS, headers=HEADERS, timeout=15)
    if res.status_code == 200:
        soup = BeautifulSoup(res.text, "html.parser")
        nuxt_script = soup.find("script", id="__NUXT_DATA__")
        if nuxt_script:
            raw_main_data = json.loads(nuxt_script.string)
            
            for item in raw_main_data:
                # Ищем объекты в кэше Nuxt, описывающие категории каталога
                if isinstance(item, dict) and "slug" in item and "id" in item and "name" in item:
                    unboxed_cat = unbox_structure(item, raw_main_data)
                    cat_name = unboxed_cat.get("name")
                    cat_id = unboxed_cat.get("id")
                    cat_slug = unboxed_cat.get("slug")
                    
                    # Исключаем служебные или пустые названия
                    if cat_name and cat_id and cat_slug and isinstance(cat_name, str) and len(cat_name) > 2:
                        # Защита от дубликатов по slug (берем наиболее короткий/чистый путь)
                        url = f"https://magnit.ru/catalog/{cat_id}-{cat_slug}"
                        if cat_name not in web_categories:
                            web_categories[cat_name] = url

            print(f"[УСПЕХ] Робот обнаружил {len(web_categories)} уникальных разделов каталога.")
except Exception as e:
    print(f"[ОШИБКА] Не удалось просканировать карту сайта: {e}")

if not web_categories:
    print("[КРИТИЧЕСКАЯ ОШИБКА] Не удалось собрать разделы. Проверьте соединение или параметры.")
    exit()

print("\n=== ШАГ 2: Глобальный сбор акций и скидок по всем разделам ===")
all_discount_products = []
processed_count = 0

for cat_name, url in web_categories.items():
    processed_count += 1
    print(f"[{processed_count}/{len(web_categories)}] Сканируем: '{cat_name}'")
    
    try:
        response = requests.get(url, params=PARAMS, headers=HEADERS, timeout=12)
        if response.status_code != 200:
            continue
            
        soup = BeautifulSoup(response.text, "html.parser")
        nuxt_script = soup.find("script", id="__NUXT_DATA__")
        
        if not nuxt_script:
            continue
            
        raw_data = json.loads(nuxt_script.string)
        cat_count = 0
        
        for item in raw_data:
            if isinstance(item, dict) and ('price' in item or 'regularPrice' in item) and ('title' in item or 'name' in item):
                unboxed = unbox_structure(item, raw_data)
                
                title = unboxed.get("title") or unboxed.get("name")
                price_raw = unboxed.get("price")
                old_price_raw = unboxed.get("oldPrice")
                
                if title and price_raw:
                    try:
                        price = float(price_raw)
                        old_price = float(old_price_raw) if old_price_raw else price
                        
                        # Регистрируем только товары с явной скидкой
                        if old_price > price:
                            discount_rub = round(old_price - price, 2)
                            discount_pct = round((discount_rub / old_price) * 100)
                            
                            all_discount_products.append({
                                "Раздел каталога": cat_name,
                                "Наименование товара": title,
                                "Цена по акции": price,
                                "Старая цена": old_price,
                                "Скидка (%)": discount_pct,
                                "Экономия (руб)": discount_rub,
                                "В наличии (шт)": unboxed.get("quantity", 0)
                            })
                            cat_count += 1
                    except ValueError:
                        continue
                        
        if cat_count > 0:
            print(f"       -> Найдено акций в разделе: {cat_count}")
        
        # Небольшая пауза, чтобы не перегружать сервер Магнита запросами
        time.sleep(1.2)
        
    except Exception as e:
        print(f"       [Пропуск] Ошибка обработки раздела: {e}")

# Очистка итоговой базы от возможных пересечений товаров
unique_products = {p["Наименование товара"]: p for p in all_discount_products}.values()
final_list = list(unique_products)

# Сортировка всей базы по величине скидки (от -99% вниз)
final_list.sort(key=lambda x: x["Скидка (%)"], reverse=True)

# Запись результатов в файлы
with open("magnit_global_discounts.json", "w", encoding="utf-8") as f:
    json.dump(final_list, f, indent=4, ensure_ascii=False)

csv_file = "все_скидки_магнита.csv"
if final_list:
    with open(csv_file, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=final_list[0].keys(), delimiter=";")
        writer.writeheader()
        writer.writerows(final_list)

print("\n" + "="*75)
print(" ГЛОБАЛЬНЫЙ МОНИТОРИНГ ЗАВЕРШЕН!")
print(f" Всего на сайте проверено категорий: {len(web_categories)}")
print(f" Найдено и структурировано уникальных скидок: {len(final_list)}")
print(f" Итоговый отчет выгружен в Excel-таблицу: {csv_file}")
print("="*75)