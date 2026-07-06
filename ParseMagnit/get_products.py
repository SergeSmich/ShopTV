import json
import requests
from bs4 import BeautifulSoup
import time
import csv

# Базовые параметры для express-доставки
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

all_discount_products = []

print("=== ШАГ 1: Получаем актуальную карту каталога сайта ===")
main_url = "https://magnit.ru/catalog"
web_categories = {}

try:
    res = requests.get(main_url, params=PARAMS, headers=HEADERS, timeout=10)
    if res.status_code == 200:
        soup = BeautifulSoup(res.text, "html.parser")
        nuxt_script = soup.find("script", id="__NUXT_DATA__")
        if nuxt_script:
            raw_main_data = json.loads(nuxt_script.string)
            # Ищем все ссылки на категории в кэше главной страницы
            for item in raw_main_data:
                if isinstance(item, dict) and "slug" in item and "id" in item and "name" in item:
                    unboxed_cat = unbox_structure(item, raw_main_data)
                    cat_name = unboxed_cat.get("name")
                    cat_id = unboxed_cat.get("id")
                    cat_slug = unboxed_cat.get("slug")
                    
                    if cat_name and cat_id and cat_slug:
                        # Формируем идеальный URL, который сайт ТОЧНО примет
                        web_categories[cat_name] = f"https://magnit.ru/catalog/{cat_id}-{cat_slug}"
                        
            print(f"Успешно найдено {len(web_categories)} живых веб-категорий!")
except Exception as e:
    print(f"Не удалось собрать меню сайта автоматически: {e}")

# Запасной вариант на случай, если главная заблокирует (подставим вашу проверенную ссылку)
if not web_categories:
    print("Используем ручные проверенные ссылки...")
    web_categories = {
        "Птица, мясо, рыба": "https://magnit.ru/catalog/64243-testmmptitsa_myaso_ryba",
        "Молочное": "https://magnit.ru/catalog/63963-testmmmolochnyy_prilavok"
    }

print("\n=== ШАГ 2: Запуск сбора товаров ===")

for cat_name, url in web_categories.items():
    print(f"Сканируем категорию: '{cat_name}'...")
    print(f"  URL: {url}")
    
    try:
        response = requests.get(url, params=PARAMS, headers=HEADERS, timeout=10)
        if response.status_code != 200:
            print(f"       [Ошибка {response.status_code}] Пропускаем.")
            continue
            
        soup = BeautifulSoup(response.text, "html.parser")
        nuxt_script = soup.find("script", id="__NUXT_DATA__")
        
        if not nuxt_script:
            print("       [Ошибка] На странице нет данных Nuxt.")
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
                        
                        if old_price > price:
                            discount_rub = round(old_price - price, 2)
                            discount_pct = round((discount_rub / old_price) * 100)
                            
                            all_discount_products.append({
                                "Категория": cat_name,
                                "Товар": title,
                                "Цена со скидкой": price,
                                "Старая цена": old_price,
                                "Скидка (%)": discount_pct,
                                "Выгода (руб)": discount_rub,
                                "Остаток": unboxed.get("quantity", 0)
                            })
                            cat_count += 1
                    except ValueError:
                        continue
                        
        print(f"       Успешно найдено акций: {cat_count}")
        time.sleep(1.5)
        
    except Exception as e:
        print(f"       [Ошибка] Сбой при обработке: {e}")

# Очистка дубликатов и сохранение
unique_products = {p["Товар"]: p for p in all_discount_products}.values()
final_list = list(unique_products)
final_list.sort(key=lambda x: x["Скидка (%)"], reverse=True)

with open("magnit_all_discounts.json", "w", encoding="utf-8") as f:
    json.dump(final_list, f, indent=4, ensure_ascii=False)

csv_file = "таблица_скидок_магнит.csv"
if final_list:
    with open(csv_file, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=final_list[0].keys(), delimiter=";")
        writer.writeheader()
        writer.writerows(final_list)

print("\n" + "="*70)
print(f" СБОР ЗАВЕРШЕН! Уникальных скидок сохранено: {len(final_list)}")
print(f" Результат ждет вас в файле: {csv_file}")
print("="*70)