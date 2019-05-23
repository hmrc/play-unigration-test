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
import play.api.Application
import play.api.http.{HeaderNames, HttpErrorHandler, Writeable}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class RequestHandlerBehavioursSpec extends WordSpec with MustMatchers with Results with GuiceOneAppPerSuite with ScalaFutures {

  "with request" should {

    "route request with given method and uri then execute test with the result" in new RequestScenario() {
      behaviours.withRequest(method, uri) { res =>
        res.futureValue must be(result)
      }
    }

    "add headers to routed request" in new RequestScenario(headers = hdrs) {
      behaviours.withRequest(method, uri, headers = hdrs) { _ =>
        actualRequest.get.headers(HeaderNames.ACCEPT_LANGUAGE) must be("en-GB")
      }
    }

    "add session to routed request" in new RequestScenario(session = sess) {
      behaviours.withRequest(method, uri, session = sess) { _ =>
        actualRequest.get.session.data("foo") must be("bar")
      }
    }

    "add tags to routed request" in new RequestScenario(requestTags = tgs) {
      behaviours.withRequest(method, uri, tags = tgs) { _ =>
        actualRequest.get.tags("baz") must be("quix")
      }
    }

    "delegate routing exceptions to error handler" in new RequestScenario(result = InternalServerError, maybeException = Some(exception)) {
      behaviours.withRequest(method, uri) { res =>
        status(res) must be(INTERNAL_SERVER_ERROR)
        serverError must be(true)
        thrownException.get must be(exception)
      }
    }

  }

  "with request and form body" should {

    "route request with given method and uri then execute test with the result" in new RequestScenario() {
      behaviours.withRequestAndFormBody(method, uri) { res =>
        res.futureValue must be(result)
      }
    }

    "add headers to routed request" in new RequestScenario() {
      behaviours.withRequestAndFormBody(method, uri, headers = hdrs) { _ =>
        actualRequest.get.headers(HeaderNames.ACCEPT_LANGUAGE) must be("en-GB")
      }

    }

    "add session to routed request" in new RequestScenario() {
      behaviours.withRequestAndFormBody(method, uri, session = sess) { _ =>
        actualRequest.get.session.data("foo") must be("bar")
      }
    }

    "add tags to routed request" in new RequestScenario() {
      behaviours.withRequestAndFormBody(method, uri, tags = tgs) { _ =>
        actualRequest.get.tags("baz") must be("quix")
      }
    }

    "add form body to routed request" in new RequestScenario(formBody = form) {
      behaviours.withRequestAndFormBody(method, uri, body = form) { _ =>
        actualRequest.get.body.asInstanceOf[AnyContentAsFormUrlEncoded].data must be(form.map(entry => entry._1 -> Seq(entry._2)))
      }
    }

    "delegate routing exceptions to error handler" in new RequestScenario(result = InternalServerError, maybeException = Some(exception)) {
      behaviours.withRequestAndFormBody(method, uri, body = form) { res =>
        status(res) must be(INTERNAL_SERVER_ERROR)
        serverError must be(true)
        thrownException.get must be(exception)
      }
    }

  }

  val hdrs: Map[String, String] = Map(HeaderNames.ACCEPT_LANGUAGE -> "en-GB")
  val sess: Map[String, String] = Map("foo" -> "bar")
  val tgs: Map[String, String] = Map("baz" -> "quix")
  val exception: Exception = new InternalServerException("It didn't work")
  val form: Map[String, String] = Map("quix" -> "foo")

  class RequestScenario(val result: Result = Ok,
                        val method: String = "GET",
                        val uri: String = "/",
                        val headers: Map[String, String] = Map.empty,
                        val session: Map[String, String] = Map.empty,
                        val requestTags: Map[String, String] = Map.empty,
                        val formBody: Map[String, String] = Map.empty,
                        val application: Application = app,
                        val maybeException: Option[Exception] = None) {

    var actualRequest: Option[Request[_]] = None
    var serverError = false
    var thrownException: Option[Throwable] = None

    val mockErrorHandler: HttpErrorHandler = new HttpErrorHandler {

      override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
        Future.successful(result)

      override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
        serverError = true
        thrownException = Some(exception)
        Future.successful(result)
      }

    }

    val behaviours = new RequestHandlerBehaviours {

      override val errorHandler: HttpErrorHandler = mockErrorHandler

      override private [behaviours] def doRoute[T](app: Application, req: Request[T])
                                                  (implicit w: Writeable[T]): Future[Result] = {
        val expectedRequest = if (formBody.nonEmpty) {
          FakeRequest(method, uri).
            withHeaders(headers.toSeq: _*).
            withSession(session.toSeq: _*).
            copyFakeRequest(tags = requestTags).
            withFormUrlEncodedBody(formBody.toSeq: _*)
        } else {
          FakeRequest(method, uri).
            withHeaders(headers.toSeq: _*).
            withSession(session.toSeq: _*).
            copyFakeRequest(tags = requestTags)
        }
        actualRequest = Some(req)
        if (isExpected(req, expectedRequest)) {
          if (maybeException.isDefined) Future.failed(maybeException.get) else Future.successful(result)
        } else throw new IllegalArgumentException("Unexpected routing request")
      }

      // only compare methhod and URI ... things like request bodies etc do not have straightforward equality
      private def isExpected[T](actual: Request[_], expected: FakeRequest[_]): Boolean =
        actual.method == expected.method &&
        actual.uri == expected.uri
    }

  }

}
