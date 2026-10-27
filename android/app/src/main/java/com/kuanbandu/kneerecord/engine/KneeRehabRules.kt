package com.kuanbandu.kneerecord.engine

import com.kuanbandu.kneerecord.model.Assessment
import com.kuanbandu.kneerecord.model.Exercise
import com.kuanbandu.kneerecord.model.ExerciseCategory
import com.kuanbandu.kneerecord.model.Slot
import com.kuanbandu.kneerecord.model.Stage

/** 训练须知勾选的有效时长 */
const val CONSENT_TTL_MS = 12 * 60 * 60 * 1000L

/** 疼痛达到这个分数开始限制动作类别 */
const val PAIN_GATE = 4

/** 疼痛达到这个分数应立即停止并考虑寻求专业帮助 */
const val PAIN_SEEK_HELP = 7

class ConsentRequiredError : IllegalStateException("训练须知未勾选或已超过 12 小时，请重新勾选") {
    val code: String = "E_CONSENT"
}

class ExerciseBlockedError(val exerciseId: String, message: String) : IllegalStateException(message) {
    val code: String = "E_EXERCISE"
}

/** 本次允许的边界 */
data class Gate(
    val maxLevel: Int,
    val forbidden: Set<ExerciseCategory>,
    val levelBlockedCount: Int,
    val romBlockedCount: Int,
) {
    val forbiddenLabels: List<String> get() = forbidden.map { it.label }
    val blockedCount: Int get() = levelBlockedCount + romBlockedCount
}

data class SessionPlan(
    val slots: Map<Slot, List<Exercise>>,
    val exercises: List<Exercise>,
    val gate: Gate,
)

data class Session(
    val stage: Stage,
    val painBefore: Int,
    val exercises: List<Exercise>,
    val startedAt: Long,
)

data class PainAdvice(
    val stop: Boolean,
    val alternative: Exercise?,
    val seekHelp: Boolean,
    val canContinue: Boolean,
    val message: String,
)

object KneeRehabRules {

    fun isConsentValid(consentAt: Long, now: Long = System.currentTimeMillis()): Boolean =
        consentAt > 0L && now - consentAt in 0 until CONSENT_TTL_MS

    /** 阶段决定等级上限，疼痛决定禁练类别，两者共同决定动作可不可以出现在清单里 */
    fun buildGate(stage: Stage, painScore: Int, romFlexion: Int): Gate {
        val forbidden = mutableSetOf<ExerciseCategory>()
        if (painScore >= PAIN_GATE) {
            forbidden += ExerciseCategory.SQUAT
            forbidden += ExerciseCategory.LUNGE
            forbidden += ExerciseCategory.JUMP
        }
        if (painScore >= PAIN_SEEK_HELP) {
            forbidden += ExerciseCategory.BALANCE
        }

        val maxLevel = when {
            painScore >= PAIN_SEEK_HELP -> stage.maxLevel.coerceAtMost(1)
            painScore >= PAIN_GATE -> stage.maxLevel.coerceAtMost(2)
            else -> stage.maxLevel
        }

        val library = ExerciseLibrary.ALL
        val levelBlocked = library.count { it.level > maxLevel || it.category in forbidden }
        val romBlocked = library.count {
            it.level <= maxLevel && it.category !in forbidden && it.needsFlexion > romFlexion
        }
        return Gate(maxLevel, forbidden, levelBlocked, romBlocked)
    }

    /**
     * 只算清单不動其他狀態，清单页展示用。
     * 不做勾选校验 —— 看清单是安全的，开练才需要勾选。
     */
    fun previewSession(assessment: Assessment): SessionPlan {
        val stage = Stage.of(assessment.stage) ?: Stage.EARLY
        return composePlan(stage, assessment.painScore, assessment.romFlexion)
    }

    /** 开练：必须通过训练须知勾选 */
    fun startSession(
        assessment: Assessment,
        consentAt: Long,
        now: Long = System.currentTimeMillis(),
    ): Session {
        if (!isConsentValid(consentAt, now)) throw ConsentRequiredError()
        val stage = Stage.of(assessment.stage) ?: Stage.EARLY
        val plan = composePlan(stage, assessment.painScore, assessment.romFlexion)
        if (plan.exercises.isEmpty()) {
            throw IllegalStateException("当前条件下没有可做的动作，请重新评估")
        }
        return Session(stage, assessment.painScore, plan.exercises, now)
    }

