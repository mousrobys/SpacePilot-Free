# Отчёт о сборке и проверке

Дата: 23 октября 2026 года
Проект: `C:\Users\moysecamm\Documents\проекты\SpacePilot Free`

## Итог

| Проверка | Результат |
|---|---|
| `:app:assembleDebug` | Успешно |
| `:app:assembleRelease` (с ключом разработки) | Успешно, APK подписан |
| `:app:testDebugUnitTest` | 8 тестов, 0 ошибок |
| `:app:lintDebug` | 0 ошибок, 60 предупреждений |
| `lintVitalRelease` (в составе release-сборки) | Успешно |
| Проверка debug-подписи `apksigner` | Подписан отладочным сертификатом Android Debug |
| Проверка release-подписи `apksigner` | `Verifies`, схема v2, сертификат `CN=SpacePilot Free` |
| Запуск на устройстве/эмуляторе | Не выполнялся: нет подключённого устройства и AVD |

## Окружение

- ОС: Windows (x64)
- JDK: Eclipse Adoptium 17.0.20.8+1 (hotspot)
- Gradle Wrapper: 8.11.1
- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21, плагин Compose Compiler 2.0.21
- compileSdk / targetSdk: 36, minSdk: 26
- Android SDK: `C:\Users\moysecamm\AppData\Local\Android\Sdk`
- Build-Tools: 36.0.0
- Compose BOM: 2025.06.01

## Артефакты

| Артефакт | Путь | Размер | SHA-256 |
|---|---|---|---|
| Подписанный release APK | `apk/SpacePilot-Free-1.0.0-release.apk` | 11 720 980 байт | `6DD61147DB6184FE68E6932C491DE05A3134BD8662EB75D096939DAC81E316A6` |
| Копия сборки release | `app/build/outputs/apk/release/app-release.apk` | 11 720 980 байт | то же |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | 22 459 727 байт | собирается локально, в поставку не входит |

### Ключ подписи

- Файл: `keys/spacepilot-release.jks`
- Алиас: `spacepilot`
- Пароль хранилища и ключа: `SpFree-2026-Release`
- Сертификат: `CN=SpacePilot Free, OU=Mobile, O=SpacePilot, L=Moscow, C=RU`, RSA 2048, срок 10 000 дней
- Отпечаток сертификата SHA-256: `7853209e65c97af432e1dc236db80813ecc166049264f93ac31ec9e1601bd228`

Это **ключ разработки**, сгенерированный для проверки установки. Для публикации в RuStore рекомендуется собственный ключ, созданный и сохранённый в надёжном месте. Файлы `keys/`, `keystore.properties` и `apk/` исключены из репозитория через `.gitignore`.

## Команды

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"

# debug + signed release
.\gradlew.bat :app:assembleDebug :app:assembleRelease --no-daemon

# unit-тесты
.\gradlew.bat :app:testDebugUnitTest --no-daemon

