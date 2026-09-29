# LPH Android — Firebase Cloud Messaging

Ta paczka jest przygotowana dla repozytorium `Thomson-19/LPH`.

## Co zawiera

- Firebase Cloud Messaging przez Firebase BoM.
- Google Services plugin.
- `google-services.json` dla `pl.lph.app`.
- kanały:
  - `LPH — Artykuły`,
  - `LPH — Wyścigi`,
  - `LPH — Aktualizacje`.
- automatyczne subskrypcje topiców:
  - `lph_articles`,
  - `lph_races`,
  - `lph_updates`.
- zgodę `POST_NOTIFICATIONS` na Androidzie 13+.
- `FirebaseMessagingService`.
- obsługę URL z powiadomienia i otwieranie go w istniejącym WebView.
- logowanie tokenu FCM pod tagiem `LPH-FCM` do pierwszych testów.

## Instalacja

Rozpakuj paczkę do katalogu głównego repozytorium LPH i pozwól nadpisać wskazane pliki.
Następnie wykonaj Gradle Sync i uruchom aplikację.

W Logcat ustaw filtr:

`LPH-FCM`

Szukaj wpisu:

`FCM TOKEN: ...`

Token można użyć w Firebase Console do wysłania testowego powiadomienia na konkretne urządzenie.

## Dane przyszłych wiadomości z backendu

Backend LPH powinien docelowo wysyłać `data`:

- `eventId`
- `type`
- `category`
- `title`
- `body`
- `url`

`url` jest otwierany w WebView tylko wtedy, gdy prowadzi do domeny LPH.
