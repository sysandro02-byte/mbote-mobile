package com.loukatech.mbote.data

import com.loukatech.mbote.model.MastaSubOption
import com.loukatech.mbote.model.MastaUser

object MastaData {

    val sampleAvatars = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=300&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=300&auto=format&fit=crop&q=80"
    )

    fun getInitialMastaUsers(): List<MastaUser> {
        return listOf(
            MastaUser(
                id = "linda_bongo",
                name = "Linda Bongo Ondimba",
                avatar = "https://images.unsplash.com/photo-1509631179647-0177331693ae?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "Personnalité publique • Libreville",
                mutualFriendsCount = 12,
                mutualFriendsAvatars = sampleAvatars.take(3),
                isOnline = true,
                city = "Libreville",
                subType = MastaSubOption.FRIENDS
            ),
            // 🟢 Online Friends (Image 1)
            MastaUser(
                id = "m1",
                name = "Bénie Emmalin Gamani",
                avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "Travaille chez MTN CONGO",
                mutualFriendsCount = 42,
                mutualFriendsAvatars = sampleAvatars.take(2),
                isOnline = true,
                city = "Brazzaville",
                subType = MastaSubOption.ONLINE
            ),
            MastaUser(
                id = "m2",
                name = "Jismie Ndebani",
                avatar = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "Travaille chez Banque mondiale",
                mutualFriendsCount = 28,
                mutualFriendsAvatars = sampleAvatars.take(3),
                isOnline = true,
                city = "Brazzaville",
                subType = MastaSubOption.ONLINE
            ),
            MastaUser(
                id = "m3",
                name = "Espoir Massamba",
                avatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "Habite à Brazzaville",
                mutualFriendsCount = 95,
                mutualFriendsAvatars = sampleAvatars.take(2),
                isOnline = true,
                city = "Brazzaville",
                subType = MastaSubOption.ONLINE
            ),
            MastaUser(
                id = "m4",
                name = "Christ Missie",
                avatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "Rois salomon, Étudiant",
                mutualFriendsCount = 14,
                mutualFriendsAvatars = sampleAvatars.take(1),
                isOnline = true,
                city = "Pointe-Noire",
                subType = MastaSubOption.ONLINE
            ),
            MastaUser(
                id = "m5",
                name = "C'y Mab",
                avatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "Ami(e) depuis juillet",
                mutualFriendsCount = 63,
                mutualFriendsAvatars = sampleAvatars.take(2),
                isOnline = true,
                city = "Kinshasa",
                subType = MastaSubOption.ONLINE
            ),

            // 📩 Received Invitations (Image 2 & Image 5)
            MastaUser(
                id = "r1",
                name = "Besle Parchet Tsionkiri",
                avatar = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "116 ami(e)s en commun",
                mutualFriendsCount = 116,
                mutualFriendsAvatars = sampleAvatars.take(2),
                timeBadge = "3 j",
                subType = MastaSubOption.RECEIVED
            ),
            MastaUser(
                id = "r2",
                name = "Campbell Falone",
                avatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "2 ami(e)s en commun",
                mutualFriendsCount = 2,
                mutualFriendsAvatars = sampleAvatars.take(2),
                timeBadge = "10 h",
                subType = MastaSubOption.RECEIVED
            ),
            MastaUser(
                id = "r3",
                name = "Lauvrey Baralonga",
                avatar = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "1 ami(e) en commun",
                mutualFriendsCount = 1,
                mutualFriendsAvatars = sampleAvatars.take(1),
                timeBadge = "8 h",
                subType = MastaSubOption.RECEIVED
            ),
            MastaUser(
                id = "r4",
                name = "Dieumira Abonga",
                avatar = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "1 ami(e) en commun",
                mutualFriendsCount = 1,
                mutualFriendsAvatars = sampleAvatars.take(1),
                timeBadge = "4 h",
                subType = MastaSubOption.RECEIVED
            ),
            MastaUser(
                id = "r5",
                name = "Bernard Carlos",
                avatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "1 ami(e) en commun",
                mutualFriendsCount = 1,
                mutualFriendsAvatars = sampleAvatars.take(1),
                timeBadge = "51 m",
                subType = MastaSubOption.RECEIVED
            ),
            MastaUser(
                id = "r6",
                name = "Diallo Ahallas San",
                avatar = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "1 ami(e) en commun",
                mutualFriendsCount = 1,
                mutualFriendsAvatars = sampleAvatars.take(1),
                timeBadge = "4 sem",
                subType = MastaSubOption.RECEIVED
            ),
            MastaUser(
                id = "r7",
                name = "Marlene Nyakaba",
                avatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "1 ami(e) en commun",
                mutualFriendsCount = 1,
                mutualFriendsAvatars = sampleAvatars.take(1),
                timeBadge = "2 a",
                subType = MastaSubOption.RECEIVED
            ),
            MastaUser(
                id = "r8",
                name = "Délicieux Obouaka",
                avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "68 ami(e)s en commun",
                mutualFriendsCount = 68,
                mutualFriendsAvatars = sampleAvatars.take(2),
                timeBadge = "31 sem",
                subType = MastaSubOption.RECEIVED
            ),

            // 📤 Sent Invitations (Image 3)
            MastaUser(
                id = "s1",
                name = "Murielle Do",
                avatar = "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "66 ami(e)s en commun",
                mutualFriendsCount = 66,
                timeBadge = "4 j",
                subType = MastaSubOption.SENT
            ),
            MastaUser(
                id = "s2",
                name = "Abigaïl Douniama",
                avatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "25 ami(e)s en commun",
                mutualFriendsCount = 25,
                timeBadge = "4 sem",
                subType = MastaSubOption.SENT
            ),
            MastaUser(
                id = "s3",
                name = "Jemina Ndoukou",
                avatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "59 ami(e)s en commun",
                mutualFriendsCount = 59,
                timeBadge = "4 sem",
                subType = MastaSubOption.SENT
            ),
            MastaUser(
                id = "s4",
                name = "Dieuleveult Nganvouli",
                avatar = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "27 ami(e)s en commun",
                mutualFriendsCount = 27,
                timeBadge = "5 sem",
                subType = MastaSubOption.SENT
            ),
            MastaUser(
                id = "s5",
                name = "Jokebed Poungui",
                avatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "43 ami(e)s en commun",
                mutualFriendsCount = 43,
                timeBadge = "5 sem",
                subType = MastaSubOption.SENT
            ),
            MastaUser(
                id = "s6",
                name = "Merveille Mikegna",
                avatar = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "16 ami(e)s en commun",
                mutualFriendsCount = 16,
                timeBadge = "5 sem",
                subType = MastaSubOption.SENT
            ),

            // 💡 Suggestions & Recommandations (Image 4)
            MastaUser(
                id = "sg1",
                name = "Divine Madecard",
                avatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "69 amis en commun",
                mutualFriendsCount = 69,
                mutualFriendsAvatars = sampleAvatars.take(2),
                subType = MastaSubOption.SUGGESTIONS
            ),
            MastaUser(
                id = "sg2",
                name = "D'or Okemba",
                avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "41 amis en commun",
                mutualFriendsCount = 41,
                mutualFriendsAvatars = sampleAvatars.take(2),
                subType = MastaSubOption.SUGGESTIONS
            ),
            MastaUser(
                id = "sg3",
                name = "Princia Mbemba",
                avatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "88 amis en commun • Pointe-Noire",
                mutualFriendsCount = 88,
                mutualFriendsAvatars = sampleAvatars.take(3),
                subType = MastaSubOption.RECOMMENDATIONS
            ),
            MastaUser(
                id = "sg4",
                name = "Yanick Mabiala",
                avatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
                infoSubtitle = "14 amis en commun • Brazzaville",
                mutualFriendsCount = 14,
                mutualFriendsAvatars = sampleAvatars.take(1),
                subType = MastaSubOption.RECOMMENDATIONS
            )
        )
    }
}
