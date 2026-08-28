# Audit MBote - 2026-07-07

## Portee

Audit local parent-agent sur le depot actif `F:\LOUK'APP\Mbote\mboté`.
Le preflight Codex Security n'a pas valide le mode exhaustif multi-agent, donc cette passe couvre les surfaces inspectees localement sans pretendre a une couverture exhaustive fichier par fichier.

## Failles et corrections appliquees

### Eleve - Signalisation d'appels directs sans controle social

- Surface: Socket.IO `call-ring`, `call-accepted-direct`, `call-offer`, `call-answer`, `call-ice-candidate`, `call-ended`.
- Risque: un utilisateur authentifie pouvait cibler directement une room `user_<id>` si le client envoyait un `targetUserId` arbitraire.
- Correction: ajout de `hasDbSocialAccess()` et `canUseDirectCall()` pour verifier une amitie acceptee ou un lien de contact prive avant toute signalisation directe.
- Fichier: `server.ts`.

### Moyen - Uploads video sans limiteur applicatif dedie

- Surface: `/api/uploads/short-videos`, `/api/uploads/actus-videos`.
- Risque: surcharge CPU/memoire/disque ou cout stockage par envois repetes.
- Correction: ajout de `uploadLimiter` avant le parseur `express.raw`.
- Fichier: `server.ts`.

### Moyen - Mutations messages sans limiteur dedie

- Surface: `/api/chats`, `/api/messages`.
- Risque: spam de creation de chats, messages, reactions et changements de lecture.
- Correction: ajout de `messageLimiter` dans le routeur messaging pour les methodes `POST`, `PATCH`, `DELETE`.
- Fichier: `src/server/modules/messaging/messaging.routes.ts`.

### Moyen - Evenements Socket.IO sans quotas par connexion

- Surface: messages temps reel, typing, appels directs, WebRTC de groupe, signalisation de reunion.
- Risque: spam websocket, surcharge CPU/memoire, relais de payloads SDP/ICE trop volumineux.
- Correction: ajout d'un compteur par socket avec emission `rate_limited` et limite de taille JSON avant traitement des evenements sensibles.
- Fichier: `server.ts`.

### Moyen - Secret E2EE extractible depuis localStorage

- Surface: chiffrement local des messages prives.
- Risque: une XSS pouvait lire le secret `mbote.crypto.localSecret.v2` cree dans `localStorage` et tenter de dechiffrer les messages locaux.
- Correction: les nouveaux messages utilisent le format `mb3:` avec une cle AES-GCM non extractible stockee dans IndexedDB. Le format existant `mb2:` reste pris en charge pour ne pas casser le dechiffrement des messages deja crees sur les navigateurs qui possedent encore l'ancien secret.
- Fichiers: `src/services/cryptoService.ts`, `src/services/chatService.ts`.

### Moyen - SMS Twilio trop permissif

- Surface: `/api/send-sms`.
- Risque: abus d'envoi SMS par compte authentifie, numeros invalides, medias non HTTPS.
- Correction: ajout de `smsLimiter`, validation E.164, limite de corps a 1000 caracteres et media HTTPS obligatoire.
- Fichier: `server.ts`.

### Moyen - Captures d'ecran autorisees dans l'APK Android

- Surface: application Android Capacitor.
- Risque: capture d'ecran, enregistrement d'ecran ou apercu recent-apps de conversations et donnees sensibles.
- Correction: activation de `WindowManager.LayoutParams.FLAG_SECURE` dans l'activite Android principale.
- Fichier: `android/app/src/main/java/com/mbote/app/MainActivity.java`.

### Faible - Regressions UI mobile detectees par tests

- Surface: detail chaine Actus et liste messages dashboard.
- Risque: actions mal centrees et liste messages passant au-dessus du header sticky.
- Correction: restauration des dimensions et z-index attendus par les tests.
- Fichiers: `src/pages/ActusPage.css`, `src/components/Dashboard.tsx`.

## Points deja couverts par le code existant

- JWT HTTP et Socket.IO verifies avec session serveur.
- Cookie auth `HttpOnly` et guard CSRF base sur origine/referer pour cookies.
- CORS limite aux origines configurees et aux reseaux locaux hors production.
- Upload video limite a 50 MB et MIME video.
- Controle d'appartenance sur chats, messages, reunions et signaux WebRTC de groupe.
- Roles admin/moderateur via permissions applicatives.
- `npm audit --audit-level=moderate`: aucune vulnerabilite dependance connue.

## Risques restants

- Moyen: l'audit n'a pas execute de tests reels multi-appareils pour appels audio/video, GPS, push et media permissions.
- Moyen: la protection anti-capture est native Android. Le web/PWA dans un navigateur ne peut pas bloquer totalement les captures d'ecran au niveau OS.
- Faible: le scan statique a ete parent-agent seulement; pas de ledger exhaustif par fichier.

## Tests executes

- `npm.cmd audit --audit-level=moderate` - OK, 0 vulnerabilite.
- `npm.cmd run lint` - OK.
- `npm.cmd run test:api-v1` - OK, 98/98.
- `git diff --check` - OK.
- `npm.cmd run build` - OK.
- `cmd /c gradle-8.14.3\bin\gradle.bat -p android assembleDebug` - OK.

## Notes Android

- `npx.cmd cap copy android` echoue sur ce poste avec `EPERM` lors de la suppression de `android/app/src/main/assets/public/cordova.js`.
- Contournement applique sans suppression: copie du contenu `dist` vers `android/app/src/main/assets/public`, puis validation que l'`index.html` Android pointe vers le bundle courant `index-D_5k3Oqj.js`.
- `android.overridePathCheck=true` a ete ajoute pour permettre le build Gradle dans le chemin local contenant `mbote`.
- `android/capacitor-cordova-android-plugins/cordova.variables.gradle` a ete ajoute comme placeholder car le projet Cordova Capacitor l'importe pendant la compilation.
