# Arquitectura de CurrencyRPC

## Arquitectura general

```mermaid
flowchart LR
    C[Java Client: CurrencyClient] -->|gRPC / Protobuf / HTTP/2| S[Currency Server: CurrencyServer]
    S --> G[Currency Service: CurrencyGrpcService]
    G --> R[Exchange Rate Provider: CurrencyRateService]
    R --> M[Tasas inmutables en memoria]
    A[AppConfig] -.-> C
    A -.-> S
```

El cliente usa el stub bloqueante generado y presenta resultados. El servidor inicia Netty, asigna hilos virtuales y cierra recursos al apagar. El adaptador gRPC convierte errores de validación en `INVALID_ARGUMENT`. El servicio de tasas normaliza códigos y calcula con `BigDecimal`. No hay persistencia.

## Flujo unary

```mermaid
sequenceDiagram
    participant C as Cliente
    participant G as Servicio gRPC
    participant R as Servicio de tasas
    C->>G: ConvertCurrency(USD, COP, "100")
    G->>R: convert(USD, COP, "100")
    R->>R: Validar y calcular 100 x 4000 / 1
    alt Entrada válida
        R-->>G: Conversion(400000.00)
        G-->>C: ConvertResponse y estado OK
    else Entrada inválida
        R-->>G: IllegalArgumentException
        G-->>C: INVALID_ARGUMENT y descripción
    end
```

`GetSupportedCurrencies` también es unary y devuelve seis códigos en orden determinista.

## Flujo server streaming

```mermaid
sequenceDiagram
    participant C as Cliente
    participant G as Servicio gRPC
    participant R as Servicio de tasas
    C->>G: WatchExchangeRate(USD, COP)
    G->>R: exchangeRate(USD, COP)
    R-->>G: Base 4000
    G-->>C: RateUpdate 1 - 4000
    Note over G: Esperar 1 segundo y comprobar cancelación
    G-->>C: RateUpdate 2 - 4004
    Note over G: Esperar 1 segundo y comprobar cancelación
    G-->>C: RateUpdate 3 - 3996
    Note over G: Esperar 1 segundo y comprobar cancelación
    G-->>C: RateUpdate 4 - 4008
    Note over G: Esperar 1 segundo y comprobar cancelación
    G-->>C: RateUpdate 5 - 4002
    G-->>C: onCompleted / OK
```

La primera respuesta es inmediata. Un stream cancelado puede conservar su hilo virtual hasta el siguiente despertar, aproximadamente un segundo como máximo. Las variaciones no cambian las conversiones unary. El flujo está acotado a cinco mensajes pequeños por solicitud.

## Despliegue previsto

```mermaid
flowchart LR
    C[Cliente local] -->|TLS / HTTP/2 :443| E[Cloud Run: terminación TLS]
    E -->|h2c / PORT| N[Contenedor Java / Netty]
```

Localmente se usa h2c. En un despliegue real de Cloud Run, el cliente externo verificaría TLS y el contenedor no administraría certificados. Este diagrama es una arquitectura prevista: el despliegue no fue ejecutado por el requisito de facturación. Véase [despliegue simulado](despliegue-simulado.md). El proveedor de tasas del diagrama es la clase local `CurrencyRateService`, no un servicio externo.
