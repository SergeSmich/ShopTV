import json
import requests

# Попробуем использовать официальный эндпоинт API
url = "https://magnit.ru/webgate/v1/goods/filters"

# Расширенные заголовки, чтобы сайт думал, что это реальный браузер Windows
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "application/json, text/plain, */*",
    "Accept-Language": "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
    "Content-Type": "application/json",
    "Origin": "https://magnit.ru",
    "Referer": "https://magnit.ru/",
    "Sec-Fetch-Dest": "empty",
    "Sec-Fetch-Mode": "cors",
    "Sec-Fetch-Site": "same-origin",
    "Connection": "keep-alive"
}

payload = {
    "client": "",
    "service": "",
    "correctQuery": False,
    "onlyAvailable": False,
    "onlyDiscount": False,
    "filters": [],
    "categoryIDs": [],
    "storeCodes": ["781225"],
    "catalogType": "2",
    "storeType": "express",
    "includeAdultGoods": True
}

print("Отправка запроса к API Магнита (с эмуляцией браузера)...")

try:
    # Увеличили timeout до 15 секунд
    response = requests.post(url, json=payload, headers=headers, timeout=15)
    
    if response.status_code == 200:
        data = response.json()
        print(" Успешно! Ответ получен.")
        with open("magnit_response.json", "w", encoding="utf-8") as f:
            json.dump(data, f, indent=4, ensure_ascii=False)
        print("Результат сохранен в 'magnit_response.json'")
    else:
        print(f" Ошибка сервера. Статус код: {response.status_code}")
        print("Ответ сервера:", response.text)

except Exception as e:
    print(f" Ошибка запроса: {e}")