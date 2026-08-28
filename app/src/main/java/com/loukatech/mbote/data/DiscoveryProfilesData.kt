package com.loukatech.mbote.data

import com.loukatech.mbote.model.DiscoverProfile

object DiscoveryProfilesData {
    val initialProfiles = listOf(
        DiscoverProfile(
            id = "disc_1",
            name = "Aïcha Malonga",
            age = 26,
            city = "Brazzaville",
            country = "Congo",
            avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
            bio = "Architecte d'intérieur passionnée d'art contemporain et de rumba congolaise 🎷. Toujours partante pour des conversations profondes sur la vie !",
            matchAffinity = 94,
            interests = listOf("Architecture", "Rumba", "Voyages", "Design", "Gastronomie"),
            languages = listOf("Français", "Lingala", "Anglais"),
            favoriteAronQuestion = "Que constituerait une journée « parfaite » pour vous ?"
        ),
        DiscoverProfile(
            id = "disc_2",
            name = "Dieudonné Bakala",
            age = 29,
            city = "Kinshasa",
            country = "RDC",
            avatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
            bio = "Ingénieur IA & passionné d'astrophysique 🌌. Curieux d'échanger avec des esprits novateurs et créatifs.",
            matchAffinity = 89,
            interests = listOf("Intelligence Artificielle", "Startups", "Lecture", "Sciences", "Échecs"),
            languages = listOf("Français", "Lingala", "Swahili"),
            favoriteAronQuestion = "Si une boule de cristal pouvait vous révéler la vérité sur votre futur, que voudriez-vous savoir ?"
        ),
        DiscoverProfile(
            id = "disc_3",
            name = "Elodie Ngoma",
            age = 24,
            city = "Pointe-Noire",
            country = "Congo",
            avatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&auto=format&fit=crop&q=80",
            bio = "Photographe de voyage et amoureuse de l'océan 🌊. Je cherche des amis pour explorer le monde et partager des idées positives.",
            matchAffinity = 91,
            interests = listOf("Photographie", "Nature", "Océan", "Cinéma", "Musique"),
            languages = listOf("Français", "Kituba", "Anglais"),
            favoriteAronQuestion = "Quel est votre souvenir le plus précieux ?"
        ),
        DiscoverProfile(
            id = "disc_4",
            name = "Kader Traoré",
            age = 31,
            city = "Abidjan",
            country = "Côte d'Ivoire",
            avatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
            bio = "Entrepreneur dans les fintechs africaines 💳. Fervent défenseur de l'entraide et de la bienveillance collective.",
            matchAffinity = 86,
            interests = listOf("Fintech", "Économie", "Sport", "Randonnée", "Podcasts"),
            languages = listOf("Français", "Anglais", "Dioula"),
            favoriteAronQuestion = "De quoi vous sentez-vous le plus reconnaissant dans votre vie ?"
        ),
        DiscoverProfile(
            id = "disc_5",
            name = "Sarah Mabiala",
            age = 27,
            city = "Brazzaville",
            country = "Congo",
            avatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&auto=format&fit=crop&q=80",
            bio = "Musicienne et cheffe de projet culturel 🎵. J'adore les débats philosophiques et la découverte de nouvelles cultures.",
            matchAffinity = 96,
            interests = listOf("Piano", "Chant", "Philosophie", "Littérature", "Mode"),
            languages = listOf("Français", "Lingala"),
            favoriteAronQuestion = "Complétez cette phrase : « J'aimerais avoir quelqu'un avec qui partager... »"
        )
    )
}
