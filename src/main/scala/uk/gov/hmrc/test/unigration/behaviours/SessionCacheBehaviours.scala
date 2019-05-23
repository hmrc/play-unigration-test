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

import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.test.unigration.UnigrationBase

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class SessionCacheBehaviours extends UnigrationBase {

  private [behaviours] lazy val sessionCache: SessionCache = new MockSessionCache

  def withSessionCache()(test: SessionCache => Unit): Unit = {
    test(sessionCache)
  }

  def withSessionCache[A](formId: String, body: A)(test: SessionCache => Unit)
                         (implicit wts: Writes[A], hc: HeaderCarrier): Unit = {
    whenReady(sessionCache.cache(formId, body)) { _ =>
      test(sessionCache)
    }
  }

  override protected def customise(builder: GuiceApplicationBuilder): GuiceApplicationBuilder =
    super.customise(builder).overrides(bind[SessionCache].to(sessionCache))
}

class MockSessionCache extends SessionCache {

  val cache: mutable.Map[String, CacheMap] = mutable.Map()

  override def defaultSource: String = "mockSessionCache"

  override def baseUri: String = "mockSessionCache"

  override def domain: String = "mockSessionCache"

  override def http: CoreGet with CorePut with CoreDelete = NoopHttpClient // ensure we throw on any attempted HTTP call

  // we ignore "source" for purposes of mocking
  override def cache[A](source: String, cacheId: String, formId: String, body: A)
                       (implicit wts: Writes[A], hc: HeaderCarrier, executionContext: ExecutionContext): Future[CacheMap] = {
    val entry = CacheMap(cacheId, Map(formId -> Json.toJson(body)))
    cache += (cacheId -> entry)
    Future.successful(entry)
  }

  override def fetch(source: String, cacheId: String)
                    (implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Option[CacheMap]] = {
    Future.successful(cache.get(cacheId))
  }

  override def remove()
                     (implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[HttpResponse] = {
    cache -= hc.sessionId.get.value
    Future.successful(HttpResponse(Status.ACCEPTED)) // TODO check what status keystore actually returns
  }

}

private [behaviours] object NoopHttpClient extends CoreGet with CorePut with CoreDelete {

  override def GET[A](url: String)
                     (implicit rds: HttpReads[A], hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    throw new UnsupportedOperationException()

  override def GET[A](url: String, queryParams: Seq[(String, String)])
                     (implicit rds: HttpReads[A], hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    throw new UnsupportedOperationException()

  override def PUT[I, O](url: String, body: I)
                        (implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier, ec: ExecutionContext): Future[O] =
    throw new UnsupportedOperationException()

  override def PUTString[O](url: String, body: String, headers: Seq[(String, String)])
                           (implicit rds: HttpReads[O], hc: HeaderCarrier, ec: ExecutionContext): Future[O] =
    throw new UnsupportedOperationException()

  override def DELETE[O](url: String)
                        (implicit rds: HttpReads[O], hc: HeaderCarrier, ec: ExecutionContext): Future[O] =
    throw new UnsupportedOperationException()

}
