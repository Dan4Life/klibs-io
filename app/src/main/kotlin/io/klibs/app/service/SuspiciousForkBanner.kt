package io.klibs.app.service

import io.klibs.core.pckg.dto.projection.ForkBanCandidateView
import io.klibs.core.pckg.repository.SuspiciousPackageCandidateRepository
import io.klibs.core.project.blacklist.BlacklistService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SuspiciousForkBanner(
    private val suspiciousPackageCandidateRepository: SuspiciousPackageCandidateRepository,
    private val blacklistService: BlacklistService,
) {

    @Transactional
    fun ban(candidate: ForkBanCandidateView, forkFullName: String): Boolean {
        val reason = "Auto-banned: $forkFullName is a fork of ${candidate.repoOwner}/${candidate.repoName}"
        val resolved = suspiciousPackageCandidateRepository.markResolved(
            projectId = candidate.projectId,
            artifactId = candidate.artifactId,
            groupId = candidate.groupId,
            notes = reason,
        )
        if (resolved == 0) {
            return false
        }

        check(blacklistService.banPackage(candidate.groupId, candidate.artifactId, reason)) {
            "Ban of ${candidate.groupId}:${candidate.artifactId} was refused"
        }
        return true
    }
}
