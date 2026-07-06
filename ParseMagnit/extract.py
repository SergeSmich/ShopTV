import json

# Предположим, вы сохранили файл из браузера как magnit.har
har_file_path = "magnit.har" 

try:
    with open(har_file_path, "r", encoding="utf-8") as f:
        har_data = json.load(f)

    webgate_urls = set()

    for entry in har_data["log"]["entries"]:
        url = entry["request"]["url"]
        if "/webgate/v1/" in url:
            # Отрезаем параметры, оставляя только чистый путь
            clean_url = url.split("?")[0]
            webgate_urls.add(clean_url)

    print("Найденные пути в вашей сессии:")
    for url in sorted(webgate_urls):
        print(url)

except FileNotFoundError:
    print(f"Сначала сохраните файл {har_file_path} в эту папку.")