# Backend partagé MBoté

L’application Android n’exécute pas de serveur distinct en production. Elle consomme l’API canonique du projet MBoté existant :

`https://mbote-backend.onrender.com/api`

Cette API est montée par `server.ts` du projet MBoté et utilise la même configuration PostgreSQL et Supabase que le web/PWA. Ne copiez jamais son fichier `.env` dans ce dépôt : les clés de service, JWT, SMTP et Supabase restent uniquement côté serveur.

Configurez l’APK avec `MBOTE_API_BASE_URL` (et, si nécessaire, `VITE_SOCKET_URL`) en utilisant les valeurs publiques du projet principal. Les contrats à privilégier sont notamment :

- `/auth` pour l’authentification et les OTP ;
- `/chats`, `/messages` et `/contacts` pour les conversations et contacts ;
- `/actus`, `/short-videos`, `/meetings` et `/notifications` pour les surfaces sociales ;
- `/health` pour la vérification de disponibilité. La version modulaire est aussi exposée sous `/api/v1`.

Le dossier `backend/` de ce dépôt sert uniquement de référence locale et de tests de compatibilité. Il ne doit pas être déployé en parallèle du serveur MBoté principal.
