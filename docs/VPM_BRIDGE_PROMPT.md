# Задача для VPM: read-only мост телеметрии для кокпита DashGrid

> Этот файл — **контракт и задача только для VPM**. Если сессия должна проверить оба
> приложения, выпустить оба релиза и подтвердить совместимость, используйте
> [`VPM_COMPAT_PROMPT.md`](VPM_COMPAT_PROMPT.md): он охватывает VPM и DashGrid вместе.

Работать только в репозитории **VAG Performance Monitor** (`io.github.nchalexz.aaperfmon`).
DashGrid не менять. VPM остаётся единственным владельцем ELM327 и VAG UDS; задача — отдать уже
собранную телеметрию другому приложению, не открывая второе OBD-соединение.

Контракт ниже — версия 1. Он уже реализован на стороне DashGrid (`net.prototux.aare.vpm`), включая
парсер, состояния и тесты. Менять authority, permission, имена ключей и смысл полей нельзя без
согласования: DashGrid проверяет `version == 1`.

## Зачем

В DashGrid есть предустановленный рабочий стол «Автомобиль» с **нативным кокпитом DashGrid**: три
прибора, пять плиток и сетка категорий, нарисованные по макетам VPM 0.4.1. Кокпит не лезет в OBD и
не копирует `CircularGauge`/`MetricTile`; он читает последний снимок состояния VPM через маленький
read-only IPC. Пока в VPM нет провайдера, кокпит показывает карточку «VPM установлен, но эта версия
не отдаёт телеметрию» — без фальшивых цифр.

## Контракт v1

### ContentProvider

- authority: `io.github.nchalexz.aaperfmon.telemetry`
- exported: `true`
- только чтение; `android:readPermission`:
  `io.github.nchalexz.aaperfmon.permission.READ_TELEMETRY`
- `protectionLevel="normal"` (DashGrid подписан другим ключом; `signature` не подойдёт)
- метод: `call("snapshot", null, null)` → `Bundle` или `null`
- `query`, `insert`, `update`, `delete` не поддерживаются; `getType` → `null`
- `call` с другим method → `null`
- в `call` явно проверить `context.enforceCallingOrSelfPermission(READ_TELEMETRY, ...)`, даже если
  `android:readPermission` уже стоит: защита от изменений в OEM-прошивках

Манифест VPM:

```xml
<permission
    android:name="io.github.nchalexz.aaperfmon.permission.READ_TELEMETRY"
    android:protectionLevel="normal" />

<uses-permission
    android:name="io.github.nchalexz.aaperfmon.permission.READ_TELEMETRY" />

<provider
    android:name=".telemetry.TelemetryProvider"
    android:authorities="io.github.nchalexz.aaperfmon.telemetry"
    android:exported="true"
    android:readPermission="io.github.nchalexz.aaperfmon.permission.READ_TELEMETRY" />
```

### Bundle снимка

| Ключ | Тип | Обязателен | Смысл |
|---|---|---|---|
| `version` | Int | да | сейчас `1` |
| `connection` | String | да | `demo`, `connecting`, `connected`, `disconnected`, `error` |
| `status` | String | да | человеческая строка VPM (`state.connectionLabel`, плюс `state.message`, если непустой) |
| `source` | String | нет | транспорт/адаптер (`Wi-Fi`, `USB`, `Bluetooth`) |
| `updatedAt` | Long | да | время последнего обновления значений, **не** время вызова; `0`, если данных ещё не было |
| `signals` | String | да | JSON-массив, максимум 128 элементов |

`connection` — `ConnectionKind.name.lowercase()`. `updatedAt` — максимум `timestampMs` среди
отданных сигналов; если таких нет, `0`.

### JSON сигнала

```json
{
  "id": "engine.coolant_c",
  "label": "ОЖ",
  "value": "86",
  "unit": "°C",
  "raw": 86.0,
  "stale": false,
  "warning": false,
  "category": "temperatures"
}
```

- `id` — стабильный `SignalDefinition.id` (например `engine.coolant_c`, `engine.boost_kpa`,
  `vehicle.speed`).
- `label` — подпись, которую VPM показывает на плитке (`MetricSpec.label(language)` или
  `definition.nameRu`).
- `value` — уже отформатированная строка **как в кокпите VPM** (`MetricSpec.format`,
  европейская запятая); `null`, если значения нет.
- `unit` — единица **отображения** (`MetricSpec.unit(language)`, для наддува `bar`, не `kPa`).
- `raw` — то же значение числом **в единицах отображения** (`raw * factor`, для наддува bar).
  Обязателен, если `value` не `null`: по нему DashGrid ставит стрелку и пороги. `null` допустим.
- `stale` — `SignalValue.isStale(now, definition)`; для отсутствующего значения `true`.
- `warning` — `MetricSpec.warningValue(raw * factor)`, то есть тот же порог, что у кокпита VPM.
- `category` — одна из `MetricRegistry.DisplayCategory`: `engine`, `temperatures`, `fuel`,
  `gearbox`, `turbo`, `diagnostics`.

### Порядок и категории

Массив строится **по категориям в порядке экрана VPM**: сначала все сигналы `engine` в порядке
`MetricRegistry.category(ENGINE)`, затем `temperatures`, `fuel`, `gearbox`, `turbo`, `diagnostics`.
Внутри категории — порядок VPM, чтобы сетка DashGrid совпадала с макетом.

Сигнал, который VPM показывает в двух вкладках, отдаётся **дважды** с разными `category`:
`engine.boost_kpa` есть и в Engine, и в Turbo; `engine.map_kpa` и `engine.baro_kpa` — тоже. Один и
тот же `(id, category)` дважды не отдавать. DashGrid дедуплицирует по паре и берёт первое
вхождение `id` для главного экрана.

