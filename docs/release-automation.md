# MBote release automation

## Ce que fait le projet apres configuration

- chaque `git commit` pousse automatiquement la branche courante vers `origin`
- chaque push sur `dev` ou `main` :
  - redeploie `m-bote.vercel.app`
  - genere `app-release.apk`
  - genere `app-release.aab`
  - met a jour la release GitHub roulante `mobile-latest`

## Secrets GitHub Actions requis

### Vercel

- `VERCEL_TOKEN`

### Android signing

- `MBOTE_ANDROID_KEYSTORE_BASE64`
- `MBOTE_ANDROID_KEYSTORE_PASSWORD`
- `MBOTE_ANDROID_KEY_ALIAS`
- `MBOTE_ANDROID_KEY_PASSWORD`

Pour remplir `MBOTE_ANDROID_KEYSTORE_BASE64` depuis Windows PowerShell :

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("F:\LOUK'APP\Mbote\mboté\android\app\mbote-release.jks"))
```

## Hook local

Le repo utilise `.githooks/post-commit`.
Active-le localement une seule fois :

```powershell
git config core.hooksPath .githooks
```

Desactive temporairement l'auto-push pour un commit :

```powershell
$env:MBOTE_SKIP_AUTO_PUSH = "1"
git commit -m "..."
Remove-Item Env:MBOTE_SKIP_AUTO_PUSH
```

## Build local store-ready

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-android-release.ps1 -VersionCode 2 -VersionName 1.0.2
```

Artefacts generes :

- `android/app/build/outputs/apk/release/app-release.apk`
- `android/app/build/outputs/bundle/release/app-release.aab`
