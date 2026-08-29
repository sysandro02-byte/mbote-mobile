# MBoté Messenger - Backend API & Serveur de Production 🇨🇬

Ce dossier contient l'ensemble des éléments d'infrastructure serveur nécessaires pour connecter l'application Android **MBoté** à des vraies données en temps réel (Node.js, Express, PostgreSQL, Supabase, JWT, WebSockets).

---

## 📁 Architecture des fichiers
- **`server.js`** : Serveur d'API REST complet (Auth, Messages, Actus, Shorts, Emploi, Admin).
- **`package.json`** : Dépendances serveur (Express, CORS, JWT, PostgreSQL, WebSockets).
- **`schema.sql`** : Schéma SQL pour base de données PostgreSQL / Supabase.

---

## 🚀 Démarrage Rapide du Serveur Local / Cloud

### 1. Installation des dépendances
```bash
cd backend
npm install
```

### 2. Configurer les secrets et la base

Copiez `../.env.example` vers `../.env`, puis renseignez au minimum :

```dotenv
DATABASE_URL=postgresql://user:password@host:5432/mbote
DATABASE_SSL=true
JWT_SECRET=une-valeur-aleatoire-de-plus-de-32-caracteres
FRONTEND_URL=https://votre-domaine.example
MBOTE_API_BASE_URL=https://api.votre-domaine.example/v1
```

Appliquez ensuite `schema.sql` dans cette base PostgreSQL. Le serveur refuse de démarrer sans `DATABASE_URL` et `JWT_SECRET` valides : il n'existe ni compte de démonstration ni clé administrateur intégrée.

### 3. Démarrage du serveur
```bash
npm start
```
Le serveur sera disponible sur `http://localhost:8080` (ou `http://10.0.2.2:8080` pour l'émulateur Android).

---

## 🔑 Variables d'Environnement & Sécurité
- **`PORT`** : Port d'écoute HTTP (défaut : `8080`).
- **`JWT_SECRET`** : secret de signature JWT aléatoire d'au moins 32 caractères.
- **`DATABASE_URL`** : Chaîne de connexion PostgreSQL / Supabase.
- **`FRONTEND_URL`** : liste d'origines web autorisées, séparées par une virgule.

---

## 📱 Connexion depuis l'Application Android MBoté
Depuis l'application MBoté :
1. Définissez `MBOTE_API_BASE_URL` avant de compiler l'APK.
2. Connectez-vous avec un compte créé sur le serveur.
3. Les droits administrateur sont attribués au rôle `ADMIN` du compte dans PostgreSQL ; aucune clé universelle n'est acceptée.
