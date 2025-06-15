# ВЫПУСКНАЯ КВАЛИФИКАЦИОННАЯ РАБОТА

---

## Тема: Разработка Программного продукта для мониторинга сетевого трафика и первичного анализа с контролем безопасности.

---

## Описание проекта
Java/Spring-продукт, состоящий из двух основных компонентов - клиентского и серверного модуля. Проект содержит набор микросервисов:
- Traffic Capture Service - захват сетевого трафика на клиентских ПК.
- Traffic Analysis Service - анализ сетевого трафика, извлечение доменов, подсчет статистики.
- Traffic Security Service - проверка пакетов на угрозы по IP-адресам.
- Notification Service - отправка уведомлений в Telegram и на почту.

## Системные требования
- Java JDK 17+
- Npcap or WinPcap (Windows) / libpcap-dev (Linux) / libpcap (MacOS)
- Docker or Kubernetes

---

## Сборка и запуск проекта

```
# Capture Service
cd ns-traffic-capture-service
./mvnw clean package -DskipTests
java -jar target/ns-traffic-capture-service.jar

# Security Service
cd ../ns-traffic-security-service
./mvnw clean package -DskipTests
java -jar target/ns-traffic-security-service.jar

# Analysis Service
cd ../ns-traffic-analysis-service
./mvnw clean package -DskipTests
java -jar target/ns-traffic-analysis-service.jar

# Notification Service
cd ../notification-service
./mvnw clean package -DskipTests
java -jar target/notification-service.jar
```
