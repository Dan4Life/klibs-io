package io.klibs.app.service

import io.klibs.core.pckg.dto.projection.ForkBanCandidateView
import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SuspiciousForkBanService(
    private val suspiciousPackageCandidateRepository: SuspiciousPackageCandidateRepository,
    private val forkChecker: ForkChecker,
    private val suspiciousForkBanner: SuspiciousForkBanner,
) {

    fun banConfirmedForks(): ForkBanSummary {
        val candidates = suspiciousPackageCandidateRepository.findForkBanCandidates()
        val checks = mutableMapOf<Pair<String, String>, ForkCheckResult>()

        val outcomes = candidates.map { candidate ->
            val parentFullName = "${candidate.repoOwner}/${candidate.repoName}"
            val check = checks.getOrPut(candidate.suspectOwner.lowercase() to parentFullName.lowercase()) {
                forkChecker.checkIsForkOf(candidate.suspectOwner, candidate.repoName, parentFullName)
            }
            when (check) {
                ForkCheckResult.FORK -> ban(candidate)
                ForkCheckResult.NOT_FORK -> Outcome.NOT_FORK
                ForkCheckResult.NO_DECISION -> Outcome.NO_DECISION
            }
        }

        return ForkBanSummary(
            evaluated = candidates.size,
            banned = outcomes.count { it == Outcome.BANNED },
            notFork = outcomes.count { it == Outcome.NOT_FORK },
            noDecision = outcomes.count { it == Outcome.NO_DECISION },
            banRefused = outcomes.count { it == Outcome.BAN_REFUSED },
        )
    }

    private fun ban(candidate: ForkBanCandidateView): Outcome {
        val forkFullName = "${candidate.suspectOwner}/${candidate.repoName}"
        val banned = try {
            suspiciousForkBanner.ban(candidate, forkFullName)
        } catch (e: Exception) {
            logger.warn("Could not ban {}:{}", candidate.groupId, candidate.artifactId, e)
            false
        }
        if (!banned) {
            return Outcome.BAN_REFUSED
        }

        logger.info(
            "Banned {}:{}: {} is a fork of {}/{}",
            candidate.groupId,
            candidate.artifactId,
            forkFullName,
            candidate.repoOwner,
            candidate.repoName,
        )
        return Outcome.BANNED
    }

    private enum class Outcome { BANNED, NOT_FORK, NO_DECISION, BAN_REFUSED }

    private companion object {
        private val logger = LoggerFactory.getLogger(SuspiciousForkBanService::class.java)
    }
}

data class ForkBanSummary(
    val evaluated: Int,
    val banned: Int,
    val notFork: Int,
    val noDecision: Int,
    val banRefused: Int,
)
