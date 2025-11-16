# HeadDB Menu Configuration / Конфигурация меню HeadDB

## English

### Overview
This folder contains configuration files for customizing HeadDB menus. You can fully customize the appearance, layout, and text of all menus.

### Files
- `main.yml` - Main menu configuration ✅ **FULLY CONFIGURABLE**
- `category.yml` - Category menu examples/documentation
- `favorites.yml` - Favorites menu examples/documentation
- `local.yml` - Local heads menu examples/documentation
- `custom_categories.yml` - Custom categories menu examples/documentation
- `purchase.yml` - Purchase menu examples/documentation
- `search.yml` - Search results menu examples/documentation

**Note:** Full configuration support is currently implemented only for the main menu.
Other files serve as documentation showing possible settings.
Menu titles for all menus can be configured in `messages/` files.

### Features
1. **Full Menu Customization**: Change titles, sizes, button positions, and more
2. **Multi-language Support**: All text supports MiniMessage format for colors and formatting
3. **Flexible Layout**: Configure slot positions for all menu elements
4. **Custom Textures**: Use custom player head textures for buttons

### Configuration Format

#### Main Menu (main.yml)
```yaml
title: "<red>HeadDB"  # Menu title
size: 6  # Number of rows (1-6)
categorySlots: [11, 12, 13, ...]  # Slots for category buttons
buttons:
  local:
    enabled: true
    slot: 41
    texture: "..."  # Player head texture value
    material: "COMPASS"  # Fallback material
    name: "<aqua>Local Heads"
    lore: []
    permission: "headdb.category.local"
```

#### Category Menu (category.yml)
```yaml
size: 6
headSlots: [10, 11, 12, ...]  # Slots where heads are displayed
controls:
  back:
    slot: 45
    name: "<gold>◀ Back"
    lore: ["<gray>Go to previous page"]
```

#### Favorites Menu (favorites.yml)
```yaml
title: "<red>HeadDB <gray>» <gold>Favorites"
size: 6
headSlots: [10, 11, 12, ...]
emptyMessage:
  slot: 22
  name: "<red>No favorites"
```

#### Local Heads Menu (local.yml)
```yaml
title: "<red>HeadDB <gray>» <gold>Local Heads"
size: 6
headDisplay:
  showPlayerName: true
  showUUID: false
```

#### Purchase Menu (purchase.yml)
```yaml
title: "<red>HeadDB <gray>» <gold>{name} <gray>» <gold>Purchase"
size: 3
purchaseButtons:
  buy1:
    slot: 10
    name: "<green>Buy x1"
```

#### Search Menu (search.yml)
```yaml
title: "<red>HeadDB <gray>» <gold>Search <gray>» <gold>{query}"
size: 6
resultSlots: [10, 11, 12, ...]
```

#### Custom Categories Menu (custom_categories.yml)
```yaml
title: "<red>HeadDB <gray>» <gold>More Categories"
size: 6
categoryDisplay:
  showHeadCount: true
```

### Opening Menus for Other Players
Use the command: `/hdb open <menu> <player>`

Examples:
- `/hdb open Main Steve` - Opens main menu for Steve
- `/hdb open Animals Alex` - Opens Animals category for Alex

**Required Permission**: `headdb.command.open.others`

### Language Files
Language files are located in `messages/` folder:
- `en.yml` - English (default)
- `ru.yml` - Russian

To add a new language, copy `en.yml` and translate all values.

---

## Русский

### Обзор
Эта папка содержит файлы конфигурации для настройки меню HeadDB. Вы можете полностью настроить внешний вид, расположение и текст всех меню.

### Файлы
- `main.yml` - Конфигурация главного меню ✅ **ПОЛНОСТЬЮ НАСТРАИВАЕТСЯ**
- `category.yml` - Примеры/документация меню категорий
- `favorites.yml` - Примеры/документация меню избранного
- `local.yml` - Примеры/документация меню локальных голов
- `custom_categories.yml` - Примеры/документация меню дополнительных категорий
- `purchase.yml` - Примеры/документация меню покупки
- `search.yml` - Примеры/документация меню результатов поиска

**Примечание:** Полная поддержка конфигурации реализована только для главного меню.
Остальные файлы служат документацией, показывая возможные настройки.
Названия всех меню настраиваются в файлах `messages/`.

### Возможности
1. **Полная настройка меню**: Изменяйте названия, размеры, позиции кнопок и многое другое
2. **Поддержка нескольких языков**: Весь текст поддерживает формат MiniMessage для цветов и форматирования
3. **Гибкая компоновка**: Настраивайте позиции слотов для всех элементов меню
4. **Пользовательские текстуры**: Используйте пользовательские текстуры голов игроков для кнопок

### Формат конфигурации

#### Главное меню (main.yml)
```yaml
title: "<red>HeadDB"  # Название меню
size: 6  # Количество строк (1-6)
categorySlots: [11, 12, 13, ...]  # Слоты для кнопок категорий
buttons:
  local:
    enabled: true
    slot: 41
    texture: "..."  # Значение текстуры головы игрока
    material: "COMPASS"  # Резервный материал
    name: "<aqua>Локальные головы"
    lore: []
    permission: "headdb.category.local"
```

#### Меню категории (category.yml)
```yaml
size: 6
headSlots: [10, 11, 12, ...]  # Слоты, где отображаются головы
controls:
  back:
    slot: 45
    name: "<gold>◀ Назад"
    lore: ["<gray>Перейти на предыдущую страницу"]
```

#### Меню избранного (favorites.yml)
```yaml
title: "<red>HeadDB <gray>» <gold>Избранное"
size: 6
headSlots: [10, 11, 12, ...]
emptyMessage:
  slot: 22
  name: "<red>Избранное пусто"
```

#### Меню локальных голов (local.yml)
```yaml
title: "<red>HeadDB <gray>» <gold>Локальные головы"
size: 6
headDisplay:
  showPlayerName: true
  showUUID: false
```

#### Меню покупки (purchase.yml)
```yaml
title: "<red>HeadDB <gray>» <gold>{name} <gray>» <gold>Покупка"
size: 3
purchaseButtons:
  buy1:
    slot: 10
    name: "<green>Купить x1"
```

#### Меню поиска (search.yml)
```yaml
title: "<red>HeadDB <gray>» <gold>Поиск <gray>» <gold>{query}"
size: 6
resultSlots: [10, 11, 12, ...]
```

#### Меню дополнительных категорий (custom_categories.yml)
```yaml
title: "<red>HeadDB <gray>» <gold>Дополнительные категории"
size: 6
categoryDisplay:
  showHeadCount: true
```

### Открытие меню для других игроков
Используйте команду: `/hdb open <меню> <игрок>`

Примеры:
- `/hdb open Main Steve` - Открывает главное меню для Steve
- `/hdb open Animals Alex` - Открывает категорию Животные для Alex

**Требуемое право**: `headdb.command.open.others`

### Языковые файлы
Языковые файлы находятся в папке `messages/`:
- `en.yml` - Английский (по умолчанию)
- `ru.yml` - Русский

Чтобы добавить новый язык, скопируйте `en.yml` и переведите все значения.
