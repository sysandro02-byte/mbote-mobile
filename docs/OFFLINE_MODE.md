# Mode hors connexion MBote

## Architecture

Les donnees hors ligne sont stockees dans IndexedDB, base `mbote-offline`. Chaque cle commence par le `userId`, ce qui empeche une session de lire le cache d'un autre compte.

Le `NetworkProvider` combine `navigator.onLine` et `GET /api/health`. Il distingue `online`, `offline`, `slow`, `syncing` et `sync-error`. La synchronisation redemarre au lancement, au retour en ligne et au retour au premier plan.

## Schema IndexedDB v1

- `cachedPosts`: publications Actus.
- `cachedStatuses`: statuts valides.
- `cachedChannels`: chaines consultees.
- `cachedProfiles`: profils consultes.
- `cachedConversations`: discussions.
- `cachedMessages`: messages recents.
- `pendingActions`: actions, idempotencyKey, retryCount et statut.
- `cachedMediaMetadata`: metadonnees media.
- `syncMetadata`: preferences et derniere synchronisation.

Chaque entree contient `id`, `userId`, `data`, `updatedAt`, `cachedAt`, `expiresAt` et `version`.

## Strategies PWA

- shell: Cache First;
- scripts, styles, polices et icones: Stale While Revalidate;
- images: cache media separe;
- audio/video: reseau, avec fallback sur une copie existante;
- API: jamais mise en cache, fallback JSON HTTP 503;
- navigation: shell principal, puis `offline.html`.

Mots de passe, OTP, jetons, cookies, routes administratives et reponses API ne sont jamais ecrits dans Cache Storage.

## Disponible

Navigation et consultation des Actus, statuts, profils, conversations et messages deja charges. Preparation de publication texte, commentaire, reaction, message, profil non sensible et brouillon de statut texte. Synchronisation manuelle ou automatique.

## Indisponible

Authentification, OTP, appels, reunions, localisation, recherche serveur, nouveaux medias, paiements et IA serveur.

Le cache localStorage historique des discussions reste lu temporairement pour compatibilite. Les nouvelles actions passent par IndexedDB.
