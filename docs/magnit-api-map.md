# Карта API Магнита

Извлечено из декомпиляции `ru.tander.magnit` 8.109.0 (jadx).
Полный реестр Retrofit-эндпоинтов, сгруппированных по домену.

**Легенда:**
- 🔑 — требует авторизации (токен / сессия)
- 🌐 — публичный (без авторизации)
- ⚡ — критично для TV-приложения
- 📦 — уже есть в ShopTV

---

## 1. Товары и каталог ⚡

```
POST /v2/goods/get                              📦 товары по магазину (магазинный API)
GET  /v3/categories/store/{store-code}           категории для конкретного магазина
POST /v1/goods/filters                           🔑 фильтры каталога
GET  /v1/articles                                🔑 статьи/описания
GET  /v1/brands/list                             🔑 список брендов
GET  /v1/brands/details                          🔑 детали бренда
GET  /v1/widgets/main                            🔑 виджеты главной страницы
GET  /v2/configs/mainpage                        🔑 конфигурация главной
```

### POST /v2/goods/get — детали

**Запрос** (`CatalogBffGoodsListGetGoodsV2Request`):
```json
{
  "catalogType": "string",     // тип каталога
  "storeCode": "781225",       // код магазина
  "storeType": "express",      // express | dostavka | market
  "cityId": "string|null",     // ID города
  "listId": 123|null,          // ID списка (избранное?)
  "pagination": {              // CatalogBffCatalogBFFLOPagination
    "limit": 36,
    "offset": 0
  }
}
```

**Ответ** (`CatalogBffGoodsListGetGoodsV2ResponseSuccess`):
```json
{
  "containerConfig": { "title": "..." },
  "items": [ ... CatalogBffListingGood ... ],
  "pagination": {
    "hasMore": true,
    "limit": 36,
    "offset": 0,
    "totalCount": 500,
    "nextOffset": 36
  }
}
```

**Товар** (`CatalogBffListingGood` — 33 поля):
```
Обязательные:
  gallery: List<CatalogBffMedia>   — изображения [{url, type}]
  id: String                       — ID товара
  name: String                     — НАЗВАНИЕ (не title!)
  price: Int                       — цена в КОПЕЙКАХ
  quantity: Int                    — остаток на складе
  storeCode: String                — код магазина
  promotion: CatalogBffPromotion   — {oldPrice: Int, title, type}
  weighted: CatalogBffWeighted     — {isWeighted, weight, unit}
  isBiometryRequired: Boolean
  isForAdults: Boolean

Опциональные:
  badges: List<CatalogBffBadge>    — [{type, text, color}]
  ratings: CatalogBffRatings       — {rating: Double, reviewsCount: Int}
  cashback: Int?                   — кэшбэк в копейках
  catalogType: String?
  fbs: Boolean?
  isInFavorites: Boolean?          — в избранном у пользователя
  isLowStock: Boolean?             — мало на складе
  pickupOnly: Boolean?             — только самовывоз
  productId: String?
  profit: Int?                     — выгода в копейках
  promoTag: String?
  seoCode: String?                 — для построения deep link
  service: String?
  serviceLabel: CatalogBffServiceLabel?
  skuGroupId: String?
  skuIds: List<String>?
  targetCart: String?              — целевая корзина (deep link)
  nearestDelivery: OffsetDateTime?
  nearestDeliveryDate: OffsetDateTime?
  needPassport: Boolean?
  orderProperties: CatalogBffOrderProperties?
  orders: Int?                     — количество заказов
  isAdditionalActionRequired: Boolean?
```

**Deep link товара:** `https://magnit.ru/product/{id}-{seoCode}?shopCode={storeCode}&shopType={storeType}`

**Навигация по страницам:** offset += limit, пока hasMore == true

**Что нужно для TV:**
- `POST /v2/goods/get` — уже работает через MagnitApiService
- `GET /v3/categories/store/{store-code}` — нужен для навигации по категориям
- `POST /v1/goods/filters` — для фильтрации на экране

