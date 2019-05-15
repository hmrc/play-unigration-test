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

package uk.gov.hmrc.test.unigration.assertions

import org.scalatest.WordSpec
import org.scalatest.exceptions.TestFailedException
import play.api.http.HeaderNames
import play.api.mvc.Results

import scala.concurrent.Future

class HttpAssertionsSpec extends WordSpec with HttpAssertions with Results {

  "was not found" should {

    "match result with 404 status" in {
      wasNotFound(Future.successful(NotFound))
    }

    "not match result without 404 status" in {
      a[TestFailedException] must be thrownBy wasNotFound(Future.successful(Ok))
    }

  }

  "was OK" should {

    "match result with 200 status" in {
      wasOk(Future.successful(Ok))
    }

    "not match result without 200 status" in {
      a[TestFailedException] must be thrownBy wasOk(Future.successful(NotFound))
    }

  }

  "was redirected" should {

    "match result with See Other status and location header with given URI" in {
      wasRedirected("/foo", Future.successful(SeeOther("/foo")))
    }

    "not match result with expected status but unexpected location" in {
      a[TestFailedException] must be thrownBy wasRedirected("/foo", Future.successful(SeeOther("/bar")))
    }

    "not match result with unexpected status but expected location" in {
      a[TestFailedException] must be thrownBy wasRedirected("/foo", Future.successful(Ok.withHeaders(HeaderNames.LOCATION -> "/foo")))
    }

  }

}
