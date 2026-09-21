# MBoté — Production & Google Play release checklist

## URLs publiques
- API: https://mbote-backend.onrender.com/v1
- Politique de confidentialité: https://mbote-backend.onrender.com/privacy
- Suppression de compte: https://mbote-backend.onrender.com/account-deletion
- Conditions d’utilisation: https://mbote-backend.onrender.com/terms

## Smoke test terrain obligatoire
Valider sur au moins deux téléphones Android réels, idéalement un appareil sur Wi-Fi et un appareil sur réseau mobile.

1. Création de compte, e-mail OTP, connexion, déconnexion, reconnexion et mot de passe oublié.
2. Actus: lecture, publication texte, photo, vidéo, réaction, commentaire et partage.
3. Messages: discussion directe, groupe, média, audio, sondage, réaction, lecture et reprise après perte réseau.
4. Appels: audio, vidéo, refus, appel manqué, bascule caméra/micro et fin propre.
5. Réunions: création, rejoindre par code, audio/vidéo, sortie et reconnexion.
6. Statuts: création, lecture, réaction, commentaire, partage et expiration.
7. ShortMBoté: upload, lecture, like, commentaire, signet, partage et suivi du créateur.
8. LIVE: diffuseur caméra+micro, spectateur, commentaires, réactions, compteur, fermeture. Tester Wi-Fi ↔ réseau mobile afin de forcer l’usage TURN lorsque nécessaire.
9. Masta: suggestions réelles, demande, acceptation, refus/annulation, blocage et création de discussion.
10. Luna/IA, emplois, profil, notifications, paramètres, stockage et suppression de compte.

Un test est validé uniquement avec des données créées sur le serveur et relues depuis PostgreSQL après redémarrage de l’application.

## Secrets GitHub nécessaires au QA
- MBOTE_TURN_URL
- MBOTE_TURN_USERNAME
- MBOTE_TURN_CREDENTIAL
- VITE_SUPABASE_URL
- VITE_SUPABASE_ANON_KEY
- GOOGLE_CLIENT_ID (si OAuth Google activé)
- GITHUB_CLIENT_ID (si OAuth GitHub activé)

## Secrets supplémentaires pour une release Play signée
Créer une clé d’upload Android hors du dépôt puis ajouter:
- MBOTE_RELEASE_KEYSTORE_B64
- MBOTE_RELEASE_KEYSTORE_PASSWORD
- MBOTE_RELEASE_KEY_ALIAS
- MBOTE_RELEASE_KEY_PASSWORD

Ne jamais committer le fichier JKS ni ses mots de passe. Le workflow `.github/workflows/release-android.yml` produit l’APK et l’AAB signés.

## Google Play — fiche
Nom: **MBoté**
Description courte proposée: **Messagerie, réseau social, appels, réunions et contenus MBoté dans une seule application.**

La fiche longue doit décrire uniquement les fonctions réellement disponibles dans la version publiée.

## Data Safety — éléments à déclarer selon les fonctions activées
L’application peut traiter:
- informations de compte et coordonnées;
- contenu utilisateur: messages, publications, photos, vidéos, audio;
- contacts si l’utilisateur autorise la synchronisation;
- localisation si l’utilisateur choisit une fonction de partage de position;
- identifiants d’appareil / jetons de notification;
- données d’utilisation et diagnostics nécessaires au fonctionnement.

Les déclarations Play Console doivent être revérifiées à chaque ajout ou retrait d’un SDK.

## Bêta fermée
Avant production publique:
- distribuer l’AAB en piste de test fermée;
- tester plusieurs versions Android et constructeurs;
- tester MTN/Airtel et Wi-Fi lorsque disponible;
- consigner crash, latence, batterie, data, appels et LIVE;
- corriger tout bug bloquant ou perte de données avant promotion vers Production.

## Critères Go / No-Go
GO uniquement si:
- CI Quality checks verte;
- production source audit vert;
- backend Render LIVE;
- health API et WebSocket monitor verts;
- APK/AAB signés et vérifiés;
- aucun secret fournisseur sensible committé;
- smoke test terrain complet validé;
- suppression de compte et politique de confidentialité accessibles publiquement;
- aucun bug P0/P1 ouvert.
