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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.http.Status
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.ExecutionContext.Implicits.global

class MockSessionCacheSpec extends WordSpec with MustMatchers with ScalaFutures {

  val mockCache = new MockSessionCache
  val formId = "theForm"
  val data: Map[String, String] = Map("foo" -> "bar", "baz" -> "quix")
  val sessionId = SessionId(UUID.randomUUID().toString)
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(sessionId))

  "cache" should {

    "put JSON serializable value into in-memory cache using sessionId as key" in {
      mockCache.cache(formId, data).futureValue.getEntry[Map[String, String]](formId) must be(Some(data))
    }

  }

  "fetch" should {

    "return previously cached data from in-memory cache" in {
      whenReady(mockCache.cache(formId, data)) { _ =>
        mockCache.fetch().futureValue.get.getEntry[Map[String, String]](formId) must be(Some(data))
      }
    }

  }

  "fetch and get entry" should {

    "return value from previously cached data" in {
      whenReady(mockCache.cache(formId, data)) { _ =>
        mockCache.fetchAndGetEntry[Map[String, String]](formId).futureValue must be(Some(data))
      }
    }

  }

  "remove" should {

    "remove previously cached data from in-memory cache" in {
      whenReady(mockCache.cache(formId, data)) { _ =>
        whenReady(mockCache.remove()) { response =>
          response.status must be(Status.ACCEPTED)
          mockCache.fetchAndGetEntry[Map[String, String]](formId).futureValue must be(None)
        }
      }
    }

  }

}
