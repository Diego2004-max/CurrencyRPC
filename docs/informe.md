# Sistema distribuido de conversión de monedas utilizando gRPC y Protocol Buffers

**Proyecto:** CurrencyRPC  
**Asignatura:** Programación Orientada a la Web, séptimo semestre  
**Autor, docente e institución:** completar para la entrega  
**Fecha:** 13 de octubre de 2026

## Introducción

Las aplicaciones distribuidas necesitan intercambiar información y ejecutar operaciones entre procesos. Este proyecto implementa un servicio central de conversión monetaria y un cliente Java mediante gRPC, Protocol Buffers y HTTP/2. Permite observar los patrones solicitud-respuesta y transmisión de varias respuestas desde el servidor.

## Planteamiento del problema

Una empresa utiliza distintas aplicaciones que requieren conversiones monetarias. Mantener reglas y tasas independientes dificulta obtener resultados consistentes. Se necesita un servicio remoto con interfaz explícita y manejo uniforme de errores.

## Justificación

Una implementación pequeña permite estudiar contrato, generación de código, serialización y llamadas remotas. Las tasas simuladas eliminan dependencias externas y permiten reproducir resultados. El alcance es académico; no se pretende proporcionar cotizaciones reales ni un sistema contable.

## Objetivo general

Construir una aplicación distribuida de conversión monetaria con gRPC y Protocol Buffers, pruebas automatizadas, contenedor Docker y preparación para despliegue cloud.

## Objetivos específicos

- Diseñar mensajes y métodos en un archivo `.proto`.
- Implementar conversiones directas y cruzadas con precisión decimal.
- Exponer operaciones unary y server streaming.
- Reportar entradas inválidas mediante estados gRPC.
- Probar lógica y transporte real.
- Documentar ejecución local y despliegue.

## Marco conceptual

**RPC** permite solicitar operaciones a otro proceso por red. Aunque el stub expone métodos similares a llamadas locales, deben contemplarse fallos y latencia. **gRPC** genera clientes y bases de servicios a partir del contrato e incorpora estados, deadlines y cancelación.

**Protocol Buffers** describe mensajes con campos numerados y permite serializarlos de forma binaria. Los números de campos no deben reutilizarse para otros significados al evolucionar el contrato. **HTTP/2** transporta las llamadas con streams multiplexados. **Unary RPC** intercambia una solicitud y una respuesta; **server streaming** entrega varias respuestas a una sola solicitud.

## Caso de estudio

El cliente consulta las monedas disponibles, convierte 100 USD a COP y 500000 COP a USD, y observa cinco tasas simuladas USD/COP. El servidor admite USD, COP, EUR, GBP, JPY y MXN sin base de datos, frontend, autenticación de aplicación ni proveedor externo.

## Arquitectura propuesta

```mermaid
flowchart LR
    C[Cliente Java] -->|gRPC / Protobuf / HTTP/2| S[Servidor Java]
    S --> G[Adaptador gRPC]
    G --> R[Lógica y tasas en memoria]
```

La configuración se concentra en `AppConfig`, el transporte en `CurrencyGrpcService` y el cálculo en `CurrencyRateService`. Véanse los diagramas de secuencia en [arquitectura.md](arquitectura.md).

## Implementación

Java 21 y Maven generan y compilan mensajes y stubs automáticamente. Netty sombreado implementa gRPC/HTTP2. Shade empaqueta dependencias y combina proveedores de servicios en un JAR ejecutable. JUnit 5 verifica la aplicación.

Las tasas por USD son 1 USD, 4000 COP, 0.92 EUR, 0.79 GBP, 150 JPY y 17 MXN. Se calcula `importe × tasaDestino / tasaOrigen` con `BigDecimal`, redondeando una vez el resultado a dos decimales mediante `HALF_UP`. Es una simplificación académica uniforme que no modela las fracciones monetarias particulares de cada divisa.

### Descripción del archivo .proto

Se usa `proto3`, paquete `currency.v1`, paquete Java `com.currencyrpc.grpc` y clases separadas mediante `java_multiple_files`. El contrato contiene `ConvertCurrency`, `GetSupportedCurrencies` y `WatchExchangeRate`. Los importes y tasas usan strings decimales para conservar precisión entre lenguajes. Cada actualización añade secuencia y timestamp Unix en milisegundos.

### Funcionamiento del servidor

`CurrencyServer` escucha en `0.0.0.0`, puerto 9090 o el indicado por `PORT`. Registra el servicio y asigna hilos virtuales a las llamadas. Un hook permite un apagado con espera limitada y cierre forzado si fuera necesario.

