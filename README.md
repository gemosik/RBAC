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

## Build service jars (for Docker)

```bash
./gradlew :user-service:bootJar :trip-service:bootJar :notification-service:bootJar
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

## Demo via Docker + JDK runner

1) Build jars:

```bash
./gradlew :user-service:bootJar :trip-service:bootJar :notification-service:bootJar
```

2) Start infra and services:

```bash
docker compose up --build -d
```

3) Run scripted demo:

```bash
./gradlew :demo-runner:run
```

If services are still warming up, run the same command again after 5-10 seconds.

Demo runner automatically:
- logs in to `trip-service` (`manager/manager123`)
- creates passenger and drivers
- seeds available drivers in `trip-service`
- creates trip with calculated price (`distance * tariff`)
- updates status, rates trip (1-5), reads stats
- reads generated notifications

4) Stop environment:

```bash
docker compose down
```
