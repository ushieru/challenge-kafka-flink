# 🚀 Notifications System

Este sistema gestiona el enriquecimiento de transacciones bancarias en tiempo real utilizando una arquitectura orientada a eventos. El flujo comienza en un servicio legacy, se procesa en un motor de streaming y finaliza notificando al usuario final.

## 🛠️ Arquitectura

* **Legacy Service:** Quarkus (Java 21) - Emite transacciones en Avro.
* **Enricher:** Apache Flink 1.18.1 (Java 17) - Cruza datos con MongoDB.
* **Listener:** Quarkus (Java 21) - Consume las notificaciones enriquecidas.
* **Infra:** Kafka, Apicurio Registry, MongoDB.

---

## 📋 Pasos para el despliegue

### 1. Levantar Infraestructura

Asegúrate de tener Podman o Docker funcionando y ejecuta el archivo de orquestación:

```bash
podman-compose up -d

```

### 2. Configurar Datos de Prueba (MongoDB)

Para que el enriquecimiento funcione, necesitamos que la cuenta exista en la base de datos de usuarios.

```bash
# Entrar a la shell de MongoDB
podman exec -it mongodb mongosh

# Dentro de la shell (mongosh):
use notification_db

db.users.insertOne({
  "accountNumber": "12345",
  "email": "user@example.com",
  "phoneNumber": "3312345678",
  "deviceId": "android-001"
})

db.users.createIndex({ "accountNumber": 1 })

```

### 3. Compilar y Desplegar el Enricher (Flink)

El Job de Flink debe compilarse con el **Shadow JAR** para incluir todas las dependencias de los conectores.

```bash
# Entrar al proyecto de Flink
cd enriched-notifications-flink
./gradlew clean shadowJar

# Subir el JAR
# 1. Acceder a http://localhost:8081
# 2. Subir el archivo: app/build/libs/enriched-notifications-flink-1.0-SNAPSHOT-all.jar
```

### 4. Ejecutar Microservicios (Quarkus)

Abre dos terminales diferentes para arrancar los servicios de soporte:

**Legacy Transaction Service (Puerto 8082):**

```bash
cd legacy-transaction-service
./gradlew quarkusDev

```

**Notification Listener (Puerto 8083):**

```bash
cd notification-listener
./gradlew quarkusDev

```

---

## 🧪 Prueba de Integración (End-to-End)

Envía una transacción de prueba al servicio legacy. Si todo está correcto, verás cómo los datos fluyen desde el Legacy hasta el Listener pasando por el enriquecimiento de Flink.

```bash
curl -X POST http://localhost:8082/transaction \
     -H "Content-Type: application/json" \
     -d '{
        "amount": 125.50,
        "merchant": "Starbucks",
        "account": "12345"
     }'

```

### ✅ Resultado Esperado en el Listener:

```text
2026-02-16 08:57:42,044 INFO  [com.notifications.system.NotificationConsumer] (vert.x-worker-thread-1) 🔔 ENVIANDO NOTIFICACIÓN:
2026-02-16 08:57:42,045 INFO  [com.notifications.system.NotificationConsumer] (vert.x-worker-thread-1) {"transactionId": "d2b76c39-c9a0-448b-a846-c90ac4f0c865", "amount": 125.5, "merchantName": "Starbucks", "accountNumber": "12345", "deviceId": "android-001", "phoneNumber": "3312345678", "email": "user@example.com"}
```

---

## 📼 Video

<p align="center">
  <video src=".github/assets/Grabación de pantalla desde 2026-02-16 08-57-31.mp4" width="80%" controls muted autoplay loop>
    Tu navegador no soporta el elemento de video.
  </video>
</p>