---

## 2. Корзина ⚡🔑

```
PUT    /v2/carts/lite                             обновить/создать корзину (v2)
PUT    /v1/carts/lite                             обновить/создать корзину (v1)
DELETE /v1/carts/{cart_id}                        удалить корзину
DELETE /carts/v1/scango/my-cart                   удалить корзину Scan&Go
PUT    /v1/checkout/preview                       превью заказа перед оплатой
GET    /v1/checkout/preview/promo-codes           промокоды для заказа
DELETE /v1/checkout/{cart_id}/certificate         убрать сертификат
PUT    v2/orders/{id}/cart/edited                 редактирование корзины заказа
```

**Структура корзины (предположительно):**
- `PUT /v2/carts/lite` — добавление/обновление товаров в корзине
- `DELETE /v1/carts/{cart_id}` — очистка
- Корзина привязана к сессии/пользователю

---

## 3. Заказы 🔑

```
GET  /v1/orders/poll                              опрос статуса заказов
GET  /v2/returns/{id}                             возврат заказа
GET  /v1/checkout/notifications/list              уведомления о заказах
POST /checkout/v1/scango/order                    оформление Scan&Go заказа
PUT  /v2/orders/{id}/cart/edited                  редактирование корзины заказа
```

---

## 4. Избранное и списки товаров ⚡🔑

```
POST   /v1/goods/add                              добавить товар в список
POST   /v1/goods/delete                           удалить товар из списка
POST   /v1/favorites/brands                       добавить бренд в избранное
DELETE /v1/favorites/brands                       удалить бренд из избранного
```

**Важно:** `GoodsListsGoodsListAddGoodsRequest` — модель запроса для `addProduct`.

---

## 5. Магазины и локации ⚡🔑

```
POST /v1/stores-facade/search                     поиск магазинов
POST /v1/stores-facade/search/detail              детали магазина
GET  /v2/stores-facade/config                     конфигурация магазинов
POST /v2/stores-facade/config/by-geo              конфиг по геолокации
POST /v1/stores-facade/store-type-groups/by-config-type  типы магазинов
POST /market/v1/city/info                         информация о городе (v1)
POST /market/v2/city/info                         информация о городе (v2)
POST market/v1/shops/pvzs                         ПВЗ (пункты выдачи)
GET  /customer_addresses/v1/address_book          адреса доставки
DELETE /customer_addresses/v1/address_book/{id}   удалить адрес
POST /customer-stores/v1/favorite                 избранный магазин
GET  /v2/stores-facade/config                     конфиг магазинов
```

**Типы магазинов Магнита:**
- `express` — Магнит у дома (магазин у дома)
- `dostavka` — Магнит Доставка
- `market` — Маркет
- `cosmetic` — Магнит Косметик

---

## 6. Авторизация 🔑

```
POST /v1/auth/cross-token                         кросс-токен (обмен между сервисами)
POST v1/auth/token/refresh                        обновление токена
POST /v1/mobile-crypto-sdk/token                  крипто-токен EBS
POST /v1/profile/deactivate                       деактивация профиля
GET  /market/v1/auth/marketCustomer               авторизация в маркете
```

---

## 7. Профиль пользователя 🔑

```
PUT /v1/user/profile                              обновить профиль
PUT /v1/user/permissions                          обновить разрешения (v1)
GET /Cuscom/v2/user/permissions                   получить разрешения (v2)
PUT /Cuscom/v2/user/permissions                   обновить разрешения (v2)
GET /mymagnit/v1/soft-update                      мягкое обновление
POST /v1/invite-survey                            пригласительный опрос
POST /v1/user-interests/categories                интересы — категории
POST /v1/user-interests/subcategories             интересы — подкатегории
```

---

## 8. Промоакции 🔑

