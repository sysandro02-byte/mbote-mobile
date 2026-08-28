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

### 2. Démarrage du serveur
```bash
npm start
```
Le serveur sera disponible sur `http://localhost:8080` (ou `http://10.0.2.2:8080` pour l'émulateur Android).

---

## 🔑 Variables d'Environnement & Sécurité
- **`PORT`** : Port d'écoute HTTP (défaut : `8080`).
- **`JWT_SECRET`** : Clé de chiffrement des tokens JWT de session utilisateur.
- **`ADMIN_KEY`** : Clé secrète d'accès Administrateur MBoté (défaut : `MBOTE-ADMIN-2026`).
- **`DATABASE_URL`** : Chaîne de connexion PostgreSQL / Supabase.

---

## 📱 Connexion depuis l'Application Android MBoté
Depuis l'application MBoté :
1. Allez dans **Paramètres** ou sur la page **Connexion**.
2. Cliquez sur **Espace Administration & Modération 🔐**.
3. Renseignez la clé admin (`MBOTE-ADMIN-2026`).
4. Dans le panneau de configuration, vous pouvez modifier l'URL du serveur en direct (ex: `https://votre-domaine.com/v1`).
