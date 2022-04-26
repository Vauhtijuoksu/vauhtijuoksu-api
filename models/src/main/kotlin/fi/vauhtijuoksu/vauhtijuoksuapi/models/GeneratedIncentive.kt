package fi.vauhtijuoksu.vauhtijuoksuapi.models

import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import java.util.UUID
import kotlin.random.Random

data class ChosenIncentive(
    val incentiveId: UUID,
    val parameter: String?,
)

data class IncentiveCode constructor(
    val code: String
) {
    init {
        if (!code.matches(codeFormat)) {
            throw ServerError("Code does not match format")
        }
    }

    companion object {
        private const val CODE_LENGTH = 4
        private const val BITS_IN_HEX_DIGIT = 4

        val codeFormat = Regex("""\{#Vj[A-F0-9]{$CODE_LENGTH}\}""")

        fun random(): IncentiveCode {
            val code = Integer.toHexString(Random.nextBits(CODE_LENGTH * BITS_IN_HEX_DIGIT))
                .uppercase()
                .padStart(CODE_LENGTH, '0')
            return IncentiveCode(
                "{#Vj$code}"
            )
        }
    }
}

data class GeneratedIncentive(
    val generatedCode: IncentiveCode,
    val chosenIncentives: List<ChosenIncentive>,
)
