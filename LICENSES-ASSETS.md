# Лицензии ресурсов

Все сторонние ресурсы — только под свободными лицензиями (раздел «Указания для Claude Code» SPEC.md).
Полные тексты лицензий лежат в папке `licenses/`.

## Шрифты (SIL Open Font License 1.1)

| Файл в `app/src/main/res/font/` | Шрифт | Авторы | Источник | Лицензия |
| --- | --- | --- | --- | --- |
| `playfair_sc_regular.ttf`, `playfair_sc_bold.ttf` | Playfair Display SC | The Playfair Display Project Authors (Claus Eggers Sørensen), 2017 | [google/fonts: ofl/playfairdisplaysc](https://github.com/google/fonts/tree/main/ofl/playfairdisplaysc) | [OFL 1.1](licenses/OFL-PlayfairDisplaySC.txt) |
| `old_standard_regular.ttf`, `old_standard_bold.ttf`, `old_standard_italic.ttf` | Old Standard TT | The Old Standard Project Authors (Alexey Kryukov), 2011 | [google/fonts: ofl/oldstandardtt](https://github.com/google/fonts/tree/main/ofl/oldstandardtt) | [OFL 1.1](licenses/OFL-OldStandardTT.txt) |
| `pt_mono.ttf` | PT Mono | ParaType Ltd., 2010–2011 | [google/fonts: ofl/ptmono](https://github.com/google/fonts/tree/main/ofl/ptmono) | [OFL 1.1](licenses/OFL-PTMono.txt) |

Файлы шрифтов не изменялись. Кириллица (все буквы, «Ё», «№», кавычки-ёлочки) проверена в каждом файле.

## Графика

Вся графика нарисована для проекта, чужие изображения не используются:

- значок приложения (`ic_launcher_*.xml`) и иконки вкладок и действий (`ic_*.xml`) — векторные рисунки проекта;
- манометр, рычаг, вентиль, тумблер, шестерни, трубы, заклёпки — рисуются кодом в Compose Canvas;
- текстуры дерева и пергамента генерируются кодом при первом запуске (`TextureGenerator`).

## Звуки

Все звуки синтезированы скриптом `tools/gen_sounds.py` из этого репозитория (синусы и шум, без чужих записей),
поэтому принадлежат проекту и сторонних лицензий не требуют. Перегенерировать: `python tools/gen_sounds.py`.

| Файл в `app/src/main/res/raw/` | Что звучит | Этап |
| --- | --- | --- |
| `notify_whistle.ogg` | Паровой свисток — звук уведомлений о сроках замены | 2 |
| `sfx_click.ogg` | Короткий металлический щелчок — кнопки, вентили, тумблеры | 3 |
| `sfx_lever.ogg` | Ход рычага, треск храповика и колокольчик — рычаг «Заменено» | 3 |
| `sfx_steam.ogg` | Шипение пара (0,75 с) — сохранение | 3 |
| `sfx_clang.ogg` | Глухой лязг — удаление | 3 |
| `sfx_drum_tick.ogg` | Тихий щелчок барабана — каждый шаг цифры при вводе пробега | 3 |
| `sfx_sync_start.ogg` | Раскрутка шестерён — начало синхронизации | 3 (звучит с этапа 4) |
| `sfx_sync_ok.ogg` | Двойной звонок — синхронизация прошла | 3 (звучит с этапа 4) |
| `sfx_sync_error.ogg` | Низкий гудок — ошибка синхронизации | 3 (звучит с этапа 4) |

Все файлы — OGG Vorbis, моно, 44,1 кГц, до 50 КБ, пики не выше −3 dBFS, громкость выровнена по RMS.

Голос механика — системный синтезатор речи Android (TextToSpeech): файлов с записями в приложении нет.
