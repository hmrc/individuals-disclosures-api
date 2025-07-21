/*
 * Copyright 2025 HM Revenue & Customs
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

package api.connectors

import com.google.common.base.Charsets
import config.MockAppConfig
import mocks.MockHttpClient
import org.scalamock.handlers.CallHandler
import play.api.http.{HeaderNames, MimeTypes, Status}
import play.api.libs.json.{Json, Writes}
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import java.net.URL
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

trait ConnectorSpec extends UnitSpec with Status with MimeTypes with HeaderNames {

  lazy val baseUrl: String           = "http://test-BaseUrl"
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val otherHeaders: Seq[(String, String)] = List(
    "Gov-Test-Scenario" -> "DEFAULT",
    "AnotherHeader"     -> "HeaderValue"
  )

  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = otherHeaders)
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val dummyHeaderCarrierConfig: HeaderCarrier.Config =
    HeaderCarrier.Config(
      List("^not-test-BaseUrl?$".r),
      Nil,
      Some("individual-disclosures-api")
    )

  val allowedHeaders: Seq[String] = List(
    "Accept",
    "Gov-Test-Scenario",
    "Content-Type",
    "Location",
    "X-Request-Timestamp",
    "X-Session-Id"
  )

  protected trait ConnectorTest extends MockHttpClient with MockAppConfig {
    protected val baseUrl: String = "http://test-BaseUrl"

    implicit protected val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders)

    protected val requiredHeaders: Seq[(String, String)]

    protected def willGet[T](url: URL): CallHandler[Future[T]] = {
      MockedHttpClient
        .get(
          url = url,
          config = dummyHeaderCarrierConfig,
          requiredHeaders = requiredHeaders,
          excludedHeaders = List("AnotherHeader" -> "HeaderValue")
        )
    }

    protected def willPost[BODY, T](url: URL, body: BODY)(implicit writes: Writes[BODY]): CallHandler[Future[T]] = {
      MockedHttpClient
        .post(
          url = url,
          config = dummyHeaderCarrierConfig,
          body = Json.toJson(body),
          requiredHeaders = requiredHeaders ++ List("Content-Type" -> "application/json"),
          excludedHeaders = List("AnotherHeader" -> "HeaderValue")
        )
    }

    protected def willPut[BODY, T](url: URL, body: BODY)(implicit writes: Writes[BODY]): CallHandler[Future[T]] = {
      MockedHttpClient
        .put(
          url = url,
          config = dummyHeaderCarrierConfig,
          body = Json.toJson(body),
          requiredHeaders = requiredHeaders ++ List("Content-Type" -> "application/json"),
          excludedHeaders = List("AnotherHeader" -> "HeaderValue")
        )
    }

    protected def willDelete[T](url: URL): CallHandler[Future[T]] = {
      MockedHttpClient
        .delete(
          url = url,
          config = dummyHeaderCarrierConfig,
          requiredHeaders = requiredHeaders,
          excludedHeaders = List("AnotherHeader" -> "HeaderValue")
        )
    }

  }

  protected trait Ifs1Test extends ConnectorTest {

    protected lazy val requiredHeaders: Seq[(String, String)] = List(
      "Authorization"     -> "Bearer ifs1-token",
      "Environment"       -> "ifs1-environment",
      "User-Agent"        -> "individual-disclosures-api",
      "CorrelationId"     -> correlationId,
      "Gov-Test-Scenario" -> "DEFAULT"
    )
    MockedAppConfig.ifs1BaseUrl returns this.baseUrl
    MockedAppConfig.ifs1Token returns "ifs1-token"
    MockedAppConfig.ifs1Env returns "ifs1-environment"
    MockedAppConfig.ifs1EnvironmentHeaders returns Some(allowedHeaders)

  }

  protected trait Ifs2Test extends ConnectorTest {

    protected lazy val requiredHeaders: Seq[(String, String)] = List(
      "Authorization"     -> "Bearer ifs2-token",
      "Environment"       -> "ifs2-environment",
      "User-Agent"        -> "individual-disclosures-api",
      "CorrelationId"     -> correlationId,
      "Gov-Test-Scenario" -> "DEFAULT"
    )

    MockedAppConfig.ifs2BaseUrl returns this.baseUrl
    MockedAppConfig.ifs2Token returns "ifs2-token"
    MockedAppConfig.ifs2Env returns "ifs2-environment"
    MockedAppConfig.ifs2EnvironmentHeaders returns Some(allowedHeaders)

  }

  protected trait HipTest extends ConnectorTest {
    private val clientId: String = "clientId"
    private val clientSecret: String = "clientSecret"

    private val token: String = Base64.getEncoder.encodeToString(s"$clientId:$clientSecret".getBytes(Charsets.UTF_8))

    protected lazy val requiredHeaders: Seq[(String, String)] = List(
      "Authorization"     -> s"Basic $token",
      "Environment"       -> "hip-environment",
      "User-Agent"        -> "individual-disclosures-api",
      "CorrelationId"     -> correlationId,
      "Gov-Test-Scenario" -> "DEFAULT"
    )
    MockedAppConfig.hipBaseUrl returns this.baseUrl
    MockedAppConfig.hipEnv returns "hip-environment"
    MockedAppConfig.hipClientId returns clientId
    MockedAppConfig.hipClientSecret returns clientSecret
    MockedAppConfig.hipEnvironmentHeaders returns Some(allowedHeaders.filterNot(Set("Content-Type")))

  }

}
