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

import org.jsoup.Jsoup
import org.scalatest.WordSpec
import org.scalatest.exceptions.TestFailedException
import play.api.http.HttpVerbs
import play.api.libs.json.Json
import play.api.mvc.{Call, Results}
import play.twirl.api.Html

import scala.concurrent.Future

class HtmlAssertionsSpec extends WordSpec with HtmlAssertions with Results {

  val link = "https://google.com"
  val action = Call("GET", "/foo")
  val className = "quix"

  val someHtml = <html>
    <head></head>
    <body>
      <div>
        <form method="GET" action={action.url}>
          <fieldset>
            <input name="foo" type="text" value="bar"/>
          </fieldset>
        </form>
        <form method="POST" action={action.url}>
          <input name="bar" type="radio" value="baz"/>
        </form>
        <p><a href={link}>Click me</a></p>
        <p class={className}><a href={link}>Click me</a></p>
      </div>
    </body>
  </html>.mkString

  val htmlResult = Future.successful(Ok(Html(someHtml)))

  val someJson: String =
    """
      |{"property":"value"}
    """.stripMargin

  val jsonResult = Future.successful(Ok(Json.parse(someJson)))

  "was html" should {

    "match result with HTML content type" in {
      wasHtml(htmlResult)
    }

    "not match result with JSON content type" in {
      a[TestFailedException] must be thrownBy wasHtml(jsonResult)
    }

  }

  "content as HTML" should {

    "return the result body as a jsoup element" in {
      contentAsHtml(htmlResult).`val`() must be(Jsoup.parseBodyFragment(someHtml).body().`val`())
    }

  }

  "includes HTML input" should {

    "match result with text input specified by name" in {
      includesHtmlInput(htmlResult, "foo")
    }

    "match result with input specified by name and type" in {
      includesHtmlInput(htmlResult, "bar", "radio")
    }

    "match result with input specified by name and value" in {
      includesHtmlInputWithValue(htmlResult, "foo", "bar")
    }

    "match result with input specified by name and value and type" in {
      includesHtmlInputWithValue(htmlResult, "bar", "baz", "radio")
    }

    "not match result without specified input" in {
      a[TestFailedException] must be thrownBy includesHtmlInput(htmlResult, "bar")
    }

  }

  "includes HTML link" should {

    "match result that contains link with given href" in {
      includesHtmlLink(htmlResult, link)
    }

    "not match result that does not contain link with given href" in {
      a[TestFailedException] must be thrownBy includesHtmlLink(htmlResult, "https://facebook.com")
    }

  }

  "includes form" should {

    "match result that contains GET form with given action" in {
      includeForm(htmlResult, action)
    }

    "match result that contains form with given action and method" in {
      includeForm(htmlResult, action, HttpVerbs.POST)
    }

    "not match result that contains form with non-matching method" in {
      a[TestFailedException] must be thrownBy includeForm(htmlResult, action, HttpVerbs.PUT)
    }

    "not match result that contains form with non-matching action" in {
      a[TestFailedException] must be thrownBy includeForm(htmlResult, Call("POST", "/bar"), HttpVerbs.POST)
    }

  }

  "includes HTML tag" should {

    "match result that contains tag by text content" in {
      includeHtmlTag(htmlResult, "p", "Click me")
    }

    "not match result that does not contain tag with text content" in {
      a[TestFailedException] must be thrownBy includeHtmlTag(htmlResult, "p", "This is not in the result")
    }

    "match result that contains tag with text content and attribute value" in {
      includeHtmlTagWithAttribute(htmlResult, "p", "class", className)
    }

    "not match result that contains tag with text content but not attribute of given name" in {
      a[TestFailedException] must be thrownBy includeHtmlTagWithAttribute(htmlResult, "p", "id", className)
    }

    "not match result that contains tag with text content but not given attribute value" in {
      a[TestFailedException] must be thrownBy includeHtmlTagWithAttribute(htmlResult, "p", "class", "not-the-class-name")
    }

  }

}