    /** 单独跟练某一个动作：同样要过勾选与门禁 */
    fun startSingleExerciseSession(
        assessment: Assessment,
        exerciseId: String,
        consentAt: Long,
        now: Long = System.currentTimeMillis(),
    ): Session {
        if (!isConsentValid(consentAt, now)) throw ConsentRequiredError()
        val stage = Stage.of(assessment.stage) ?: Stage.EARLY
        val gate = buildGate(stage, assessment.painScore, assessment.romFlexion)
        val exercise = ExerciseLibrary.byId(exerciseId)
            ?: throw ExerciseBlockedError(exerciseId, "动作不在动作库里")

        if (exercise.level > gate.maxLevel) {
            throw ExerciseBlockedError(
                exerciseId,
                "疼痛 ${assessment.painScore} 分时，这个动作超出当前阶段允许的范围",
            )
        }
        if (exercise.category in gate.forbidden) {
            throw ExerciseBlockedError(
                exerciseId,
                "疼痛 ${assessment.painScore} 分时，${exercise.category.label}类动作不适合做",
            )
        }
        if (exercise.needsFlexion > assessment.romFlexion) {
            throw ExerciseBlockedError(
                exerciseId,
                "这个动作需要约 ${exercise.needsFlexion}° 屈膝活动度，你目前自评 ${assessment.romFlexion}°",
            )
        }
        return Session(stage, assessment.painScore, listOf(exercise), now)
    }

    /** 跟练过程中的疼痛上报处置 */
    fun handlePainReport(session: Session, pain: Int): PainAdvice = when {
        pain >= PAIN_SEEK_HELP -> PainAdvice(
            stop = true,
            alternative = null,
            seekHelp = true,
            canContinue = false,
            message = "疼痛 $pain 分：立即停止本次训练。若持续不缓解，建议尽早寻求专业帮助。",
        )
        pain >= PAIN_GATE -> PainAdvice(
            stop = true,
            alternative = ExerciseLibrary.byId(ExerciseLibrary.SAFE_ALTERNATIVE_ID),
            seekHelp = false,
            canContinue = true,
            message = "疼痛 $pain 分：停止当前动作，可改做低强度动作；如果休息后仍不缓解就结束本次训练。",
        )
        else -> PainAdvice(
            stop = false,
            alternative = null,
            seekHelp = false,
            canContinue = true,
            message = "疼痛 $pain 分：可以继续，保持动作标准。",
        )
    }

    private fun composePlan(stage: Stage, painScore: Int, romFlexion: Int): SessionPlan {
        val gate = buildGate(stage, painScore, romFlexion)
        val allowed = ExerciseLibrary.ALL.filter {
            it.level <= gate.maxLevel &&
                it.category !in gate.forbidden &&
                it.needsFlexion <= romFlexion
        }

        val slots = linkedMapOf<Slot, List<Exercise>>()
        Slot.entries.forEach { slot ->
            val picked = pick(allowed.filter { it.slot == slot }, ExerciseLibrary.QUOTA[slot] ?: 0)
            if (picked.isNotEmpty()) slots[slot] = picked
        }
        val flat = slots.values.flatten()
        return SessionPlan(slots, flat, gate)
    }

    /** 先保证类别不重复（同类中等级高的优先），不够再按等级补齐 */
    private fun pick(candidates: List<Exercise>, want: Int): List<Exercise> {
        if (want <= 0) return emptyList()
        val ranked = candidates.sortedWith(compareByDescending<Exercise> { it.level }.thenBy { it.id })
        val out = mutableListOf<Exercise>()
        for (e in ranked) {
            if (out.size >= want) break
            if (out.none { it.category == e.category }) out.add(e)
        }
        for (e in ranked) {
            if (out.size >= want) break
            if (out.none { it.id == e.id }) out.add(e)
        }
        return out.sortedWith(compareBy<Exercise> { it.level }.thenBy { it.id })
    }
}
