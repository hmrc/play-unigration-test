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

import java.util.UUID

import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.emailaddress.EmailAddress

class UnigrationFixturesSpec extends WordSpec with MustMatchers {

  "random UUID" should {

    "return a valid Java UUID" in new UnigrationFixtures {
      val uuid = randomUUID
      UUID.fromString(uuid).toString must be(uuid)
    }

  }

  "random domain name" should {

    "return a random 8-character alphanumeric string with a random TLD" in new UnigrationFixtures {
      val domain = randomDomainName.split("\\.")
      domain.head must fullyMatch regex "^[a-zA-Z0-9]{8}$"
      tlds must contain("." + domain.tail.mkString("."))
    }

  }

  "random email address" should {

    "return a valid email address" in new UnigrationFixtures {
      EmailAddress.isValid(randomEmail) must be(true)
    }

    "return a valid email address given first and last name" in new UnigrationFixtures {
      EmailAddress.isValid(randomEmail(randomFirstName, randomLastName)) must be(true)
    }

  }

  "random first name" should {

    "return one of the most common English first names" in new UnigrationFixtures {
      firstNames must contain(randomFirstName)
    }

  }

  "random last name" should {

    "return one of the most common English last names" in new UnigrationFixtures {
      lastNames must contain(randomLastName)
    }

  }

  "random int" should {

    "return an integer between 0 and the given exclusive upper bound" in new UnigrationFixtures {
      val i = randomInt(42)
      i must be >= 0
      i must be < 42
    }

  }

  "random big decimal"  should {

    "return a random integer as a big decimal up to Int.MaxValue - 1" in new UnigrationFixtures {
      val i = randomBigDecimal.intValue()
      i must be >= 0
      i must be < Int.MaxValue
    }

    "return a random integer as a big decimal up to given exclusive upper bound" in new UnigrationFixtures {
      val i = randomBigDecimal(42).intValue()
      i must be >= 0
      i must be < 42
    }

  }

  "random0To9" should {

    "return an integer between 0 and 9 inclusive" in new UnigrationFixtures {
      val i = random0To9
      i must be >= 0
      i must be < 10
    }

  }

  "random string" should {

    "return a random alphanumeric string of the specified length" in new UnigrationFixtures {
      val str = randomString(8)
      str must fullyMatch regex "^[a-zA-Z0-9]{8}$"
      str.length must be(8)
    }

  }

  "random boolean" should {

    "return both true and false with acceptable frequency" in new UnigrationFixtures {
      (1 to 100).map(_ => randomBoolean).toSet.size must be(2) // i.e. both true and false have been generated at some point within 100 attempts
    }

  }

  "random currency code" should {

    "return one of the ISO 4217 currency codes" in new UnigrationFixtures {
      iso4217 must contain(randomISO4217CurrencyCode)
    }

  }

  "random country code" should {

    "return one of the ISO 3166 alpha-2 country codes" in new UnigrationFixtures {
      iso3166 must contain(randomISO3166Alpha2CountryCode)
    }

  }

}
