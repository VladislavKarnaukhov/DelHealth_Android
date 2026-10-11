# Интеграция с приложениями здоровья

Приложение читает данные из **Health Connect** (Google Fit, Samsung Health, Garmin, Fitbit, Oura и др. пишут в него сами).
Сейчас читаются: шаги, сон, тренировки, пульс (по дням, до 30 дней). Только чтение.

## Как сайт обращается к приложению

Страницы сайта в приложении видят объект `window.DelHealthNative` (в обычном браузере его нет):

```js
if (window.DelHealthNative) {
  window.onDelHealthNative = (event, data) => {
    // event: "status" | "permission" | "summary" | "error"
    if (event === "status" && data.available && !data.granted) DelHealthNative.requestHealthAccess();
    if (event === "permission" && data.granted) DelHealthNative.getHealthSummary(7);
    if (event === "summary") console.log(data.days); // [{date, steps, sleep_minutes, exercise_minutes, heart_rate_avg}]
  };
  DelHealthNative.getHealthStatus();
}
```

`status.state`: `AVAILABLE`, `NEEDS_INSTALL_OR_UPDATE` (вызовите `installHealthConnect()`), `UNSUPPORTED`.
Мост доступен только страницам с хоста сайта; все чужие ссылки открываются во внешнем браузере.

## Серверная часть (готова, в репозитории сайта)

- `POST /api/health-sync/connect` `{consent: true}` — отдельное согласие, пишется в `health_connections` с версией политики.
- `POST /api/health-sync` `{days: [...]}` — до 31 дня, значения проверяются по диапазонам; день заменяется целиком. Нужны вход и заголовок `X-CSRF-Token`.
- Раздел «Данные из приложений здоровья» в `/account/settings` (JS — `static/js/health-sync.js`), отключение удаляет данные.
- Обновлены `/consent/health`, `/privacy`, `POLICY_VERSION` → `2026-10-11` (всем снова покажется баннер cookie).
- Данные пока только хранятся и показываются; в расчёт индекса и анкету не подставляются.

## Что осталось

- Подсказывать в анкете значения из шагов и сна (с подтверждением пользователя, не молча).
- Показ динамики шагов и сна графиком в кабинете.

## Перед публикацией в Google Play

Health Connect требует заполнить форму Health Apps declaration и политику конфиденциальности (адрес `/privacy`
уже подключён к экрану разрешений через `PermissionsRationaleActivity`). Запрашивайте только нужные типы данных.
