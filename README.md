# Toxser Censor

Серверный Fabric-мод для фильтрации чата. Поддерживает два режима:

- **banwords** — полная замена слова на `***`
- **partial** — маскировка середины слова (первый и последний символ сохраняются)

## Требования

- Minecraft 26.1.2
- Fabric Loader ≥ 0.19.3
- Java ≥ 25

## Установка

Скопируйте `toxsercensor-1.0.0.jar` в папку `mods/` вашего сервера.

## Конфигурация

При первом запуске создаётся файл `config/toxsercensor.json` со стандартным списком стоп-слов.

### Команды (op ≥ 2)

| Команда | Описание |
|---------|----------|
| `/toxsercensor create` | Создать конфиг |
| `/toxsercensor reload` | Перечитать конфиг из файла |
| `/toxsercensor remove` | Удалить конфиг |
| `/toxsercensor status` | Показать текущую статистику |
| `/toxsercensor edit banwords add <слово>` | Добавить слово в banwords |
| `/toxsercensor edit banwords remove <слово>` | Удалить слово из banwords |
| `/toxsercensor edit banwords list` | Показать все banwords |
| `/toxsercensor edit partial add <слово>` | Добавить слово в partial |
| `/toxsercensor edit partial remove <слово>` | Удалить слово из partial |
| `/toxsercensor edit partial list` | Показать все partial слова |

### Формат конфига

```json
{
  "banwords": ["слово1", "слово2"],
  "partialWords": ["бля", "хуй"]
}
```

## Сборка

```bash
./gradlew build
```

JAR будет в `build/libs/toxsercensor-1.0.0.jar`.

## Лицензия

MIT
