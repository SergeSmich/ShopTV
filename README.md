# 🛒 ShopTV — Android TV приложение для покупок в супермаркетах

[![Android](https://img.shields.io/badge/Android-TV-green)](https://developer.android.com/tv)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)](https://kotlinlang.org/)
[![Leanback](https://img.shields.io/badge/Leanback-1.0-orange)](https://developer.android.com/training/tv/start/layouts)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

**ShopTV** — Android TV клиент для онлайн-покупок в супермаркетах. Использует API, извлечённое из декомпиляции нативных приложений магазинов.

> ⚠️ **Дисклеймер:** Проект предназначен для изучения API и прототипирования. Используется публичный API, который приложения сами объявляют в AndroidManifest. Авторизация и приватные эндпоинты не используются.

---

## 📱 Скриншоты

```
┌─────────────────────────────────────────────────┐
│  🛒 Магнит — 194 товара                🔍      │
├──────────┬──────────────────────────────────────┤
│          │  Скидки дня                          │
│ Поиск    │  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │
│ Скидки   │  │ 📷  │ │ 📷  │ │ 📷  │ │ 📷  │   │
│ Молочное │  │99₽  │ │89₽  │ │54₽  │ │199₽ │   │
│ Мясо     │  └─────┘ └─────┘ └─────┘ └─────┘   │
│ Готовая  │                                      │
│ Заморозка│  Молочное                            │
│ Сладкое  │  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │
│ Только у │  │ 📷  │ │ 📷  │ │ 📷  │ │ 📷  │   │
│ нас      │  │79₽  │ │129₽ │ │45₽  │ │89₽  │   │
│          │  └─────┘ └─────┘ └─────┘ └─────┘   │
└──────────┴──────────────────────────────────────┘
```

---

## 🏗 Архитектура

### Модульная структура

```
ShopTV/
├── app/                          # Android TV приложение (Leanback UI)
│   ├── presentation/
│   │   ├── main/                 # Главный экран (BrowseSupportFragment)
│   │   ├── search/               # Поиск (SearchSupportFragment)
│   │   └── common/               # ProductCardPresenter
│   └── di/                       # Koin модули приложения
│
├── core/
│   ├── model/                    # UnifiedProduct, StoreType
│   ├── network/                  # NetworkModule (Retrofit, OkHttp)
│   └── common/                   # Result<T>, утилиты
│
├── feature/
│   ├── magnit/                   # 🟢 Магнит (активный)
│   │   ├── data/
│   │   │   ├── MagnitCatalogApi.kt    # POST /v2/goods/get
│   │   │   ├── MagnitCartApi.kt       # PUT /v2/carts/lite
│   │   │   ├── MagnitStoresApi.kt     # POST /v1/stores-facade/search/detail
│   │   │   ├── MagnitModels.kt        # 33 поля товара (CatalogBffListingGood)
│   │   │   └── MagnitRepository.kt    # Бизнес-логика + offline-фолбэк
│   │   ├── di/                        # Koin модуль
│   │   └── assets/
│   │       └── magnit_catalog.json    # Offline-слепок каталога
│   │
│   ├── lenta/                    # 🔴 Лента (заготовка)
│   ├── perekrestok/              # 🔴 Перекрёсток (заготовка)
│   └── cart/                     # 🔴 Корзина (заготовка)
│
├── docs/
│   ├── magnit-api-map.md         # Полная карта API (80+ эндпоинтов)
│   └── magnit-deeplinks.md       # Deep links (140 маршрутов)
│
└── ParseMagnit/
    └── magnit_catalog.py         # Парсер каталога с сайта magnit.ru
```

### Стек технологий

| Технология | Версия | Назначение |
|------------|--------|------------|
| Kotlin | 2.0 | Основной язык |
| Android SDK | 35 (min 23) | Платформа |
| Leanback | 1.0.0 | TV UI |
| Retrofit | 2.11.0 | HTTP-клиент |
| OkHttp | 4.12.0 | Сетевой слой |
| Kotlin Serialization | 1.7.1 | JSON |
| Coil | 2.7.0 | Загрузка изображений |
| Koin | 3.5.6 | Dependency Injection |
| Coroutines | 1.8.1 | Асинхронность |

---

## 🔌 API Магнита

### Источник

API извлечено из декомпиляции APK `ru.tander.magnit` версии 8.109.0 (versionCode 1395709) с помощью jadx. Модели данных находятся в пакете `ru.tander.models.gateway.*`.

### Базовые URL

```
web-gateway.middle-api.magnit.ru    — каталог товаров
middle-api.magnit.ru                — корзина, заказы, профиль
magnit.ru/webgate                   — веб-гейт (категории, тайлы)
```

### Хедеры запросов

```kotlin
// Обязательные для всех запросов
"x-client-name"     to "magnit"
"x-device-platform" to "Android"
"x-device-id"       to "tv-shop-001"
"x-app-version"     to "8.109.0"
"Content-Type"      to "application/json"
"Accept"            to "application/json"

// Для авторизованных запросов
"Authorization"     to "Bearer {token}"
```

### Эндпоинты

#### Каталог товаров

```http
POST /v2/goods/get
Content-Type: application/json

{
  "catalogType": "main",
  "storeCode": "781225",
  "storeType": "express",
  "pagination": {
    "limit": 36,
    "offset": 0
  }
}
```

**Ответ:**
```json
{
  "containerConfig": { "title": "Каталог" },
  "items": [
    {
      "id": "1000178487",
      "name": "Молоко 3.2% 1л",
      "price": 8999,                    // в КОПЕЙКАХ!
      "gallery": [
        { "type": "image", "url": "https://..." }
      ],
      "promotion": {
        "isPromotion": true,
        "oldPrice": 12999,              // в копейках
        "discountPercent": 30
      },
      "ratings": { "rating": 4.8, "reviewsCount": 156 },
      "badges": [{ "type": "hit", "text": "Хит" }],
      "quantity": 42,
      "isInFavorites": false,
      "storeCode": "781225",
      "catalogType": "main"
      // ... ещё 20+ полей
    }
  ],
  "pagination": {
    "hasMore": true,
    "limit": 36,
    "offset": 0,
    "totalCount": 500,
    "nextOffset": 36
  }
}
```

#### Корзина

```http
PUT /v2/carts/lite?storeCode=781225&service=express
Content-Type: application/json

{
  "items": [
    {
      "goodId": "1000178487",
      "offerId": "1000178487",
      "qnty": 1,
      "goodStoreCode": "781225",
      "goodService": "express",
      "createdFromScreen": "tv_catalog"
    }
  ]
}
```

#### Поиск магазинов

```http
POST /v1/stores-facade/search/detail
Content-Type: application/json

{
  "filters": {
    "cityFiasId": "...",
    "storeTypeListV2": ["express"],
    "geo": {
      "latitude": 59.93,
      "longitude": 30.31,
      "radius": 5000
    }
  },
  "pagination": { "limit": 20, "offset": 0 },
  "sorting": { "field": "distance", "order": "asc" }
}
```

#### Авторизация

```http
POST /v1/auth/cross-token
```

### Типы сервисов (CartsDtoServiceEnum)

```
express            — Магнит у дома (экспресс-доставка)
dostavka           — Магнит Доставка
market             — Маркет
cosmetic           — Магнит Косметик
cosmetic_darkstore — Косметик даркстор
apteka             — Аптека
rte                — Готовая еда (Ready To Eat)
undecodable        — неопределён
```

### Deep Links

```kotlin
// Товар в приложении Магнита
https://magnit.ru/product/{id}-{slug}?shopCode={code}&shopType={type}

// Товар (deep link)
magnit://product?catalogType=&storeCode=&storeType=&targetCart=&source=

// Каталог с фильтрами
magnit://catalog/listing?filters=[{"id":"onlyDiscount","value":"true"}]&title=Скидки

// Корзина
magnit://dostavka/delivery/express/basket

// Примеры
magnit://catalog/category?isPromo=&source_category_id=
magnit://discounts/?is_from_today=true
magnit://favorites?catalogType=&storeType=&storeCode=
magnit://store-search
magnit://orders/detail/{id}
```

Полный список: [docs/magnit-deeplinks.md](docs/magnit-deeplinks.md)

### Вся карта API

Полный реестр 80+ эндпоинтов: [docs/magnit-api-map.md](docs/magnit-api-map.md)

---

## 🚀 Быстрый старт

### Требования

- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17
- Android SDK 35
- Android TV эмулятор (API 23+) или реальное устройство

### Установка

```bash
# Клонировать репозиторий
git clone https://github.com/SergeSmich/ShopTV.git
cd ShopTV
git checkout arena/019fa7b5-shoptv

# Открыть в Android Studio
# File → Open → выбрать папку ShopTV

# Запустить
# Run → Run 'app' (выбрать Android TV эмулятор)
```

### Обновление offline-каталога

```bash
cd ParseMagnit
pip install requests beautifulsoup4
python magnit_catalog.py
# Результат: feature/magnit/src/main/assets/magnit_catalog.json
```

---

## 🧩 Модель данных товара

Товар из API содержит **33 поля** (из `CatalogBffListingGood`):

```kotlin
data class MagnitGood(
    val id: String,                        // ID товара
    val name: String,                      // Название
    val price: Int?,                       // Цена в КОПЕЙКАХ
    val gallery: List<MagnitMedia>,        // Изображения
    val promotion: MagnitPromotion,        // Акция/скидка
    val quantity: Int,                     // Остаток на складе
    val storeCode: String,                 // Код магазина
    val weighted: MagnitWeighted,          // Весовой товар
    val ratings: MagnitRatings?,           // Рейтинг
    val badges: List<MagnitBadge>?,        // Бейджи
    val catalogType: String?,              // Тип каталога
    val isInFavorites: Boolean?,           // В избранном
    val productId: String?,                // Product ID
    val seoCode: String?,                  // Для deep link
    val targetCart: String?,               // Целевая корзина
    // ... и ещё 18 полей
)
```

**Важно:** все цены в API приходят в **копейках** (Int). Для отображения делим на 100.

---

## 🔧 Для AI-ассистентов

### Ключевые файлы

| Файл | Назначение |
|------|-----------|
| `feature/magnit/data/MagnitCatalogApi.kt` | API каталога (запрос/ответ) |
| `feature/magnit/data/MagnitModels.kt` | 33 поля товара + вложенные типы |
| `feature/magnit/data/MagnitCartApi.kt` | API корзины + enum сервисов |
| `feature/magnit/data/MagnitStoresApi.kt` | API магазинов + авторизация |
| `feature/magnit/data/MagnitRepository.kt` | Бизнес-логика, конвертация |
| `feature/magnit/di/MagnitModule.kt` | Koin DI |
| `app/presentation/main/MainFragment.kt` | Главный экран TV |
| `app/presentation/search/SearchFragment.kt` | Поиск |
| `docs/magnit-api-map.md` | Полная карта API |
| `docs/magnit-deeplinks.md` | Deep links |

### Паттерны

- **Конвертация:** `MagnitGood` → `UnifiedProduct` (унифицированная модель)
- **Фолбэк:** API → offline-слепок (assets JSON) → пустой экран
- **DI:** Koin модули (magnitModule, appModule)
- **UI:** Leanback BrowseSupportFragment + ListRow + Presenter

### Как добавить новый маркет

1. Создать `feature/{name}/data/{Name}Api.kt` — Retrofit интерфейс
2. Создать `feature/{name}/data/{Name}Models.kt` — модели данных
3. Создать `feature/{name}/data/{Name}Repository.kt` — бизнес-логика
4. Создать `feature/{name}/di/{Name}Module.kt` — Koin модуль
5. Добавить `include(":feature:{name}")` в `settings.gradle.kts`
6. Добавить `implementation(project(":feature:{name}"))` в `app/build.gradle.kts`
7. Зарегистрировать модуль в `ShopTvApplication.kt`

---

## 📋 Roadmap

Смотрите [ROADMAP.md](ROADMAP.md) для полного плана развития.

### Краткий план

1. **Фаза 2** — Стабилизация Магнита (авторизация, выбор магазина, корзина)
2. **Фаза 3** — Мульти-маркет (Лента, Перекрёсток)
3. **Фаза 4** — Список покупок (Room, синхронизация)
4. **Фаза 5** — AI-функционал (голос, рекомендации, автоматизация)

---

## 📄 Лицензия

MIT License. См. [LICENSE](LICENSE).

---

## 🙏 Благодарности

- Декомпиляция APK выполнена с помощью [jadx](https://github.com/skylot/jadx)
- UI построен на [Android Leanback](https://developer.android.com/training/tv/start/layouts)
- Парсинг каталога — Python + BeautifulSoup
