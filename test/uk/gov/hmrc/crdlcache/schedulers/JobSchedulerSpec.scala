package uk.gov.hmrc.crdlcache.schedulers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.quartz.Trigger.TriggerState
import org.quartz.impl.StdSchedulerFactory
import org.quartz.{Job, SchedulerFactory}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.crdlcache.config.AppConfig

import scala.concurrent.ExecutionContext

class JobSchedulerSpec
  extends AnyFlatSpec
  with Matchers
  with MockitoSugar
  with BeforeAndAfterEach
  with Eventually {
  given ExecutionContext = ExecutionContext.global

  private val schedulerFactory: SchedulerFactory = new StdSchedulerFactory()
  private var lifecycle: ApplicationLifecycle    = _
  private var jobFactory: ScheduledJobFactory    = _
  private var appConfig: AppConfig               = _
  private var jobScheduler: JobScheduler         = _

  override def beforeEach(): Unit = {
    // Clear down any job or schedule information
    schedulerFactory.getScheduler.clear()

    lifecycle = mock[ApplicationLifecycle]
    jobFactory = mock[ScheduledJobFactory]
    appConfig = mock[AppConfig]

    when(appConfig.importCodeListsSchedule).thenReturn("0 0 4 * * ? 2099")
    when(jobFactory.newJob(any(), any())).thenReturn(_ => Thread.sleep(50))

    jobScheduler = new JobScheduler(lifecycle, schedulerFactory, jobFactory, appConfig)
  }

  "JobScheduler.codeListImportStatus" should "return trigger status NORMAL when the job is not running" in {
    jobScheduler.codeListImportStatus() mustBe JobStatus(TriggerState.NORMAL)
  }

  it should "return the status BLOCKED when the job is running" in {
    jobScheduler.startCodeListImport()
    // Trigger state should go to blocked while the job is running
    eventually { jobScheduler.codeListImportStatus() mustBe JobStatus(TriggerState.BLOCKED) }
    // It should return to normal once the job ends
    eventually { jobScheduler.codeListImportStatus() mustBe JobStatus(TriggerState.NORMAL) }
  }
}
