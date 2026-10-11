# Del Health для Android

Обёртка над сайтом (WebView) + мост к Health Connect. Подробнее про данные здоровья: [HEALTH_INTEGRATION.md](HEALTH_INTEGRATION.md).

## Сборка

1. Установите Android Studio (Ladybug или новее), откройте эту папку — студия сама скачает Gradle и SDK.
2. Адрес сайта задан в `app/build.gradle.kts` (`SITE_URL`). Для локального Flask в эмуляторе: `http://10.0.2.2:5000/`
   (и временно `usesCleartextTraffic="true"` в манифесте).
3. Run ▶ на эмуляторе или телефоне (Android 9+). Health Connect встроен в Android 14+, на более старых ставится из Play.

Релиз: Build → Generate Signed Bundle / APK. Ключ подписи (`*.jks`) не коммитьте.
