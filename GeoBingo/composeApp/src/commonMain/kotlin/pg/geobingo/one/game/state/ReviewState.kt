package pg.geobingo.one.game.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pg.geobingo.one.network.CaptureDto
import pg.geobingo.one.network.VoteDto

class ReviewState {
    var reviewPlayerIndex by mutableStateOf(0)
    var reviewCategoryIndex by mutableStateOf(0)
    var allCaptures by mutableStateOf(listOf<CaptureDto>())
    var categoryVotes by mutableStateOf(mapOf<String, Boolean>())
    var hasSubmittedCurrentCategory by mutableStateOf(false)
    var hasVotedToEnd by mutableStateOf(false)
    var endVoteCount by mutableStateOf(0)
    var allCategoriesCaptured by mutableStateOf(false)
    var finishSignalDetected by mutableStateOf(false)
    var votes by mutableStateOf(mapOf<String, List<Boolean>>())
    var allVotes by mutableStateOf(listOf<VoteDto>())
}