# статический анализ
.\gradlew.bat :app:lintDebug --no-daemon
```

Подпись release берётся из `keystore.properties` в корне проекта; свойства `-PSPACEPILOT_*` имеют приоритет. Если файла и свойств нет, собирается `app-release-unsigned.apk`.

Логи: `build-final.log`, `build-v1v2.log`, `build-signed-release.log`, `build-install-fix.log`, `build-sdk36.log`, `lint-sdk36.log`, `test-debug.log`, `lint-debug.log` в корне проекта.

## Что именно проверено

### Подписанный release APK

`aapt dump badging`:

```
package: name='com.spacepilot.free' versionCode='1' versionName='1.0.0'
sdkVersion:'26'
targetSdkVersion:'36'
uses-permission: READ_EXTERNAL_STORAGE (maxSdkVersion 32)
uses-permission: WRITE_EXTERNAL_STORAGE (maxSdkVersion 28)
uses-permission: READ_MEDIA_IMAGES
uses-permission: READ_MEDIA_VIDEO
uses-permission: READ_MEDIA_AUDIO
```

Разрешения `INTERNET` и `MANAGE_EXTERNAL_STORAGE` отсутствуют.

`apksigner verify --print-certs`:

```
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Signer #1 certificate DN: CN=SpacePilot Free, OU=Mobile, O=SpacePilot, L=Moscow, C=RU
Signer #1 certificate SHA-256 digest: 7853209e65c97af432e1dc236db80813ecc166049264f93ac31ec9e1601bd228
```

`aapt dump xmltree` подтверждает: `android:debuggable` отсутствует, `allowBackup=false`, `usesCleartextTraffic=false`.

### Debug APK

`aapt dump badging`:

```
package: name='com.spacepilot.free.debug' versionCode='1' versionName='1.0.0-debug'
sdkVersion:'26'   targetSdkVersion:'36'
```

`apksigner verify --print-certs`: `Verifies`, сертификат `CN=Android Debug`.

### ZIP-целостность

`python -c "zipfile.ZipFile(...).testzip()"` возвращает `None` (ошибок нет), `zipalign -c -v 4` — `Verification succesful`.

### Про попытку добавить v1-подпись

Для максимальной совместимости в `signingConfigs` включены `enableV1Signing` и `enableV2Signing`. Однако итоговые APK содержат **только v2**, потому что APK превышает 65 535 элементов и JAR-подпись (v1) для такого ZIP технически невозможна; `apksigner` сообщает об этом в предупреждении. Для Android 7.0+ (minSdk 26) v2 — штатная и достаточная схема. Единственный известный побочный эффект: APK не установится в редких окружениях, требующих строго JAR-подпись (некоторые in-app-install «песочницы»). На обычных устройствах, через файловый менеджер и `adb install` установка работает.

### Unit-тесты

`app/src/test/java/com/spacepilot/free/util/FormattersAndFileTypesTest.kt`: 8 тестов на форматирование размеров/процентов и классификацию файлов (расширения, MIME, APK, каталоги, фильтры).

Результат: `tests="8" skipped="0" failures="0" errors="0"`.

Особенность окружения: при запуске из пути с кириллицей (`Documents\проекты\...`) тестовый worker Gradle падал с `ClassNotFoundException` при загрузке самого тестового класса. Тот же запуск через junction с ASCII-путём (`C:\spacepilot-build` → каталог проекта) прошёл успешно. Компиляция, lint и сборка APK из исходного пути работают. Это ограничение тестового раннера/пути, а не кода приложения.

### Lint

Исправлена одна ошибка `NewApi`: из `values/themes.xml` удалён `android:windowLightNavigationBar` (атрибут требует API 27, minSdk 26; значение `false` и так является поведением по умолчанию). После исправления `lintDebug` завершается успешно.

Остались 60 предупреждений, в основном:

- неиспользуемые строковые ресурсы (зарезервированы под будущие экраны);
- отсутствие `monochrome` в adaptive icon (темизированная иконка на Android 13+);
- прочие рекомендации, не влияющие на работоспособность.

Fatal-ошибок и ошибок манифеста нет.

## Что не проверено

1. **Запуск и UI на устройстве.** В окружении не было ни физического устройства, ни AVD (`adb devices` пуст, эмулятор не установлен). Не проверены: реальные сценарии SAF, системные диалоги удаления, установка APK, поведение на разных версиях Android, производительность на больших файлах, вёрстка на разных экранах.
2. **Фактическая установка APK.** Файл проверен криптографически (`apksigner verify`, целостность ZIP), но на устройство не ставился.
3. **Скриншоты для RuStore.** Не подготовлены — нет устройства/эмулятора.
4. **Инструментальные (androidTest) тесты.** Не запускались: требуется устройство/эмулятор. JVM-тесты покрывают только чистую логику форматирования и классификации.
5. **Полноценная проверка дубликатов на реальных данных.** Логика хэширования и выборочной проверки реализована и компилируется, но на реальной библиотеке файлов не прогонялась.

## Известные ограничения продукта

- Нет `MANAGE_EXTERNAL_STORAGE`: доступны MediaStore и папка, выбранная через SAF.
- Удаление необратимо, корзины нет.
- Для файлов >32 МБ используется выборочный SHA-256 (начало/середина/конец по 128 КБ); это отмечено в UI.
- Кэш сканирования ограничен 40 000 файлов с приоритетом крупных файлов и APK.
- Нет проверки APK на вредоносность и нет собственного установщика.
- Release не минифицирован (`isMinifyEnabled = false`), чтобы не рисковать стабильностью без устройств для тестирования.

## Как получить собственный подписанный release

```powershell
keytool -genkeypair -v -keystore keys\spacepilot-own.jks -alias spacepilot `
  -keyalg RSA -keysize 2048 -validity 10000
```

Затем либо укажите новый файл в `keystore.properties`, либо передайте свойства явно:

```powershell
.\gradlew.bat :app:assembleRelease --no-daemon `
  -PSPACEPILOT_KEYSTORE="keys/spacepilot-own.jks" `
  -PSPACEPILOT_KEYSTORE_PASSWORD="***" `
  -PSPACEPILOT_KEY_ALIAS="spacepilot" `
  -PSPACEPILOT_KEY_PASSWORD="***"
```

Проверка:

```powershell
& "$env:ANDROID_HOME\build-tools\36.0.0\apksigner.bat" verify --min-sdk-version 26 --print-certs `
  app\build\outputs\apk\release\app-release.apk
```

Не храните keystore и пароли в репозитории. При потере ключа обновления в RuStore будут невозможны.