### Значения и пороги

Использовать `MetricRegistry` VPM (`gauges` и `mainTiles` для главного экрана, `category(...)` для
сетки), а не сырые PID. Это гарантирует те же единицы, округление и пороги, что на экране VPM:

- `engine.coolant_c` — 40…120 °C, предупреждение с 105, критическое с 115;
- `engine.boost_kpa` — −1…2 bar, предупреждение с 1.75, критическое с 1.9;
- `engine.oil_c` — 40…150 °C, предупреждение с 130, критическое с 145;
- плитки — speed, rpm, gear, `fuel.l100_km`, `fuel.level_pct`;
- запасные источники (`vag.7e0.11be.oil_temp`, `vag.714.202f.oil_temp`, `vag.7e1.210f.gear`) отдавать
  как обычные сигналы: DashGrid сам выберет первый ответивший.

## Реализация в VPM

Ориентировочно:

- `app/src/main/java/io/github/nchalexz/aaperfmon/telemetry/TelemetrySnapshot.kt` — чистая сборка
  `Bundle`/JSON из `Telemetry.state.value` и `MetricRegistry`;
- `app/src/main/java/io/github/nchalexz/aaperfmon/telemetry/TelemetryProvider.kt` — `ContentProvider`,
  только `onCreate` и `call`;
- манифест — permission, uses-permission, provider;
- тесты — `TelemetrySnapshotTest`: JSON, `null`, `stale`, `warning`, категории, дубликат
  `(id, category)`, потолок 128, `connection`, `version`;
- документация — короткий `docs/DASHGRID_BRIDGE.md`: authority, permission, схема, пример
  `content call`, безопасность, версия;
- CHANGELOG и версия VPM (например 0.5.0 / versionCode 5), потому что появился публичный read-only
  API.

Провайдер должен:

1. В `onCreate` инициализировать только то, что уже инициализирует AppWidget/чтение настроек.
   **Не** вызывать `Telemetry.startService()`, `connect()`, `Elm327` и не опрашивать PID.
2. Брать **только** `Telemetry.state.value`; `StateFlow.value` читается мгновенно, JSON собирать на
   месте.
3. Возвращать `signals = []` и `updatedAt = 0`, если сервис ещё не работал; не падать.
4. Не отдавать историю, настройки, VIN, путь к адаптеру и любые записи; только текущие значения и
   статус.
5. Не менять `TelemetryWidgetProvider`, его настройки и обновление.
6. Не открывать `CockpitActivity` и не менять AA-сборку: DashGrid не запускает чужой экран.

Если VPM не запущен и состояние ещё `DEMO`, отдавать `connection = "demo"`: DashGrid покажет
плашку DEMO, а не выдаст это за живые данные. Не стартовать сервис из провайдера: запуск OBD из
чтения — неожиданный побочный эффект.

## Безопасность

- Провайдер экспортирован, но только read-only; никаких `insert/update/delete`.
- `normal`-разрешение означает, что прочитать может любое приложение, которое его объявит. Это
  осознанный компромисс: данные — текущие показания датчиков, без истории и без управления.
- `enforceCallingOrSelfPermission` в `call` обязателен.
- Не добавлять `grantUriPermissions`, file paths, `openFile`.
- Не экспортировать `CockpitActivity` и не менять AA-сборку.

## Проверка

1. `./gradlew test` и `assembleStandardDebug` (или актуальные задачи VPM) зелёные.
2. На устройстве/эмуляторе:

   ```sh
   adb shell content call \
     --uri content://io.github.nchalexz.aaperfmon.telemetry \
     --method snapshot
   ```

   Должен вернуться Bundle с `version=1`, `connection`, `updatedAt` и `signals`.
3. Открыть VPM, включить демо, повторить вызов: значения и `updatedAt` меняются, `stale`/`warning`
   соответствуют экрану VPM.
4. Закрыть VPM/остановить сервис, повторить вызов: `connection` не `connected`, `signals` пустые
   или старые, нового подключения ELM327 в логах нет.
5. Приложение без `<uses-permission ...READ_TELEMETRY>` получает `SecurityException`.
6. Существующий AppWidget VPM продолжает работать; `CockpitActivity` не экспортирована; AA-сборка
   не тронута.
7. В логах при чтении провайдера нет второго `ELM327`/`Obd`-соединения: соединение одно, его
   держит `TelemetryService`.

## Критерии приёмки

- [ ] Authority, permission и ключи Bundle совпадают с контрактом v1.
- [ ] `snapshot` отдаёт `version=1`, `connection`, `status`, `updatedAt`, `signals`.
- [ ] Каждый сигнал: `id`, `label`, `value`/`raw` в единицах отображения, `unit`, `stale`,
      `warning`, `category`.
- [ ] Порядок по категориям VPM; `(id, category)` не повторяется; дубликат между категориями есть.
- [ ] Провайдер не открывает OBD и не запускает сервис.
- [ ] Нет write-методов и истории.
- [ ] Тесты и сборка зелёные; `content call` проверен на устройстве.
- [ ] Документация и CHANGELOG обновлены; версия VPM поднята.

## Что не делать

- Не менять DashGrid и его кокпит.
- Не открывать второе OBD-соединение и не опрашивать адаптер из провайдера.
- Не портировать UI DashGrid в VPM и наоборот.
- Не экспортировать `CockpitActivity` и не трогать AA-сборку.
- Не добавлять запись, управление адаптером, настройки или историю в API.
- Не делать `protectionLevel="signature"`: DashGrid подписан другим ключом и не прочитает.
