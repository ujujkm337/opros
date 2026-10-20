# DeepSeek V4.1 Flash Free — Настройка

## Что это

DeepSeek V4.1 Flash — нативная мультимодальная MoE-модель (552B основных + 196B Engram параметров),
доступная **бесплатно** через OpenCode Zen.

## Бесплатный тариф

| Параметр | Бесплатно | Платный Zen |
|----------|-----------|-------------|
| Model ID | `opencode/deepseek-v4.1-flash-free` | `opencode/deepseek-v4.1-flash` |
| Вход | **$0** | $0.30/1M |
| Выход | **$0** | $1.20/1M |
| Кэш | **$0** | $0.06/1M |
| Контекст | 200K | 1M |
| Выход макс | 32K | 384K |
| Где работает | Только внутри OpenCode | Любой клиент |
| Данные | Zero-retention | Zero-retention |

## Автоматическая настройка (через Opros Setup)

Setup автоматически записывает конфиг:

```json
// ~/.config/opencode/opencode.json
{
  "$schema": "https://opencode.ai/config.json",
  "model": "opencode/deepseek-v4.1-flash-free"
}
```

## Ручная настройка

Если нужно настроить вручную в Termux:

```bash
mkdir -p ~/.config/opencode

cat > ~/.config/opencode/opencode.json << 'EOF'
{
  "$schema": "https://opencode.ai/config.json",
  "model": "opencode/deepseek-v4.1-flash-free"
}
EOF
```

## Первый запуск

```bash
opencode
```

При первом запуске:
1. Выполните `/connect`
2. Выберите **OpenCode** (Zen provider)
3. При необходимости — войдите в аккаунт OpenCode

Далее:
- `/models` — просмотреть список моделей
- Модель DeepSeek V4.1 Flash Free должна быть выбрана по умолчанию

## Ограничения бесплатного тарифа

1. **Только внутри OpenCode** — API-запросы вне OpenCode вернут 403
2. **200K контекст** — вместо 1M у платной версии
3. **"Limited time"** — дата окончания не опубликована
4. **32K выход** — вместо 384K

## Альтернативные бесплатные модели в OpenCode

| Модель | Бесплатно? | Контекст |
|--------|-----------|----------|
| DeepSeek V4.1 Flash Free | ✅ Да | 200K |
| GLM-5.3-Flash Free | ✅ Да | ? |
| Space Bunny Free | ✅ Да | ? |
| Cat Jam Free | ✅ Да | ? |
| LongCat 2.5 Preview Free | ✅ Да | ? |
| Step 5 Preview Free | ✅ Да | ? |
| Exo Free | ✅ Да | ? |
| MiMo-V2.6-Flash Free | ✅ Да | ? |
| Nemotron 3 Ultra Free | ✅ Да | ? |

## Прямой API DeepSeek (если бесплатная кончится)

```bash
export DEEPSEEK_API_KEY="sk-..."
```

Цены DeepSeek API (off-peak):
- Вход: $0.15/1M токенов
- Выход: $0.60/1M токенов
- Кэш: $0.003/1M

Конфиг для прямого API:
```json
{
  "$schema": "https://opencode.ai/config.json",
  "provider": {
    "deepseek": {
      "npm": "@ai-sdk/openai-compatible",
      "name": "DeepSeek",
      "options": {
        "baseURL": "https://api.deepseek.com",
        "apiKey": "{env:DEEPSEEK_API_KEY}"
      },
      "models": {
        "deepseek-chat": {
          "name": "DeepSeek V4 Flash"
        }
      }
    }
  }
}
```
