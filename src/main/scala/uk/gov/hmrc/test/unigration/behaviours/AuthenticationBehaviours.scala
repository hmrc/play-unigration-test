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

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

import org.mockito.Mockito.when
import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.filters.csrf.CSRF.Token
import play.filters.csrf.{CSRFConfig, CSRFConfigProvider, CSRFFilter}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, NoActiveSession}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.test.unigration.{UnigrationBase, UnigrationFixtures, UserFixture}

import scala.concurrent.Future

trait AuthenticationBehaviours extends UnigrationBase with UnigrationFixtures with MockitoSugar with AppName {

  lazy val randomUser: UserFixture = userFixture()

  lazy val notLoggedInException: NoActiveSession = new NoActiveSession("A girl has no name") {}

  lazy val authConnector: AuthConnector = mock[AuthConnector]

  private [behaviours] lazy val cfg: CSRFConfig = component[CSRFConfigProvider].get

  private [behaviours] lazy val token: String = component[CSRFFilter].tokenProvider.generateToken

  override def configuration: Configuration = app.configuration

  def ggLoginRedirectUri(fromUri: String): String = s"/gg/sign-in?continue=${URLEncoder.encode(fromUri, StandardCharsets.UTF_8.displayName())}&origin=${appName}"

  lazy val authenticationTags: Map[String, String] = Map(
    Token.NameRequestTag -> cfg.tokenName,
    Token.RequestTag -> token
  )

  //noinspection ConvertExpressionToSAM
  private val noBearerTokenMatcher: ArgumentMatcher[HeaderCarrier] = new ArgumentMatcher[HeaderCarrier] {
    override def matches(hc: HeaderCarrier): Boolean = hc != null && hc.authorization.isEmpty
  }

  //noinspection ConvertExpressionToSAM
  private def enrolmentsMatcher(user: UserFixture): ArgumentMatcher[Predicate] = new ArgumentMatcher[Predicate] {
    override def matches(p: Predicate): Boolean = user.enrolments.enrolments.map(enrolment => Enrolment(enrolment.key)).exists(_ == p)
  }

  def withSignedInUser(user: UserFixture = randomUser)(test: (Map[String, String], Map[String, String], Map[String, String]) => Unit): Unit = {
    when(
      authConnector
        .authorise(
          ArgumentMatchers.argThat(enrolmentsMatcher(user)),
          ArgumentMatchers.eq(credentials and name and email and affinityGroup and internalId and allEnrolments))(ArgumentMatchers.any(), ArgumentMatchers.any()
        )
    ).thenReturn(
      Future.successful(new ~(new ~(new ~(new ~(new ~(user.credentials, user.name), user.email), user.affinityGroup), user.internalId), user.enrolments))
    )
    test(Map(cfg.headerName -> token), userSession(user), authenticationTags)
  }

  def withoutSignedInUser()(test: => Unit): Unit = {
    when(
      authConnector
        .authorise(
          ArgumentMatchers.any(),
          ArgumentMatchers.any[Retrieval[_]])(ArgumentMatchers.argThat(noBearerTokenMatcher), ArgumentMatchers.any()
        )
    ).thenReturn(
      Future.failed(notLoggedInException)
    )
    test
  }

  def userSession(user: UserFixture): Map[String, String] = Map(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.userId -> user.internalId.getOrElse(randomString(8))
  )

  override protected def customise(builder: GuiceApplicationBuilder): GuiceApplicationBuilder =
    super.customise(builder).overrides(bind[AuthConnector].to(authConnector))

}
