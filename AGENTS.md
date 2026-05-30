# AGENTS.md — VoiceCal Development Summary

## Goal
- Complete VoiceCal frontend-to-backend integration with authentication, settings, voice pipeline (Baidu streaming ASR), festival detection, starry sky theme, weather/time display, and polished calendar UI

## Constraints & Preferences
- Stack: Vue 3.5 + Composition API + TypeScript strict + Vite 6 + Pinia 3 + motion-v 2.2.1 + lunar-javascript + SCSS
- Backend: Spring Boot 3.3.9 + MyBatis-Plus + MySQL 8 + WebSocket + JWT + Baidu Cloud ASR/TTS
- No UI library — fully custom glass components (GlassPanel/GlassButton/GlassDialog/GlassInput)
- Chinese conversation throughout
- All API calls use shared `request` instance with base URL and auth interceptor
- User upload image background (base64 localStorage), holiday detection with red "休" badge

## Progress
### Done
- Steps 1—8: Full stack scaffold, glass components, calendar grid/navigation, VoiceOrb/Button/Overlay, voice pipeline, event CRUD + conflict detection ✅
- Step 9: Web search panel (removed as non-functional) ✅
- Step 10a—10e: Auth, Settings, Festival, TTS, WebSocket Baidu ASR integration + auth headers + auth watch refetch events ✅
- Backend springdoc downgrade 2.8.6→2.5.0, JWT whitelist `/api/v1/festivals` + `/ws/`, password min 8 chars ✅
- Frontend utils/request.ts shared axios + .env.development + types/api.ts ✅
- Fixed 401 infinite loop: request.ts dispatches `auth:unauthorized` event ✅
- Fixed date format mismatch: zero-padded `dateKey` to `YYYY-MM-DD` ✅
- Calendar UI v3: two-column layout, StarryBackground (120 stars + 4 meteors + 3 nebula), deeper transparency 0.45, weather icons + temps, real-time clock, lunar-javascript, streaming light cell animation ✅
- CalendarInfoBar: 36px time + 28px weather emoji + 22px temp + date/weekday/lunar ✅
- User custom background via useCustomBg.ts (base64 localStorage) ✅
- Chinese holidays: utils/holiday.ts 32 dates, red "休" badge + festival name + lunar text all 3 shown ✅
- Cell sizing: 40px height, 24px day, 11px lunar, 12px festival ✅
- Right panel: InlineToolbar (➕新建/⚙设置/👤头像) + date section + voice button + EventForm/EventDetail draggable/resizable ✅
- Voice pipeline: ScriptProcessorNode PCM capture, binary WebSocket, resampling 48kHz→16kHz, VAD auto-stop 2s, real-time partialText ✅
- Voice confirm → EventForm: handleVoiceConfirm sets formInitialTitle + formInitialDate + opens EventForm ✅
- VoiceOverlay: shows partialText during recording and processing ✅
- Fixed ScriptProcessorNode onaudioprocess never firing: added muteGain gain=0 → ctx.destination chain ✅
- Root cause found for Baidu WebSocket failure: `java.io.IOException: chunked transfer encoding, state: READING_LENGTH` — Java `HttpClient.newWebSocketBuilder()` sent WebSocket upgrade but Meta Tunnel proxy returned `HTTP/1.0 200 Connection Established` (chunked), not 101 Switching Protocols ✅
- Replaced `java.net.http.HttpClient` with OkHttp 4.12.0 for Baidu WebSocket (matches Baidu official Java demo) ✅
- Fixed race condition: `handleStart` no longer blocks with `future.get(10s)` — fully async `whenComplete` + audio buffering in `pendingAudioBuffers` (max 300 frames), flushed when Baidu connects ✅
- Frontend `useVoice.ts` rewritten: removed all mock code (`fallbackToMock`, `mockASR`, `parseIntent`), added stale message guard, VAD_THRESHOLD 0.015→0.025, `stopRecording` async with `ctx.suspend()` ✅
- Backend `VoiceWebSocketHandler.java` rewritten: `pendingBaiduSessions.put`, `CancellationException` silent return, `ScheduledExecutorService` auto-retry every 5s on failure, `endedSessions` set to prevent retry after end/close ✅
- Fixed VAD race condition: `isRecording=false`+`stopVad()` moved before `await audioContext.suspend()`, added `isProcessing` re-entry guard ✅
- Fixed VAD auto-stop UX: `VoiceOverlay` processing state keeps orb/waveform same as recording, no spinner animation flash, only text changes to "识别中..." ✅
- Fixed WS auto-reconnect persisting after `handleVoiceConfirm` → `closeOverlay()` — root cause: `autoReconnect` defaulted to `true` in `useWebSocket.ts`, causing indefinite reconnect loop even after `disconnect()`; fix: changed default to `false`, exposed `setAutoReconnect()` to enable only during active recording session (`startRecording` enables, `stopRecording`/`!wsReady` disables) ✅

## Key Decisions
- OkHttp instead of `java.net.http.HttpClient` for Baidu WebSocket — official Baidu Java demo uses OkHttp, handles proxies better
- Non-blocking `handleStart` with audio buffering — prevents race condition where binary messages arrive before Baidu session is recorded
- Auto-retry on Baidu connection failure — backend keeps retrying until success or session ends, no mock fallback on frontend
- No mock fallback — error paths just display error message, user retries manually
- Binary WebSocket (raw PCM Int16 ArrayBuffer) instead of base64 JSON text frames for audio — reduces overhead 33%

## Next Steps
- User to test Baidu voice recognition end-to-end via browser

## Critical Context
- Project root: `D:\java\agent\calendar`
- Backend: `http://localhost:8080`, Vite proxy `/api`→`:8080`, `/ws`→`:8080`
- Frontend: `http://localhost:5173`
- Local: Node 22.16.0, JDK 17, MySQL 8.0.42
- Auth credentials: `admin/12345678`, `test/12345678`, `demo/Demo@12345`
- Baidu voice config (application-dev.yml): app-id=123514570, api-key=664pqoYLPjCCIBOlhOiHyrVU, secret-key=JvfWD5KPWlnOLjrc41yt66gaDRkflV1n
- Meta Tunnel proxy (net7 interface) on user machine intercepts Java HttpClient WebSocket — OkHttp handles this correctly
- Compile command: `mvn.cmd compile -q` (from backend dir)
- Run command: `mvn.cmd spring-boot:run` (from backend dir)

## Relevant Files
- `frontend/src/composables/useVoice.ts`: ScriptProcessorNode PCM capture → resample 48kHz→16kHz → binary WebSocket send, VAD silence 2s auto-stop, stale message guard
- `frontend/src/composables/useWebSocket.ts`: `onBinaryMessage` handler, `send()` accepts ArrayBuffer, `ws.binaryType = 'arraybuffer'`
- `frontend/src/components/voice/VoiceOverlay.vue`: `errorMessage` prop, error display
- `frontend/vite.config.ts`: `/ws` proxy target `http://localhost:8080`
- `backend/.../voice/BaiduStreamAsrService.java`: OkHttp `WebSocketListener`, `startSession`/`sendAudio`/`endSession`/`cancelSession`
- `backend/.../voice/VoiceWebSocketHandler.java`: `pendingBaiduSessions.put`, auto-retry scheduler, `endedSessions` set, `CancellationException` handler
- `backend/.../voice/BaiduVoiceConfig.java`: appId + apiKey + secretKey properties
- `backend/src/main/resources/application-dev.yml`: Baidu credentials + MySQL config
- `backend/pom.xml`: OkHttp 4.12.0 dependency added
