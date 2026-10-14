# Guía de sustentación

1. **¿Qué problema resuelve CurrencyRPC?** Centraliza conversiones para que varias aplicaciones utilicen las mismas tasas y reglas.
2. **¿Por qué gRPC?** Ofrece un contrato tipado, generación de clientes y streaming integrado para comunicar procesos.
3. **¿Qué es RPC?** Es solicitar a otro proceso que ejecute una operación a través de la red.
4. **¿Qué es Protocol Buffers?** Es el lenguaje del contrato y el formato binario con el que enviamos los mensajes.
5. **¿Por qué HTTP/2?** Permite streams multiplexados y es el transporte utilizado por gRPC en este proyecto.
6. **¿Qué diferencia hay con REST?** REST suele modelar recursos con métodos HTTP; gRPC expone operaciones tipadas. REST no exige JSON ni HTTP/1.1. Aquí elegimos gRPC por los stubs y el streaming.
7. **¿Qué es Unary RPC?** Una llamada con una solicitud y una respuesta.
8. **¿Qué es Server Streaming?** Una solicitud recibe varias respuestas del servidor antes de finalizar.
9. **¿Dónde está implementado Unary RPC?** En `CurrencyGrpcService.convertCurrency` y `getSupportedCurrencies`.
10. **¿Dónde está implementado Server Streaming?** En `CurrencyGrpcService.watchExchangeRate`, que envía cinco `RateUpdate`.
11. **¿Qué hace currency.proto?** Define los tres métodos RPC y los campos de los mensajes compartidos por cliente y servidor.
12. **¿Cómo se generan las clases Java?** Maven ejecuta `protoc` y el plugin gRPC durante el build; quedan en `target/generated-sources/protobuf`.
13. **¿Por qué no usamos base de datos?** El alcance solo requiere un conjunto pequeño e inmutable de tasas en memoria.
14. **¿Por qué usamos tasas fijas?** Hacen la demostración reproducible y eliminan la dependencia de una API externa; no son tasas de mercado.
15. **¿Qué validaciones existen?** Monedas obligatorias y soportadas; importe positivo, hasta 18 dígitos enteros y 8 decimales, sin exponentes. También se validan puertos y la opción TLS.
16. **¿Qué ocurre si llega una moneda inválida?** El servidor responde con `INVALID_ARGUMENT` y una descripción; sigue disponible para nuevas llamadas.
17. **¿Cómo se ejecuta localmente?** Compilo con `mvn clean package`, inicio el JAR del servidor y ejecuto `CurrencyClient` en otra terminal.
18. **¿Cómo se desplegaría en cloud?** Con el Dockerfile y `gcloud run deploy --source . --use-http2`; el servidor lee `PORT` y el cliente usaría TLS en 443.
19. **¿Por qué se documentó pero no se ejecutó el despliegue?** Cloud Run exige vincular facturación y se decidió no ingresar una tarjeta. La URL mostrada es simulada y no está activa.
20. **¿Qué ventajas tiene gRPC en este caso?** Un contrato común, mensajes binarios, código generado y streaming. No afirmamos mejoras de rendimiento medidas porque no hicimos un benchmark.

Para presentar: [guion de cuatro minutos](demo.md). Para sustentar resultados: [evidencias reales](evidencias/ejecucion-local.txt).
