/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.test.unigration.behaviours

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, Enrolments, NoActiveSession}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.test.unigration.{UnigrationFixtures, UserFixture}

import scala.concurrent.ExecutionContext.Implicits.global

class AuthenticationBehavioursSpec extends WordSpec with MustMatchers with GuiceOneAppPerSuite with UnigrationFixtures with ScalaFutures {

  "with signed-in user" should {

    val anEnrolment = Enrolments(Set(Enrolment("HMRC_FOO")))

    "match expected user with auth connector" in new AuthenticationScenario {
      behaviours.withSignedInUser(maybeUser.get.copy(enrolments = anEnrolment)) { (_, _, _) =>
        val actual: Credentials ~ Name ~ Option[String] ~ Option[AffinityGroup] ~ Option[String] ~ Enrolments = behaviours.
          authConnector.
          authorise(
            Enrolment("HMRC_FOO"),
            credentials and name and email and affinityGroup and internalId and allEnrolments
          ).futureValue
        actual must be(new ~(new ~(new ~(new ~(new ~(maybeUser.get.credentials, maybeUser.get.name), maybeUser.get.email), maybeUser.get.affinityGroup), maybeUser.get.internalId), anEnrolment))
      }
    }

    "set auth headers" in new AuthenticationScenario {
      behaviours.withSignedInUser() { (hdrs, _, _) =>
        hdrs(behaviours.cfg.headerName) must be(behaviours.token)
      }
    }

    "set user session ID" in new AuthenticationScenario {
      behaviours.withSignedInUser() { (_, session, _) =>
        session.get(SessionKeys.sessionId).isDefined must be(true)
      }
    }

    "set user internal ID in session" in new AuthenticationScenario {
      behaviours.withSignedInUser(maybeUser.get) { (_, session, _) =>
        session(SessionKeys.userId) must be(maybeUser.get.internalId.get)
      }
    }

    "set auth tags" in new AuthenticationScenario {
      behaviours.withSignedInUser() { (_, _, tags) =>
        tags must be(behaviours.authenticationTags)
      }
    }

  }

  "without signed-in user" should {

    "setup not logged in exception with auth connector" in new AuthenticationScenario {
      behaviours.withoutSignedInUser() {
        whenReady(behaviours.authConnector.authorise(
          Enrolment("HMRC_FOO"),
          credentials and name and email and affinityGroup and internalId and allEnrolments
        ).failed) { ex =>
          ex must be(a[NoActiveSession])
          ex.getMessage must be(behaviours.notLoggedInException.getMessage)
        }
      }
    }

  }

  class AuthenticationScenario(val maybeUser: Option[UserFixture] = Some(userFixture())) {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val behaviours = new AuthenticationBehaviours {}
  }

}