```
GET /v1/promotions/type/popular                   популярные акции
GET /v1/promotions/categories                     категории акций
GET /v1/promotions/city                           акции по городу
GET /v1/promotions/club-products                  товары клуба
GET /v1/promos/mp/{shopCode}/{promoId}?limit=10   промо конкретного магазина
GET /promosearcher/v1/search?limit=10             поиск промо
POST /v1/offers/club/cancel-offer/{code}          отмена клубного предложения
```

---

## 9. Рецепты и Журнал 🔑

```
GET  /v1/recipes                                  список рецептов
GET  /v1/recipes/{recipeId}                       конкретный рецепт
GET  /v1/recipes/categories                       категории рецептов
GET  /online-journal/v1/recipes-on-main-page-enable  рецепты на главной
POST /online-journal/v2/search-recipes            поиск рецептов
GET  /online-journal/v1/themed-tile/{id}          тематическая плитка
GET  /online-journal/v1/filters-page/{id}         страница фильтров
POST /online-journal/v1/publications/favorites/{id}  + избранное (публикация)
POST /online-journal/v1/recipes/favorites/{id}    + избранное (рецепт)
POST /online-journal/v1/selections/favorites/{id} + избранное (подборка)
DELETE (те же пути)                               - избранное
```

---

## 10. Отзывы 🔑

```
POST   /v1/listing/reviews/like                   лайк отзыва
DELETE /v1/listing/reviews/like                   убрать лайк
DELETE /v1/feedbacks/{feedback_id}/delete         удалить отзыв
DELETE /v1/reviews/{review_id}/delete             удалить отзыв
```

---

## 11. Scan & Go 🔑

```
POST   /checkout/v1/scango/order                  оформление заказа
DELETE /carts/v1/scango/my-cart                   удалить корзину
POST   /v1/scan-go-article                       артикул для сканирования
GET    /rte/v1/vendors/availability               доступность вендоров
```

---

## 12. Оплата 🔑

```
PUT    /v2/payment-methods/default                способ оплаты по умолчанию
DELETE /v2/payment-methods/{payment_method_id}    удалить способ оплаты
```

---

## 13. Уведомления 🔑

```
GET  /inbox/v1/total-unread                       непрочитанные
GET  /v1/checkout/notifications/list              уведомления о заказах
GET  /v1/popups                                   всплывающие окна
POST /Cuscom/v1/on-user-auth                     авторизация для пушей
POST Cuscom/v3/user-device/enroll                 регистрация устройства
```

---

## 14. Конфигурация 🌐

```
GET  Cuscom/v1/mobile-app/config                  конфиг мобильного приложения
GET  /configurator/v1/experiments?namespace=loyalty  A/B-тесты (лояльность)
```

---

## 15. Аналитика и события (не нужны для TV)

```
POST /v1/event                                    событие
POST /fsa/                                        FSA аналитика
POST /ad-facade/v1/event                          рекламное событие
POST /v1/animated-widget/record-view              просмотр виджета
POST /spotlights/v1/user-state/register-spotlights  регистрация спотлайтов
```

---

## 16. InAppStory SDK (не нужны для TV)

```
GET  v2/feed/{feed}                               фид историй
GET  v2/story                                     список историй
GET  v2/story/{id}                                конкретная история
POST v2/session/open                              открытие сессии
POST v2/session/close                             закрытие сессии
POST v2/session/update                            обновление сессии
POST v2/story-favorite/{id}                       избранное (история)
POST v2/story-like/{id}                           лайк (история)
POST /v2/game/{id}/launch                         запуск игры
PUT  v2/game/{id}/instance-user-data              данные пользователя в игре
POST /v2/game/{id}/logger                         логгер игры
GET  v2/game/preload                              предзагрузка игры
GET  v2/ugc/feed                                  UGC контент
GET  v2/ugc/story/{id}                            UGC история
GET  v2/ugc/editor-config                         конфиг редактора
GET  v2/story-share/{id}                          шаринг истории
PUT  v2/story-data/{id}                           данные истории
POST v2/game/{id}/logger                          логгер
POST exception                                    отправка ошибок
GET  stat/{event_name}                            статистика
GET  /v1/tenant/{tenant_id}/playlist              плейлист
```

