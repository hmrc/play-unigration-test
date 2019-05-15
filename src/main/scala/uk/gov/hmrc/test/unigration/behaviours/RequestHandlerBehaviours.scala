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

import play.api.Application
import play.api.http.{HttpErrorHandler, Writeable}
import play.api.libs.crypto.CSRFTokenSigner
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.test.unigration.UnigrationBase

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait RequestHandlerBehaviours extends UnigrationBase {

  val errorHandler: HttpErrorHandler = component[HttpErrorHandler]

  val contextPath: String = "/professional-bodies"

  val GET: String = "GET"

  val POST: String = "POST"

  private val csrfTokenSigner: CSRFTokenSigner = component[CSRFTokenSigner]

  implicit def hc: HeaderCarrier = HeaderCarrier()

  def uriWithContextPath(path: String): String = s"$contextPath$path"

  def withRequest(method: String, uri: String,
                  headers: Map[String, String] = Map.empty,
                  session: Map[String, String] = Map.empty,
                  tags: Map[String, String] = Map.empty)
                 (test: Future[Result] => Unit): Unit = {
    val r = FakeRequest(method, uri).
      withHeaders(headers.toSeq: _*).
      withSession(session.toSeq: _*).
      copyFakeRequest(tags = tags)
    val res: Future[Result] = doRoute(app, r).recover {
      case e: Exception => delegateToErrorHandler(r, e)
    }
    test(res)
  }

  def withRequestAndFormBody(method: String, uri: String,
                             headers: Map[String, String] = Map.empty,
                             session: Map[String, String] = Map.empty,
                             tags: Map[String, String] = Map.empty,
                             body: Map[String, String] = Map.empty)
                            (test: Future[Result] => Unit): Unit = {
    val token = csrfTokenSigner.generateSignedToken
    val r = FakeRequest(method, uri).
      withHeaders((headers ++ Map("Csrf-Token" -> token)).toSeq: _*).
      withSession(session.toSeq: _*).
      copyFakeRequest(tags = tags ++ Map("CSRF_TOKEN" -> token)).
      withFormUrlEncodedBody(body.toSeq: _*)
    val res: Future[Result] = doRoute(app, r).recover {
      case e: Exception => delegateToErrorHandler(r, e)
    }
    test(res)
  }

  private def delegateToErrorHandler(r: Request[_], e: Exception): Result = Await.result(errorHandler.onServerError(r, e), 500.millis)

  private [behaviours] def doRoute[T](app: Application, req: Request[T])(implicit w: Writeable[T]): Future[Result] = route(app, req).get

}
