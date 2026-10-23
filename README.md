# SpacePilot Free

Локальный офлайн-инструмент для анализа памяти Android-устройства: обзор занятого места, поиск больших файлов, поиск дубликатов по содержимому (SHA-256), файловый менеджер через Storage Access Framework и раздел APK. Интерфейс на Jetpack Compose (Material 3), русский язык по умолчанию, английский — дополнительный.

Приложение не имеет разрешения `INTERNET`, не содержит рекламы, аналитики, телеметрии, регистрации и подписок. Все операции выполняются на устройстве.

## Статус сборки

| Артефакт | Путь | Размер | SHA-256 |
|---|---|---|---|
| Подписанный release APK (для установки) | `apk/SpacePilot-Free-1.0.0-release.apk` | 11 720 980 байт | `6DD61147DB6184FE68E6932C491DE05A3134BD8662EB75D096939DAC81E316A6` |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | 22 459 727 байт | — |

Release APK подписан ключом `keys/spacepilot-release.jks` (v2-схема) и устанавливается на устройство. SHA-256 release-файла: `6DD61147DB6184FE68E6932C491DE05A3134BD8662EB75D096939DAC81E316A6`. Параметры подписи по умолчанию лежат в `keystore.properties`; их можно переопределить свойствами `-PSPACEPILOT_*`.

> Ключ в `keys/` создан для разработки. Для публикации в RuStore сгенерируйте собственный ключ и не теряйте его — без него обновления выпустить нельзя. Файлы `keystore.properties`, `keys/` и `apk/` добавлены в `.gitignore`.

8 JVM-тестов проходят, `lintDebug` — 0 ошибок (60 предупреждений). Подробности и ограничения: `docs/BUILD_REPORT.md`.

## Возможности

- **Обзор хранилища** — общий/занятый/свободный объём, диаграмма, категории файлов, последний скан и история операций.
- **Сканирование** — MediaStore (изображения, видео, аудио) и выбранная пользователем папка через SAF. Прогресс, отмена, отчёт о недоступных путях; результат кэшируется локально.
- **Большие файлы** — фильтры 100 МБ / 500 МБ / 1 ГБ, сортировка по размеру, имени, дате; множественное удаление.
- **Дубликаты** — группировка по размеру, затем SHA-256. Файлы до 32 МБ хэшируются полностью; более крупные — по трём сегментам (начало/середина/конец по 128 КБ) с явной пометкой «выборочная проверка». Выбор лишних копий и удаление.
- **Файловый менеджер** — навигация по выбранной папке, поиск, фильтр по типу, сортировка, информация о файле, открытие, копирование и перемещение.
- **APK** — список APK из результатов сканирования, сведения о пакете (имя, версия, SDK), запуск системного установщика. Приложение не устанавливает APK в обход системы.
- **Безопасное удаление** — подтверждение, для SAF используется `DocumentsContract.deleteDocument`, для MediaStore — системный запрос `MediaStore.createDeleteRequest` на Android 11+; на старых версиях возможен запрос через `RecoverableSecurityException`. Никаких обходов системных ограничений.
- **Настройки** — тёмная (по умолчанию), светлая, системная тема; русский/английский/системный язык; управление доступом к папке; очистка истории и сброс; сведения о приложении и политика конфиденциальности.

## Требования

- Android 8.0 (API 26) и выше, targetSdk 36 (Android 16).
- JDK 17, Android SDK Platform 36, Build-Tools 36.0.0.
- Интернет нужен только для первой загрузки зависимостей Gradle.

## Сборка

Из корня проекта (Windows):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
.\gradlew.bat :app:assembleDebug --no-daemon
```

Готовый файл: `app/build/outputs/apk/debug/app-debug.apk`.

JVM-тесты и lint:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Если тестовый worker падает с `ClassNotFoundException` из-за кириллицы в пути, создайте junction с ASCII-путём и запустите тесты через него:

```powershell
cmd /c mklink /J C:\spacepilot-build "C:\Users\<user>\Documents\проекты\SpacePilot Free"
cd C:\spacepilot-build
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

