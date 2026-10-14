# Demostración de CurrencyRPC — 4 minutos

Abrir dos terminales en la raíz del repositorio y estos archivos en el editor: `currency.proto`, `Dockerfile` y `docs/despliegue-simulado.md`. No ejecutar comandos de despliegue cloud.

## Preparación antes de presentar

```powershell
cd D:\CurrencyRPC
mvn clean package
```

Esperar `BUILD SUCCESS`. Si se desean regenerar todas las evidencias, con el puerto 9090 libre y `JAVA_HOME` apuntando al JDK 21:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verificar-entrega.ps1
```

El script ejecuta pruebas, empaqueta, inicia un servidor temporal y un cliente separado, guarda sus salidas y detiene únicamente el proceso de servidor que creó. Sobrescribe las evidencias con resultados nuevos. No comprueba apagado gradual: la limpieza del servidor automatizado utiliza `Stop-Process`.

## 1. Contrato — 0:00 a 0:25

Mostrar `src/main/proto/currency.proto`: «Este contrato define las operaciones y los mensajes. Maven genera las clases Java para ambos extremos».

## 2. Conversión unary — 0:25 a 0:45

Señalar `ConvertCurrency`: «Recibe origen, destino e importe y devuelve una respuesta. Los importes viajan como texto decimal y se calculan con BigDecimal».

## 3. Iniciar servidor — 0:45 a 1:05

Terminal 1, PowerShell:

```powershell
$env:PORT = '9090'
java -jar target/currency-rpc.jar
```

Señalar el inicio en `0.0.0.0:9090`: «El puerto se configura por entorno».

## 4. Ejecutar cliente — 1:05 a 1:25

Terminal 2, PowerShell:

```powershell
$env:GRPC_HOST = 'localhost'
$env:GRPC_PORT = '9090'
$env:GRPC_TLS = 'false'
java -cp target/currency-rpc.jar com.currencyrpc.client.CurrencyClient
```

«Son dos procesos comunicándose mediante gRPC sobre HTTP/2». El cliente muestra toda la demo automáticamente en unos segundos; los siguientes pasos comentan esa salida.

## 5. Monedas — 1:25 a 1:40

Señalar USD, COP, EUR, GBP, JPY y MXN. «Este listado también es una llamada unary».

## 6. Conversiones y validación — 1:40 a 2:15

Señalar `100 USD -> 400000.00 COP` y `500000 COP -> 125.00 USD`. «Las tasas son simuladas». Al final de la salida mostrar los dos `INVALID_ARGUMENT`: importe cero y moneda XYZ. «Los errores son esperados y el servidor permanece disponible».

## 7. Streaming y pruebas — 2:15 a 2:55

Señalar `Update 1` a `Update 5`: «Una solicitud produce cinco respuestas separadas aproximadamente por un segundo». Si se quiere repetir, volver a ejecutar el comando del cliente. Mostrar `docs/evidencias/tests.txt`: «29 pruebas exitosas; la evidencia corresponde a una ejecución real».

## 8. Dockerfile — 2:55 a 3:15

«La primera etapa compila y prueba; la segunda ejecuta el JAR con Java 21 y usuario sin privilegios. El Dockerfile está preparado, pero no se ejecutó porque Docker no está disponible».

## 9. Despliegue simulado — 3:15 a 3:45

Mostrar `docs/despliegue-simulado.md`: «Cloud Run requería facturación; no ingresamos tarjeta. Este procedimiento no fue ejecutado. La URL rotulada como simulada es representativa y no corresponde a un servicio activo». Explicar TLS externo, h2c interno y `PORT`.

## 10. Cierre — 3:45 a 4:00

«Verificamos contrato, unary, streaming y validaciones con pruebas y procesos locales reales. Dejamos documentado el despliegue sin atribuirnos una ejecución cloud». Detener servidor con Ctrl+C.

## Alternativa CMD

Terminal 1:

```bat
set PORT=9090
java -jar target/currency-rpc.jar
```

Terminal 2:

```bat
set GRPC_HOST=localhost
set GRPC_PORT=9090
set GRPC_TLS=false
java -cp target/currency-rpc.jar com.currencyrpc.client.CurrencyClient
```

Si el puerto está ocupado, detener el servidor previo o cambiar `PORT` y `GRPC_PORT` al mismo puerto libre. Si la demostración en vivo falla, mostrar la evidencia guardada indicando que corresponde a una ejecución anterior, sin presentarla como una conexión en vivo.
