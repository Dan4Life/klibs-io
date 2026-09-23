package io.klibs.app.service

import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException
import java.time.Instant
import kotlin.test.assertEquals

class ForkCheckerTest {

    private val gitHubIntegration: GitHubIntegration = mock()

    private val uut = ForkChecker(gitHubIntegration)

    @Test
    fun `a fork of the expected repository is a fork`() {
        stubLookup(repository(fork = true, parentFullName = "cashapp/zipline"))

        assertEquals(ForkCheckResult.FORK, uut.checkIsForkOf("suspect", "zipline", "cashapp/zipline"))
    }

    @Test
    fun `parent names are compared case-insensitively`() {
        stubLookup(repository(fork = true, parentFullName = "CashApp/Zipline"))

        assertEquals(ForkCheckResult.FORK, uut.checkIsForkOf("suspect", "zipline", "cashapp/zipline"))
    }

    @Test
    fun `a fork of a different repository is not a fork`() {
        stubLookup(repository(fork = true, parentFullName = "someone-else/zipline"))

        assertEquals(ForkCheckResult.NOT_FORK, uut.checkIsForkOf("suspect", "zipline", "cashapp/zipline"))
    }

    @Test
    fun `a repository that is not a fork is not a fork`() {
        stubLookup(repository(fork = false, parentFullName = null))

        assertEquals(ForkCheckResult.NOT_FORK, uut.checkIsForkOf("suspect", "zipline", "cashapp/zipline"))
    }

    @Test
    fun `a missing repository is a completed not-a-fork`() {
        stubLookup(null)

        assertEquals(ForkCheckResult.NOT_FORK, uut.checkIsForkOf("suspect", "zipline", "cashapp/zipline"))
    }

    @Test
    fun `a failed lookup gives no decision`() {
        whenever(gitHubIntegration.getRepository("suspect", "zipline")).thenAnswer { throw IOException("rate limited") }

        assertEquals(ForkCheckResult.NO_DECISION, uut.checkIsForkOf("suspect", "zipline", "cashapp/zipline"))
    }

    private fun stubLookup(repository: GitHubRepository?) {
        whenever(gitHubIntegration.getRepository("suspect", "zipline")).thenReturn(repository)
    }

    private fun repository(fork: Boolean, parentFullName: String?) = GitHubRepository(
        nativeId = 2,
        name = "zipline",
        createdAt = Instant.EPOCH,
        defaultBranch = "trunk",
        owner = "suspect",
        hasGhPages = false,
        hasIssues = false,
        hasWiki = false,
        archived = false,
        stars = 0,
        lastActivity = Instant.EPOCH,
        fork = fork,
        parentFullName = parentFullName,
    )
}
