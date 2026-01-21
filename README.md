# PromoDisplay

Стажировка PromoDisplay. Приложение для Android TV для федеральной сети салонов красоты. Оно работает на приставке с Android 13, подключённой к нескольким телевизорам, и воспроизводит рекламные видеоролики в непрерывном цикле. Приложение загружает управляемый сервером плейлист, скачивает медиафайлы локально и сохраняет историю воспроизведения для последующей загрузки в аналитику.

## Goals

- Auto-start on boot for unattended operation.
- Poll the server for playlist/config updates on a dynamic interval.
- Download assets with retries, resume support, and parallelism.
- Play available videos in order with priority and repeat counts.
- Show a fallback welcome screen or asset when no videos are available.
- Track playback counts and timestamps for analytics.
- Handle low storage by evicting assets per a server policy.

## API Overview

- `GET /config` returns playlist items, polling interval, and policies.
- `GET /media/{assetId}` serves assets with `Content-Length` and `Range`.
- `POST /analytics` accepts batched playback events.

## Tech Stack

- Kotlin, Jetpack Compose (BOM), Material3
- Retrofit + OkHttp
- Coroutines + Flow
- Media3 (ExoPlayer + UI)
- Room
- WorkManager
- Tracer (apptracer.ru)
