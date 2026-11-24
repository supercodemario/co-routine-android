# FlowLab

Small HTTP API for an Android coroutines / Flow exercise.

- **Auth** — register, login, JWT
- **Dashboard** — devices, latest reading, recent history
- **Live stream** — Server-Sent Events, one JSON object per event
- **ESP32** — optional `POST` that shows up on the same stream

The phone never talks to the ESP32 directly. The board posts to this API, and the app collects the stream as a `Flow`.

## Run

```bash
cd back_end
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

- API docs: http://127.0.0.1:8000/docs
- Browser dashboard: http://127.0.0.1:8000
- On a phone, use your computer's LAN IP, for example `http://192.168.1.20:8000`

Demo login (created on first start):

| | |
|---|---|
| email | `demo@flow.app` |
| password | `demo1234` |
| ESP32 key | `esp32-demo-key` |

## Endpoints

| Method | Path | Auth | What it is |
|---|---|---|---|
| POST | `/api/auth/register` | no | `{name, email, password}` → token |
| POST | `/api/auth/login` | no | `{email, password}` → token |
| GET | `/api/auth/me` | Bearer | current user |
| GET | `/api/dashboard` | Bearer | summary for the home screen |
| GET | `/api/devices` | Bearer | devices and their keys |
| POST | `/api/devices` | Bearer | `{name}` → new device + `api_key` |
| POST | `/api/devices/telemetry` | `X-Device-Key` | ESP32 reading |
| POST | `/api/stream/simulate` | Bearer | one fake reading, no hardware |
| GET | `/api/stream/events` | Bearer or `?token=` | SSE stream |

### Telemetry body

```json
{ "temperature": 26.4, "humidity": 61.2, "message": "esp32" }
```

### Stream events

Each SSE message is `data: {json}\n\n`. Types:

- `hello` — connection opened
- `tick` — every 5 seconds, so `collect` has work without a sensor
- `telemetry` — a real or simulated reading
- `device_created` — a device was registered

`telemetry` looks like:

```json
{
  "type": "telemetry",
  "device_id": 1,
  "device_name": "Desk ESP32",
  "user_id": 1,
  "temperature": 26.4,
  "humidity": 61.2,
  "message": "esp32",
  "created_at": "2026-11-23T04:50:00Z"
}
```

## Android Flow sketch

Use the computer's LAN IP as `baseUrl`. OkHttp is enough; Ktor's SSE client works the same way.

```kotlin
data class StreamEvent(
    val type: String,
    val deviceId: Int? = null,
    val deviceName: String? = null,
    val temperature: Double? = null,
    val humidity: Double? = null,
    val message: String? = null,
    val createdAt: String? = null,
)

fun liveEvents(client: OkHttpClient, baseUrl: String, token: String): Flow<StreamEvent> =
    callbackFlow {
        val request = Request.Builder()
            .url("$baseUrl/api/stream/events")
            .header("Authorization", "Bearer $token")
            .header("Accept", "text/event-stream")
            .build()
        val call = client.newCall(request)
        val response = call.execute()
        val source = response.body?.source() ?: error("empty body")
        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val event = Json.decodeFromString<StreamEvent>(line.removePrefix("data: "))
                trySend(event)
            }
            close()
        } catch (e: Exception) {
            close(e)
        } finally {
            call.cancel()
        }
        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)
```

Ideas to practice on top of that flow:

- `retry` when the socket drops
- `filter { it.type == "telemetry" }`
- `map` into a UI model
- `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`
- `combine` the stream with `flow { emit(api.dashboard()) }` so the screen has both a snapshot and live updates

Dashboard snapshot (one-shot, still a `Flow` if you want):

```kotlin
fun dashboard(token: String): Flow<Dashboard> = flow {
    emit(api.getDashboard(token))
}
```

## ESP32

Sketch: `firmware/esp32_telemetry.ino`

1. Install **ArduinoJson** in the Arduino IDE.
2. Set Wi-Fi, `API_URL` (LAN IP of this computer), and `DEVICE_KEY`.
3. Flash the board. It posts about every 4 seconds.
4. Open the app stream or http://127.0.0.1:8000 and watch `telemetry` events.

A device counts as online for 45 seconds after its last post.

Without a board, `POST /api/stream/simulate` (or the button on the browser page) emits the same event shape.
