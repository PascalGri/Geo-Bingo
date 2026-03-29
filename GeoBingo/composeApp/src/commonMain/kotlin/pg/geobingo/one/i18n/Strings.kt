package pg.geobingo.one.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Language(val code: String, val displayName: String) {
    DE("de", "Deutsch"),
    EN("en", "English"),
}

object S {
    var current: StringRes by mutableStateOf(De)
        private set
    var language: Language by mutableStateOf(Language.DE)
        private set

    fun switchLanguage(lang: Language) {
        language = lang
        current = when (lang) {
            Language.DE -> De
            Language.EN -> En
        }
    }
}

interface StringRes {
    // ── General ─────────────────────────────────────────────────────────
    val appName: String
    val back: String
    val close: String
    val cancel: String
    val confirm: String
    val save: String
    val delete: String
    val share: String
    val retry: String
    val loading: String
    val error: String
    val ok: String
    val you: String
    val points: String
    val pointsAbbrev: String
    val categories: String
    val players: String

    // ── Offline ─────────────────────────────────────────────────────────
    val noInternet: String

    // ── Home ────────────────────────────────────────────────────────────
    val createRound: String
    val joinRound: String
    val heroTagCapture: String
    val heroTagRate: String
    val heroTagWin: String
    val photoConsentDisclaimer: String
    val recentGames: String
    val showAll: String
    val settings: String
    val impressum: String
    val privacy: String
    val howToPlay: String
    val roundCode: String

    // ── Onboarding ──────────────────────────────────────────────────────
    val onboardingTitle1: String
    val onboardingBody1: String
    val onboardingTitle2: String
    val onboardingBody2: String
    val onboardingTitle3: String
    val onboardingBody3: String
    val onboardingTitle4: String
    val onboardingBody4: String
    val onboardingSkip: String
    val onboardingNext: String
    val onboardingStart: String

    // ── Mode Select ─────────────────────────────────────────────────────
    val gameMode: String
    val howDoYouWantToPlay: String
    val modeClassic: String
    val modeClassicSubtitle: String
    val modeClassicDesc: String
    val modeBlindBingo: String
    val modeBlindBingoSubtitle: String
    val modeBlindBingoDesc: String
    val modeWeirdCore: String
    val modeWeirdCoreSubtitle: String
    val modeWeirdCoreDesc: String
    val modeQuickStart: String
    val modeQuickStartSubtitle: String
    val modeQuickStartDesc: String
    val letsGo: String
    val whereDoYouPlay: String
    val whereDoYouPlaySolo: String
    val outdoor: String
    val indoor: String

    // ── Create Game ─────────────────────────────────────────────────────
    val nameAndAvatar: String
    val namePlaceholder: String
    val otherPlayersJoinViaCode: String
    val categoriesSelected: String // param: count
    val customCategory: String
    val templates: String
    val otherSuggestions: String
    val enterName: String
    val minCategoriesNeeded: String
    fun createRoundWithCategories(count: Int): String
    val quickStartCreateRound: String

    // ── Create Game Banners ─────────────────────────────────────────────
    val blindBingoActive: String
    val blindBingoActiveDesc: String
    val weirdCoreActive: String
    val weirdCoreActiveDesc: String
    val quickStartActive: String
    val quickStartActiveDesc: String
    val weirdCoreCategoryHint: String

    // ── Duration ────────────────────────────────────────────────────────
    val gameDuration: String
    fun minutesLabel(min: Int): String

    // ── Speed Bonus ─────────────────────────────────────────────────────
    val speedBonus: String
    val speedBonusDesc: String

    // ── Join Game ───────────────────────────────────────────────────────
    val enterCode: String
    val joinGame: String

    // ── Lobby ───────────────────────────────────────────────────────────
    val waitingRoom: String
    val leave: String
    fun playersCount(count: Int): String
    fun minPlayersNeeded(count: Int): String
    fun startGame(playerCount: Int): String
    val waitingForHost: String
    fun lobbyClosesIn(time: String): String
    val hostClosedLobby: String
    val codeCopied: String
    val shareCode: String
    val tapToCopy: String

