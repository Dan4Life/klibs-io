package io.klibs.app.job

import io.klibs.app.service.SuspiciousForkBanService
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty("klibs.indexing", havingValue = "true")
class BanSuspiciousForksJob(
    private val suspiciousForkBanService: SuspiciousForkBanService,
) {

    @Scheduled(initialDelay = 0, fixedRate = 1, timeUnit = TimeUnit.DAYS)
    @SchedulerLock(name = "banSuspiciousForksLock", lockAtMostFor = "30m")
    fun banSuspiciousForks() {
        LockAssert.assertLocked()
        val summary = suspiciousForkBanService.banConfirmedForks()
        logger.info(
            "Banned suspicious forks: evaluated={}, banned={}, notFork={}, noDecision={}, banRefused={}",
            summary.evaluated,
            summary.banned,
            summary.notFork,
            summary.noDecision,
            summary.banRefused,
        )
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(BanSuspiciousForksJob::class.java)
    }
}
