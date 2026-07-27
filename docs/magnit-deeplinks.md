# Deep links приложения Магнит

Разобрано из APK `ru.tander.magnit` 8.109.0 (versionCode 1395709):
`AndroidManifest.xml` и enum
`ru.tander.magnit.magnit_core.core.deeplink.data.DeepLink` — 140 маршрутов
с полным списком поддерживаемых query-параметров.

Это публичный интерфейс: приложение само объявляет схемы в манифесте,
любое стороннее приложение вправе их вызывать.

## Корзина

Маршруты корзины существуют, но **параметров не принимают**:

```
magnit://dostavka/delivery/express/basket    DELIVERY_EXPRESS_CART
magnit://dostavka/multicart                  DELIVERY_MULTICART
```

Оба объявлены с пустым списком queries (`new String[0]`), то есть просто
открывают экран корзины. Положить в неё товары ссылкой нельзя: корзина
серверная, привязана к сессии, наполняется запросами с токеном.

## Товар — с прицелом в корзину

```
magnit://product?catalogType=&storeCode=&storeType=&targetCart=&source=
```

Ключевой параметр — **`targetCart`**. Судя по названию, указывает, в какую
корзину класть товар (у Магнита их несколько: доставка, экспресс,
маркет — отсюда `multicart`). Открывает карточку товара сразу с нужным
контекстом.

Точную семантику надо проверять на устройстве: как минимум открывает
карточку, максимум — подставляет корзину для кнопки «В корзину».

Альтернатива, работающая всегда:

```
https://magnit.ru/product/{id}-{slug}?catalogType=&shopType=&shopCode=
```

App Link с `autoVerify="true"`: перехватывается приложением, при его
отсутствии открывается в браузере. Это поле `detailUrl` в нашем каталоге.

## Списки товаров

```
magnit://catalog/listing?title=&filters=&category=&term=
```

`filters` принимает JSON. В самом APK есть готовая константа:

```
magnit://catalog/listing?filters=[{"id":"onlyDiscount","value":"true"}]&title=Товары со скидкой
```

То есть можно открыть **произвольную выборку товаров** с заголовком.
Стоит проверить, принимает ли `filters` фильтр по списку id товаров —
если да, это ближайший аналог «передать корзину».

Параметр `term` — поисковый запрос, тоже способ показать нужные позиции.

```
magnit://dostavka/*?title=&storeCode=&chainId=&filters=&category=&term=
magnit://alreadyBought?catalogType=&storeType=      «уже покупали»
magnit://favorites?catalogType=&storeType=&storeCode=
```

## Каталог

```
magnit://catalog
magnit://catalog/category?isPromo=&source_category_id=
magnit://catalog/hidden-category?title=
magnit://catalog/good
magnit://category/*?storeType=&storeCode=&catalogType=&source=
magnit://dynamicCategory/*?catalogType=&storeType=&storeCode=
magnit://discounts?category_id=&is_from_today=
```

Готовые константы из APK:

```
magnit://discounts/?is_from_today=true
magnit://discounts/?is_from_today=true&category_id=151
```

## Веб-страница с сессией

```
magnit://web?url=
magnit://webWithAuth?url=&in_app=
```

`webWithAuth` открывает страницу **внутри приложения с авторизацией
пользователя**. Если у magnit.ru найдётся URL, наполняющий корзину, —
это рабочий путь к передаче списка.

## Прочее

```
magnit://loyaltyCard?showSbp=     magnit://pricechecker
magnit://store-search             magnit://favorites
magnit://orders/detail/*          magnit://profile/history_order
magnit://recommendations?screen_type=&catalog_type=&service=&goods_id=
magnit://recipes/                 magnit://online-journal
magnit://market/*?category_id=&sku_id=&sku_group_id=
magnit://cosmetic/*               magnit://migom
```

## План для ShopTV

1. **Один товар** — `https://magnit.ru/product/{id}-{slug}`, уже работает.
2. **Проверить `targetCart`** у `magnit://product` — возможно, кладёт
   товар в конкретную корзину.
3. **Проверить `filters`** у `magnit://catalog/listing` — если принимает
   список id, получим «открыть весь список» одной ссылкой.
4. **Запасной вариант** — QR-код на телефон со списком товаров.

Пункты 2 и 3 требуют проверки на реальном устройстве с установленным
приложением Магнита: `adb shell am start -a android.intent.action.VIEW -d "..."`

## Граница

В APK есть приватный API и антифрод Group-IB (`ru.fp.sdk`,
`services.antifraud.facct`, заголовки `x-gib-*`), плюс блокировка VPN.
Deep links используем — это публичный контракт. Приватную авторизацию
не трогаем: технически хрупко и нарушает условия использования.