    // ── Game Screen ─────────────────────────────────────────────────────
    fun foundCount(found: Int, total: Int): String
    val joker: String
    val useJoker: String
    val jokerTopicPrompt: String
    val jokerTopicPlaceholder: String
    val takePhoto: String
    fun voteToEnd(current: Int, total: Int): String
    fun votedToEnd(current: Int, total: Int): String
    val uploadFailed: String
    val voteFailed: String
    val youFoundAll: String
    val someoneFoundAll: String
    fun secondsRemaining(sec: Int): String

    // ── Review ──────────────────────────────────────────────────────────
    val voting: String
    val yourPhotoBeingRated: String
    val othersAreVoting: String
    fun votedOf(current: Int, total: Int): String
    val skipHostOption: String
    val noPhotoFound: String
    val noSubmissionSkipping: String
    val howWellDoesItFit: String
    val tapTheStars: String
    val doesntFitAtAll: String
    val barelyFits: String
    val fitsOkay: String
    val fitsWell: String
    val perfect: String
    val rate: String
    fun categoryOfTotal(current: Int, total: Int): String
    fun playerOfTotal(current: Int, total: Int): String

    // ── Results ─────────────────────────────────────────────────────────
    val results: String
    val wins: String
    val allResults: String
    val bestPhoto: String
    val allPhotos: String
    val rematchSameCategories: String
    val newGame: String
    val watchBonus: String
    val shareResultText: String
    val showYourSkills: String

    // ── Results Detail ──────────────────────────────────────────────────
    val categoryBreakdown: String
    val speedBonusLabel: String
    val averageRating: String
    val controversialPhoto: String
    val bestInCategory: String

    // ── Rematch Voting ──────────────────────────────────────────────────
    val rematch: String
    val voteForRematch: String
    val sameCategories: String
    val newCategories: String
    val rematchVoteQuestion: String
    fun rematchVotes(current: Int, total: Int): String
    val waitingForRematchVotes: String

    // ── History ─────────────────────────────────────────────────────────
    val gameHistory: String
    val noGamesYet: String
    val playedGamesAppearHere: String
    val photos: String
    val noPhotosAvailable: String

    // ── How to Play ─────────────────────────────────────────────────────
    val howToPlayTitle: String
    val exploreYourCity: String
    val step1Title: String
    val step1Body: String
    val step2Title: String
    val step2Body: String
    val step3Title: String
    val step3Body: String
    val step4Title: String
    val step4Body: String
    val step5Title: String
    val step5Body: String
    val speedBonusTipTitle: String
    val speedBonusTipBody: String
    val tipsTitle: String
    val tip1: String
    val tip2: String
    val tip3: String

    // ── Settings ────────────────────────────────────────────────────────
    val settingsTitle: String
    val general: String
    val soundEffects: String
    val soundEffectsDesc: String
    val hapticFeedback: String
    val hapticFeedbackDesc: String
    val advertising: String
    val adSettings: String
    val adSettingsDesc: String
    val support: String
    val contact: String
    val legal: String
    val privacyPolicy: String
    val language: String
    val languageDesc: String

    // ── Stats ───────────────────────────────────────────────────────────
    val statsTitle: String
    val gamesPlayed: String
    val gamesWon: String
    val winRate: String
    val avgRating: String
    val longestStreak: String
    val noStatsYet: String

    // ── Quick Start Options ─────────────────────────────────────────────
    val duration10min: String
    val duration15min: String
    val duration20min: String
    val difficultyEasy: String
    val difficultyMedium: String
    val difficultyHard: String
    val previewCategories: String

    // ── Team Mode ───────────────────────────────────────────────────────
    val teamMode: String
    val teamModeDesc: String
    val team: String
    fun teamName(number: Int): String
    val teamScore: String
    val selectTeams: String
    val createTeam: String
    val teamNamePlaceholder: String
    val joinTeam: String
    val noTeam: String
    val minTwoTeamsNeeded: String
    fun teamOfTotal(current: Int, total: Int): String
    fun capturedBy(name: String): String
    fun teamFoundCount(found: Int, total: Int): String

    // ── Chat / Reactions ────────────────────────────────────────────────
    val ready: String
    val hurryUp: String
    val niceShot: String
    val funny: String
    val wow: String
    val reactions: String

    // ── Empty States ────────────────────────────────────────────────────
    val emptyNoCategoriesHint: String
    val emptyLobbyHint: String
    val emptyNoPhotoRetryHint: String
    val emptyAllCaughtCelebration: String

    // ── Privacy ────────────────────────────────────────────────────────
    val namePrivacyHint: String

