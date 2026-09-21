# MBoté — Validation publique et bêta fermée

Dernière mise à jour : 21 septembre 2026

Ce document est la procédure de livraison publique. Il complète les workflows GitHub Actions et sépare les contrôles automatisables des validations physiques sur appareils.

## 1. Gates automatisés obligatoires

Avant toute release publique :

- `ci/production-audit.sh` doit réussir.
- Les tests backend doivent réussir.
- Le smoke test de production doit réussir.
- Les tests unitaires Android et le lint doivent réussir.
- L'APK QA doit être signé et installable.
- Aucun secret serveur ne doit être présent dans Android.
- Le package public doit être `com.loukatech.mbote`.
- Le workflow `release-android.yml` doit produire un APK Release et un AAB Release signés.
- `versionCode` doit être strictement supérieur à la version déjà publiée.
- Les empreintes SHA-256 des artefacts doivent être conservées avec la release.

## 2. TURN / WebRTC

Les credentials TURN sont des secrets d'exploitation serveur. Ils ne doivent jamais être intégrés à `BuildConfig`, aux resources Android ou au dépôt.

Android charge sa configuration ICE via :

`GET /v1/rtc/ice-servers`

Contraintes :

- endpoint authentifié ;
- réponse `Cache-Control: no-store, private` ;
- secrets TURN uniquement dans les variables Render ;
- rotation du credential TURN sans reconstruction de l'APK ;
- après rotation, ouvrir un nouveau LIVE/appel pour forcer la récupération de la nouvelle configuration.

## 3. Smoke test terrain — deux appareils

Minimum :

- Téléphone A : Wi-Fi.
- Téléphone B : réseau mobile MTN ou Airtel.
- Refaire ensuite le test en inversant les réseaux si possible.

Parcours à valider sur un compte neuf puis un compte existant :

1. inscription ;
2. réception OTP e-mail ;
3. vérification OTP ;
4. connexion ;
5. Actus : texte, photo, audio, vidéo ;
6. LIVE : démarrage diffuseur, arrivée spectateur, audio/vidéo, commentaires/réactions, compteur, arrêt ;
7. Messages : texte, média, accusé de lecture, saisie, réception temps réel ;
8. Appel audio ;
9. Appel vidéo ;
10. réunion : création, rejoindre, quitter, fin ;
11. statuts ;
12. ShortMBoté ;
13. Luna ;
14. emplois ;
15. profil ;
16. paramètres ;
17. déconnexion ;
18. reconnexion.

Pour le LIVE et les appels, vérifier au minimum un scénario où les deux appareils sont sur des réseaux différents afin de valider le relais TURN.

## 4. Tests de résilience réseau

Sur les fonctions temps réel :

- couper le Wi-Fi pendant une session et passer aux données mobiles ;
- réactiver le réseau après 10 à 30 secondes ;
- vérifier la reconnexion WebSocket ;
- vérifier qu'aucun message n'est perdu après reconnexion ;
- vérifier présence en ligne/hors ligne ;
- vérifier accusés de lecture ;
- vérifier appel manqué ;
- vérifier notification lorsque l'application est en arrière-plan ;
- vérifier reconnexion LIVE ;
- vérifier que le compteur de spectateurs revient à une valeur correcte après départ/reconnexion.

## 5. Audit zéro simulation

Toute donnée présentée comme donnée utilisateur doit provenir de l'API/PostgreSQL ou être un état local UI légitime.

Le pipeline refuse notamment :

- `example.invalid` ;
- `default_live` ;
- profils de démonstration connus ;
- images Unsplash utilisées comme identités de démonstration ;
- fallback TURN public ;
- clés API intégrées au code Android ;
- références Android aux secrets TURN, Gemini, Brevo, JWT ou Supabase service-role.

Les fichiers historiques de compatibilité peuvent rester présents uniquement s'ils renvoient des listes vides et si les écrans chargent leurs données depuis le backend.

## 6. Sécurité avant lancement

À faire après la validation bêta et avant l'ouverture publique :

- faire tourner le credential TURN déjà utilisé pendant le développement ;
- faire tourner toute clé ayant été exposée dans une conversation, un terminal, un screenshot ou un fichier non sûr ;
- conserver `JWT_SECRET`, Gemini, Brevo, Supabase service-role et clés de paiement uniquement sur Render ;
- conserver la clé de signature Release hors dépôt ;
- sauvegarder la clé de signature Release dans au moins deux emplacements sûrs ;
- ne jamais remplacer cette clé après la première publication Play Store sauf procédure officielle de rotation ;
- tester la suppression de compte et vérifier que l'URL publique `/account-deletion` fonctionne.

## 7. Play Store

Éléments à préparer/vérifier :

- nom : MBoté ;
- package : `com.loukatech.mbote` ;
- icône haute résolution ;
- feature graphic ;
- captures téléphone ;
- description courte et longue ;
- e-mail support ;
- URL politique de confidentialité : `/privacy` ;
- URL suppression de compte : `/account-deletion` ;
- classification du contenu ;
- Data Safety ;
- déclaration caméra ;
- déclaration microphone ;
- déclaration localisation si la fonction est maintenue ;
- déclaration contacts si la synchronisation est maintenue ;
- déclaration notifications ;
- déclaration foreground service `specialUse` si conservée ;
- justification claire de chaque permission réellement nécessaire.

Toute permission non indispensable au produit final doit être supprimée du manifeste avant publication.

## 8. Bêta fermée

Cible minimale recommandée : 20 à 50 testeurs sur plusieurs modèles Android.

Couverture :

- MTN ;
- Airtel ;
- Wi-Fi domestique ;
- Android 8+ jusqu'aux versions récentes ;
- appareils faibles en mémoire ;
- appareils récents ;
- caméra avant/arrière ;
- notifications écran verrouillé.

Critères de sortie de bêta :

- aucun crash bloquant connu ;
- inscription/OTP/connexion fiables ;
- publications fiables ;
- messages temps réel fiables ;
- appels audio/vidéo validés sur réseaux différents ;
- LIVE validé sur réseaux différents ;
- notifications arrière-plan validées ;
- aucun secret serveur dans l'APK ;
- aucune donnée fictive visible ;
- backend stable sans hausse anormale de 5xx ;
- latence PostgreSQL acceptable ;
- consommation batterie/data jugée acceptable sur les parcours principaux.

## 9. Ordre de publication

1. Fusionner uniquement une CI verte.
2. Vérifier le déploiement Render.
3. Installer le nouvel APK QA.
4. Exécuter le smoke test terrain.
5. Faire tourner les secrets exposés.
6. Lancer la bêta fermée.
7. Corriger les anomalies remontées.
8. Exécuter `Production Android release` avec le nouveau `versionCode`.
9. Charger l'AAB signé dans Google Play.
10. Vérifier Data Safety, permissions et fiche Store.
11. Publier progressivement.
