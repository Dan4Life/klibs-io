package io.klibs.app.service

import io.klibs.integration.github.GitHubIntegration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ForkChecker(
    private val gitHubIntegration: GitHubIntegration,
) {

    fun checkIsForkOf(suspectOwner: String, repoName: String, parentFullName: String): ForkCheckResult {
        val repository = try {
            gitHubIntegration.getRepository(suspectOwner, repoName)
        } catch (e: Exception) {
            logger.warn("Could not look up GitHub repository {}/{}", suspectOwner, repoName, e)
            return ForkCheckResult.NO_DECISION
        }

        val isFork = repository != null &&
            repository.fork &&
            repository.parentFullName.equals(parentFullName, ignoreCase = true)
        return if (isFork) ForkCheckResult.FORK else ForkCheckResult.NOT_FORK
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ForkChecker::class.java)
    }
}

enum class ForkCheckResult {
    FORK,
    NOT_FORK,
    NO_DECISION,
}
