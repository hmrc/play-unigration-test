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

package uk.gov.hmrc.test.unigration

import com.google.inject.ConfigurationException
import org.scalatest.{MustMatchers, WordSpec}
import play.api.inject.bind
import play.api.http.HttpErrorHandler
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits

class UnigrationBaseSpec extends WordSpec with MustMatchers {

  class Scenario(val base: UnigrationBase = new TestUnigrationBase() {}) {

  }

  "the base spec" should {

    "have app materializer" in new Scenario {
      base.mat must be(base.app.materializer)
    }

    "have default execution context" in new Scenario {
      base.ec must be(Implicits.defaultContext)
    }

    "have messages API" in new Scenario {
      base.messages must be(base.app.injector.instanceOf[MessagesApi])
    }

  }

  "component" should {

    "return injectable instance of given type" in new Scenario {
      base.component[HttpErrorHandler] must be(base.app.injector.instanceOf[HttpErrorHandler])
    }

  }

  "customise" should {

    "include component A" in new Scenario {
      base.app.injector.instanceOf[ComponentA] must be(ComponentA())
      a[ConfigurationException] must be thrownBy base.app.injector.instanceOf[ComponentB]
      a[ConfigurationException] must be thrownBy base.app.injector.instanceOf[ComponentC]
    }

    "include components A and C" in new Scenario(base = new MixedInTestUnigrationBase {}) {
      base.app.injector.instanceOf[ComponentA] must be(ComponentA())
      base.app.injector.instanceOf[ComponentC] must be(ComponentC())
      a[ConfigurationException] must be thrownBy base.app.injector.instanceOf[ComponentB]
    }

    "include components A, B, and C" in new Scenario(base = new ExtendedTestUnigrationBase {}) {
      base.app.injector.instanceOf[ComponentA] must be(ComponentA())
      base.app.injector.instanceOf[ComponentB] must be(ComponentB())
      base.app.injector.instanceOf[ComponentC] must be(ComponentC())
    }

  }

}

// so long as we always call super.customise, we can mix and match multiple customisations to Guice binding

// has component A
trait TestUnigrationBase extends UnigrationBase {
  override protected def customise(builder: GuiceApplicationBuilder): GuiceApplicationBuilder =
    super.customise(builder).overrides(bind[ComponentA].to(ComponentA()))
}

// has components A and C
trait MixedInTestUnigrationBase extends TestUnigrationBase {
  override protected def customise(builder: GuiceApplicationBuilder): GuiceApplicationBuilder =
    super.customise(builder).overrides(bind[ComponentC].to(ComponentC()))
}

// has components A, B, and C
trait ExtendedTestUnigrationBase extends TestUnigrationBase with MixedInTestUnigrationBase {
  override protected def customise(builder: GuiceApplicationBuilder): GuiceApplicationBuilder =
    super.customise(builder).overrides(bind[ComponentB].to(ComponentB()))
}

case class ComponentA(name: String = "Component A")

case class ComponentB(name: String = "Component B")

case class ComponentC(name: String = "Component C")