---

## 17. Реклама (не нужны для TV)

```
GET  /ad-facade/v1/adv-info/{adv_uid}             информация о рекламе
POST /ad-facade/v1/event                          событие рекламы
GET  /v1/widgets/main                             виджеты
```

---

## Не найдено в списке (возможно в другом пакете)

- ❓ **Поиск товаров** — нет явного `/search` для товаров (есть только для рецептов)
- ❓ **Товар по ID** — нет `/goods/{id}` для карточки товара
- ❓ **Детали заказа** — нет `/orders/{id}`
- ❓ **История заказов** — нет `/orders/list` или `/orders/history`

Возможно эти эндпоинты проксируются через `/v2/goods/get` с разными параметрами,
или находятся в BFF-слое, который не виден как Retrofit-интерфейс.

---

## Базовые URL (из APK)

```
web-gateway.middle-api.magnit.ru    — основной API товаров
middle-api.magnit.ru                — корзина, заказы, профиль
magnit.ru/webgate                   — веб-гейт (tiles, категории)
```

## Хедеры запросов (из APK)

```
x-device-id         — ID устройства (DeviceRepository)
x-device-platform   — "Android"
x-app-version       — версия приложения (8.109.0)
x-platform-version  — Build.VERSION.SDK_INT
x-app-type          — "OMNI"
x-device-tag        — тег устройства
authorization       — "bearer {accessToken}" (нижний регистр!)
auth-phone-number   — номер телефона (из JWT)
x-request-sign      — HMAC-SHA512 подпись (см. ниже)
x-session-id        — ID сессии
user-agent          — User-Agent
```

## Подпись запросов (x-request-sign)

Алгоритм: **HmacSHA512**, цепочка (chained HMAC):

```
step1 = HMAC-SHA512(masterKey_bytes, platform.toLowerCase())
step2 = HMAC-SHA512(step1, appVersion.toLowerCase())
step3 = HMAC-SHA512(step2, deviceId.toLowerCase())
step4 = HMAC-SHA512(step3, phone)
step5 = HMAC-SHA512(step4, path.toLowerCase())
step6 = HMAC-SHA512(step5, accessToken.toLowerCase() ?? "")
step7 = HMAC-SHA512(step6, MD5(body))       // если есть body
result = toHexString(step7)                 // hex строка
```

- Все промежуточные значения в нижнем регистре
- Тело запроса сначала MD5, потом HMAC
- masterKey берётся из конфигурации приложения
- Есть LRU-кэш для step1-step4

## Авторизация

```
1. POST /v1/auth/cross-token → { "token": "..." }
2. Обмен token → { "accessToken": "...", "refreshToken": "..." }
3. Все запросы: authorization: bearer {accessToken}
4. При 401 → автоматическое обновление через refreshToken
```

---

## Стратегия для TV-приложения

### Фаза 1: Каталог (сейчас)
- [x] `POST /v2/goods/get` — товары
- [x] Offline-слепок как фолбэк
- [ ] `GET /v3/categories/store/{code}` — навигация по категориям
- [ ] `POST /v1/goods/filters` — фильтры

### Фаза 2: Корзина
- [ ] `PUT /v2/carts/lite` — добавление товаров
- [ ] `DELETE /v1/carts/{id}` — очистка
- [ ] `PUT /v1/checkout/preview` — превью заказа
- [ ] Авторизация (cross-token)

### Фаза 3: Магазины
- [ ] `POST /v1/stores-facade/search` — поиск магазинов
- [ ] `POST /v2/stores-facade/config/by-geo` — магазины по геолокации
- [ ] `POST /market/v2/city/info` — определение города

### Фаза 4: Профиль
- [ ] Авторизация (MagnitId)
- [ ] Избранное (`/v1/goods/add`, `/v1/goods/delete`)
- [ ] История заказов
- [ ] Рецепты и журнал
