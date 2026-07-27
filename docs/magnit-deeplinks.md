# Deep links приложения Магнит

Разобрано из APK `ru.tander.magnit` версии 8.109.0 (versionCode 1395709):
`AndroidManifest.xml` и класс
`ru.tander.magnit.magnit_core.core.deeplink.data.DeepLink`.

Это публичный интерфейс: приложение само объявляет схемы в манифесте,
любое стороннее приложение вправе их вызывать.

## Главный вывод

**Маршрута корзины не существует.** Ни `magnit://cart`, ни варианта
с параметрами. Передать собранный список товаров в приложение одной
ссылкой нельзя — такого контракта Магнит не предоставляет.

Корзина у них серверная, привязана к сессии авторизованного
пользователя (`GET /webgate/v1/carts/lite` возвращает объект с UUID),
и наполняется только запросами с токеном.

## Что можно открыть

### Товар

```
magnit://product
magnit://catalog/good
https://magnit.ru/product/{id}-{slug}      ← App Link, autoVerify
```

HTTPS-ссылка на товар перехватывается приложением автоматически
(`autoVerify="true"`), а при его отсутствии открывается в браузере.
Это самый надёжный способ: работает всегда.

Так же обрабатываются `cosmetic.magnit.ru/product`,
`apteka.magnit.ru/product`, `mm.ru/product`.

### Каталог

```
magnit://catalog
magnit://catalog/category
magnit://catalog/hidden-category
magnit://catalog/listing
magnit://category
magnit://dynamicCategory
magnit://dostavka                          ← раздел доставки
magnit://discounts?category_id=&is_from_today=
```

### Произвольная веб-страница

```
magnit://web?url={url}
magnit://webWithAuth?url={url}&in_app={bool}
```

`webWithAuth` открывает страницу **внутри приложения с сессией
пользователя**. Это единственная лазейка в сторону авторизованных
действий: если бы у magnit.ru был URL вида «добавить товары в корзину»,
через него это сработало бы. Такого URL мы не нашли.

### Прочее

```
magnit://loyaltyCard?showSbp=      карта лояльности
magnit://pricechecker              сканер цен
magnit://store-search              поиск магазинов
magnit://recommendations           рекомендации
magnit://personalpromotions/promo  персональные предложения
magnit://recipes/                  рецепты
magnit://clubs/products/*          клубы (алко, зоо, родители, красота)
magnit://magnitPay/                оплата
magnit://settings
```

## Как это использовать в ShopTV

Пользователь собирает корзину у нас, а оформляет в Магните:

1. **Один товар** — `https://magnit.ru/product/{id}-{slug}` из поля
   `detailUrl` каталога. Открывается и в приложении, и в браузере.
2. **Список** — QR-код на экране телевизора, пользователь сканирует
   телефоном и добавляет товары в своём приложении, где уже авторизован.

Вводить логин пультом не нужно, наши данные никуда не уходят.

## Чего делать не стоит

В APK есть приватный API и антифрод Group-IB
(`ru.tander.magnit.services.antifraud.facct`, заголовки `x-gib-*`).
Вытаскивать оттуда механизм авторизации и притворяться официальным
приложением — нарушение условий использования и технически хрупко:
подписи меняются, токены протухают, любое обновление ломает интеграцию.

Deep links используем, приватную авторизацию — нет.
