# Publication audit — 2026-09-07

## Implemented in Android

- Jobs load from the shared backend `GET /jobs`.
- Job creation uses authenticated `POST /jobs` and the returned job identifier.
- Applications use authenticated `POST /jobs/:id/apply`, including the optional CV URL. Success is shown only after server acknowledgement. External jobs open the recruiter's URL.
- Job sharing opens the Android share chooser. Filters derive from loaded offers.
- Removed the unused fictional job catalogue and invented default counters.
- Actus and job forms remain open on API failure. Publishing indicators disable repeat submissions.
- Status and short-video publishing indicators follow the ViewModel request state.
- Short-video visibility is forwarded from the form to the backend (`public`, `friends`, `private`).
- Added isolated HTTP client tests for job mapping, rejected applications and unauthenticated creation.

## Confirmed remaining work

- Production GET status, actus (limit=2) and short-videos (limit=2) still each exceed 2,000,000 bytes. This remains incompatible with the publication client's size limit. A health HTTP 200 does not validate these feeds or writes.
- The inspected shared backend stores published jobs and applications in process memory. Durable database persistence is still required.
- Job likes/bookmarks still use local state; no shared API persistence is implemented in this change.
- Short-video draft, trimming, music/mute and comment controls require further implementation; visibility forwarding alone does not implement those controls.
- Some secondary actus menus still require replacement of local feedback with actual API actions.
- No device was detected by `adb devices`; publication creation and rendering on Android have not been validated end-to-end in this session.

## Checks to run

`assembleDebug` succeeded on 2026-09-07. APK: `app/build/outputs/apk/debug/app-debug.apk` (37,782,690 bytes).

`gradle :app:testDebugUnitTest --tests com.loukatech.mbote.PublicationApiServiceTest :app:assembleDebug`

The new HTTP tests use a local server and do not create production users or publications. Do not interpret an APK build or these tests as validation of production publication creation.