Подробности: `docs/BUILD_REPORT.md`.

Release-сборка подписывается автоматически параметрами из `keystore.properties` (файл в `.gitignore`). Если файла нет и свойства `-PSPACEPILOT_*` не заданы, на выходе будет `app-release-unsigned.apk`.

Пример с явными свойствами:

```powershell
.\gradlew.bat :app:assembleRelease --no-daemon `
  -PSPACEPILOT_KEYSTORE="keys/spacepilot-release.jks" `
  -PSPACEPILOT_KEYSTORE_PASSWORD="***" `
  -PSPACEPILOT_KEY_ALIAS="spacepilot" `
  -PSPACEPILOT_KEY_PASSWORD="***"
```

Файл `local.properties` уже содержит путь к SDK. Свойство `android.overridePathCheck=true` в `gradle.properties` добавлено из-за кириллицы в пути проекта.

## Установка APK

Готовый подписанный файл: `apk/SpacePilot-Free-1.0.0-release.apk`.

```powershell
& "C:\Users\<user>\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "apk\SpacePilot-Free-1.0.0-release.apk"
```

Или скопировать APK на устройство и открыть, разрешив установку из неизвестных источников.

Debug-версия (`app-debug.apk`) имеет applicationId `com.spacepilot.free.debug` и может стоять рядом с release-версией:

```powershell
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
```

Если система пишет «пакет недействителен»: скачайте APK заново и сверьте SHA-256, а также удалите ранее установленную версию с тем же пакетом.

## Структура проекта

```
app/src/main/java/com/spacepilot/free/
  MainActivity.kt, SpacePilotApplication.kt
  data/            репозитории: MediaStore, SAF, хэши/анализ, удаление, кэш, настройки
  data/model/      модели данных
  ui/              навигация SpacePilotApp.kt
  ui/screens/      Home, Scan, LargeFiles, Duplicates, FileManager, Apk, Results, Settings
  ui/components/   общие компоненты, списки, диалоги, диаграмма
  ui/theme/        тема Material 3
  util/            форматирование, типы файлов, разрешения, локаль, интенты
  viewmodel/       AppViewModel
docs/              материалы для RuStore, политика конфиденциальности, отчёт о сборке
```

## Ограничения

- Приложение не запрашивает `MANAGE_EXTERNAL_STORAGE`: доступ к файлам ограничен MediaStore и папкой, выбранной через SAF. Полный обход файловой системы недоступен по правилам Android.
- В корзину файлы не помещаются; удаление необратимо после подтверждения (для MediaStore — после системного диалога).
- «Выборочный SHA-256» для файлов >32 МБ теоретически может не отличить файлы с совпадающими размерами и сегментами, но разным содержимым между ними. Это явно показывается в интерфейсе.
- Кэш сканирования ограничен 40 000 файлов; при большем количестве приоритет отдаётся крупным файлам и APK.
- APK не проверяются на вредоносность; установка выполняется системным установщиком.
- Сборка проверена компиляцией, JVM-тестами, lint и статическим анализом APK. Запуск на реальном устройстве/эмуляторе не выполнялся: в окружении сборки не было ни подключённого устройства, ни AVD. См. `docs/BUILD_REPORT.md`.
- Для установки доступна только v2-подпись (v1 не помещается из-за ограничения ZIP-формата для больших APK). На Android 7+ это штатный вариант; специальные проверки JAR-подписи такие APK отвергают.

## Материалы для публикации

- `docs/RUSTORE.md` — готовые данные для RuStore: название, все описания, разрешения, техданные, чек-лист загрузки.
- `docs/PRIVACY_POLICY_RU.md` — политика конфиденциальности (русский).
- `docs/PRIVACY_POLICY_EN.md` — privacy policy (English).
- `docs/store/icon-512.png` — иконка 512×512 для магазина.
- `docs/BUILD_REPORT.md` — отчёт о сборке и проверке.