    // ── Rejoin ─────────────────────────────────────────────────────────
    val rejoinTitle: String
    val rejoinBody: String
    val rejoinButton: String
    val rejoinDismiss: String
    val rejoining: String

    // ── Ad Rewards ─────────────────────────────────────────────────────
    val adRewardReceived: String

    // ── Solo / Offline Mode ──────────────────────────────────────────
    val soloMode: String
    val soloModeSubtitle: String
    val soloModeDesc: String
    val soloChallengeComplete: String
    val soloLeaderboard: String
    val soloNoScoresYet: String
    val soloYourBestScore: String
    val soloGlobalBest: String
    val soloStartChallenge: String
    fun soloTimeBonus(seconds: Int): String
    val soloTotalScore: String
    val soloYourRank: String
    fun soloRankDisplay(rank: Int, total: Int): String
    fun soloApproxRank(bracket: String): String

    // ── Transition Screens ──────────────────────────────────────────────
    val reviewInProgress: String
    val calculatingResults: String
    val getReady: String
    val gameStartsNow: String

    // ── Stars / Monetization ──────────────────────────────────────────
    val stars: String
    val earnStars: String
    val earnStarsDesc: String
    fun adsRemaining(count: Int): String
    val watchVideo: String
    val useSkipCard: String
    fun skipCardsRemaining(count: Int): String
    val dailyLoginBonus: String
    fun dailyLoginBonusReward(amount: Int): String
    val removeAds: String
    val removeAdsPrice: String
    val removeAdsDesc: String
    val adsRemoved: String
    val restorePurchases: String
    val purchaseSuccess: String
    val purchaseFailed: String
    val rerollCategory: String
    val newSuggestions: String
    fun rerollCost(stars: Int): String
    fun newSuggestionsCost(stars: Int): String
    val orWatchVideo: String
    val notEnoughStars: String
    val extremeMode: String
    val extremeModeSubtitle: String
    val extremeModeDesc: String
    fun unlockWithStars(stars: Int): String
    val unlockWithVideos: String
    val locked: String
    val dailyChallenge: String
    val dailyChallengeDesc: String
    val dailyChallengeCompleted: String
    fun dailyChallengeReward(stars: Int): String
    val challengeWinRound: String
    val challengePlayMode: String
    val challengeCaptureCategories: String
    val shop: String
    val buyStars: String
    val adSkipper: String
    fun starsPackage(stars: Int, price: String): String
    fun skipCardsPackage(cards: Int, price: String): String
    val interstitialDisabledNote: String
    val starsEarned: String

    // ── Account ───────────────────────────────────────────────────
    val account: String
    val signIn: String
    val signUp: String
    val signOut: String
    val emailPlaceholder: String
    val passwordPlaceholder: String
    val accountCreated: String
    val signedIn: String
    val signedOut: String
    val syncData: String
    val syncDataDesc: String
    val notLoggedIn: String
    val loggedInAs: String
    val authError: String
    val passwordTooShort: String

    // ── Profile Setup ───────────────────────────────────────────
    val profileSetupTitle: String
    val profileSetupSubtitle: String
    val profileSetupNameLabel: String
    val profileSetupAvatarLabel: String
    val profileSetupComplete: String
    val profileSetupSkip: String
    val profileSetupNameRequired: String
    val editProfile: String
    val profileUpdated: String
    val displayName: String
    val changeAvatar: String
    val removeAvatar: String

    // ── OAuth / Auth Providers ───────────────────────────────────
    val continueWithGoogle: String
    val continueWithApple: String
    val orContinueWithEmail: String
    val forgotPassword: String
    val resetPassword: String
    val resetPasswordDesc: String
    val resetPasswordSent: String
    val resetPasswordError: String
    val deleteAccount: String
    val deleteAccountConfirm: String
    val deleteAccountDesc: String
    val accountDeleted: String

    // ── Account Management ──────────────────────────────────────────
    val changeEmail: String
    val changeEmailDesc: String
    val newEmail: String
    val emailChanged: String
    val emailChangeError: String
    val changePassword: String
    val changePasswordDesc: String
    val currentPassword: String
    val newPassword: String
    val confirmPassword: String
    val passwordChanged: String
    val passwordChangeError: String
    val passwordsDoNotMatch: String
    val inviteFriends: String
    val inviteFriendsDesc: String
    val inviteFriendsMessage: String
}