Se validan monedas e importes positivos, con límites de 18 dígitos enteros y 8 decimales, sin exponentes. Entradas incorrectas producen `INVALID_ARGUMENT`. El streaming aplica cinco factores deterministas y comprueba cancelación antes de cada envío, manteniendo intactas las tasas base.

### Funcionamiento del cliente

El cliente obtiene host, puerto y TLS mediante variables de entorno. Ejecuta una demostración automática, aplica deadline de 30 segundos por RPC, informa errores y cierra el canal. Ante fallo termina con código de salida distinto de cero.

## Pruebas realizadas

Las pruebas unitarias cubren conversiones directas, inversas y cruzadas, redondeo, normalización y entradas inválidas. Las pruebas de integración levantan un servidor Netty en puerto efímero para verificar las tres RPC, estados de error, orden de cinco respuestas y deadline del cliente sin impedir nuevas consultas.

Comandos reproducibles: `mvn test` y `mvn clean package`. Los reportes están en `target/surefire-reports`. La evidencia final de ejecución se registra al final de este informe.

## Dockerización

El Dockerfile compila y prueba con Maven/JDK 21 y ejecuta con JRE 21 como usuario sin privilegios. Respeta `PORT` y expone 9090. `.dockerignore` excluye Git, resultados de compilación y archivos locales. Docker no se encontró en este entorno: la construcción y ejecución de la imagen quedan pendientes de verificación.

## Despliegue en la nube

Se eligió Google Cloud Run por su soporte de gRPC y streaming. Con HTTP/2 habilitado, el contenedor recibe h2c; el cliente externo usa TLS en 443. El README incluye comandos de Artifact Registry, Cloud Build y Cloud Run con `--use-http2`.

No se realizó despliegue porque no se encontró Google Cloud CLI. Se requieren cuenta autenticada, proyecto, facturación y permisos. No se atribuyen resultados, URL pública ni mediciones a un despliegue inexistente.

## Resultados y límites

Las tasas elegidas producen 400000.00 COP para 100 USD y 125.00 USD para 500000 COP. El stream USD/COP produce 4000, 4004, 3996, 4008 y 4002. No se realizaron pruebas de carga, comparaciones de rendimiento frente a REST ni validación de alta disponibilidad.

## Conclusiones

La separación entre contrato, transporte y cálculo permite estudiar dos patrones RPC y verificar errores de forma independiente. Los decimales representados como texto evitan introducir aproximaciones binarias en el contrato. El JAR y la configuración por entorno preparan el servidor para ejecución local y cloud; validar Docker y la nube requiere completar las herramientas externas pendientes.

## Evidencia de ejecución local

Verificación del 13 de octubre de 2026, Windows x86_64, JDK 21.0.7 y Maven 3.9.6:

| Verificación | Resultado observado |
| --- | --- |
| `mvn clean compile` | `BUILD SUCCESS`, contrato generado y 19 fuentes compiladas |
| `mvn test` | `BUILD SUCCESS`, 29 pruebas, cero fallos, errores u omisiones |
| `mvn clean package` | `BUILD SUCCESS`, 29 pruebas y JAR con dependencias generado |
| Servidor empaquetado | Inicio en `0.0.0.0:9090` |
| Cliente en proceso separado | Conexión a `localhost:9090`, salida 0 |
| Monedas soportadas | USD, COP, EUR, GBP, JPY, MXN |
| Conversiones | 100 USD = 400000.00 COP; 500000 COP = 125.00 USD |
| Streaming | Cinco respuestas: 4000, 4004, 3996, 4008 y 4002; finalización correcta |
| Docker | No ejecutado: herramienta no disponible |
| Cloud Run | No desplegado: Google Cloud CLI no disponible |

El acceso inicial a Maven Central fue bloqueado por el sandbox; la compilación se completó al permitir la descarga de dependencias. Las advertencias de Shade sobre metadatos repetidos no impidieron ejecutar el JAR. Los resultados locales no sustituyen la validación pendiente de Docker y Cloud Run.

## Referencias

- [gRPC Java](https://grpc.io/docs/languages/java/basics/).
- [Protocol Buffers proto3](https://protobuf.dev/programming-guides/proto3/).
- [Cloud Run y gRPC](https://docs.cloud.google.com/run/docs/triggering/grpc).
- [Cloud Run y HTTP/2](https://docs.cloud.google.com/run/docs/configuring/http2).
