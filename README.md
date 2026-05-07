# RBAC Taxi Platform (Study Project)

Учебный backend-проект сервиса такси в формате multi-module.

## Modules

- `user-service` - управление пассажирами и водителями.
- `trip-service` - управление поездками и статусами.
- `notification-service` - очередь уведомлений и фоновая обработка.

## Run tests

```bash
./gradlew test
```

## Run services

```bash
./gradlew :user-service:bootRun
./gradlew :trip-service:bootRun
./gradlew :notification-service:bootRun
```

## Quick health check

- `GET http://localhost:8081/api/v1/system/ping`
- `GET http://localhost:8082/api/v1/system/ping`
- `GET http://localhost:8083/api/v1/system/ping`
