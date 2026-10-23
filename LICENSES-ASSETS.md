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

Звуки действий (щелчки, рычаг, пар, удар) появятся на этапе 3.
