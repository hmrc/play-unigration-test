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

import java.util.UUID

import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

class SessionCacheBehavioursSpec extends WordSpec with MustMatchers {

  "with session cache" should {

    "add given data to session cache" in new CachingScenario {
      behaviours.withSessionCache[Map[String, String]](form, data) { cache =>
        cache.getEntry[Map[String, String]](form) must be(Some(data))
      }
    }

  }

  class CachingScenario() {
    val sessionId = SessionId(UUID.randomUUID().toString)
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(sessionId))
    val form = "theForm"
    val data = Map("foo" -> "bar", "baz" -> "quix")
    val behaviours = new SessionCacheBehaviours {}
  }

}
