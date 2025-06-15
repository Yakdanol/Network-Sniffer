# TrafficCaptureService

**TrafficCaptureService** — микросервис на Java (Spring Boot) для:
- Захвата сетевого трафика с помощью [pcap4j](https://github.com/kaitoy/pcap4j).
- Отправки пакетов в удалённый Kafka-брокер (режим `remote` или `both`) либо локальное сохранение в файлы (режим `local`).
- Автоматического fallback в локальный режим при недоступности Kafka.

## Основные возможности

1. **Захват сетевого трафика**
    - Использует библиотеку `pcap4j` (версия 1.8.2).
    - Может фильтровать пакеты по BPF-фильтру (`traffic-capture.filter`).
    - Работает на интерфейсах Windows / Linux / macOS (где pcap доступен).

2. **Гибкие режимы работы**
    - **`local`**: все пакеты пишутся в файлы (PCAP/JSON/CSV/XML).
    - **`remote`**: все пакеты отправляются в Kafka. При сбоях → fallback в локальные файлы.
    - **`both`**: “double-write”: Kafka и файлы. При недоступном брокере → оставляем файлы.

3. **Fallback при проблемах с Kafka**
    - Если Kafka недоступна при старте: сервис сразу переходит в локальный режим.
    - Если брокер падает во время работы: `KafkaPacketSender` пробует несколько попыток отправки; при провале — закрывает ресурсы и сообщает исключение, чтобы `TrafficCaptureService` переключил режим на локальный.

 ---

## Структура и ключевые компоненты

- **`TrafficCaptureService`**
    - Центральный сервис.
    - Инициализирует `PcapHandle` (через `pcap4j`), запускает два пула потоков:
        - **captureExecutor** для чтения пакетов (loop(-1)).
        - **processingExecutor** для обработки (записи/отправки).
    - Анализирует `traffic-capture.mode` и устанавливает “активный” отправитель (Kafka или локальный).

- **`PacketSender`** (интерфейс)
    - Объявляет метод `void sendPacket(Packet packet) throws Exception`.
    - Реализации:
        - **`KafkaPacketSender`**: отправляет пакеты в Kafka (Spring Kafka).
        - **`LocalFilePacketSender`**: записывает пакеты в файлы.
