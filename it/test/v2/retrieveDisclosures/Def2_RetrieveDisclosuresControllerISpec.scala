/*
 * Copyright 2026 HM Revenue & Customs
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

package v2.retrieveDisclosures

import api.models.errors.*
import api.services.{AuthStub, DownstreamStub, MtdIdLookupStub}
import api.support.IntegrationBaseSpec
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*
import v2.retrieveDisclosures.def2.fixture.Def2_RetrieveDisclosuresFixture.*

class Def2_RetrieveDisclosuresControllerISpec extends IntegrationBaseSpec {

  private trait Test {
    val nino: String    = "AA123456A"
    val taxYear: String = "2026-27"

    val downstreamUrl: String = s"/itsd/disclosures/$nino/$taxYear"

    def setupStubs(): Unit = ()

    def request: WSRequest = {
      AuthStub.authorised()
      MtdIdLookupStub.ninoFound(nino)
      setupStubs()
      buildRequest(s"/$nino/$taxYear")
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.2.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

    def errorBody(code: String): String =
      s"""
        |{
        |  "origin": "HIP",
        |  "response":  [
        |    {
        |      "errorCode": "$code",
        |      "errorDescription": "downstream message"
        |    }
        |  ]
        |}
      """.stripMargin

  }

  "Calling the 'Retrieve Disclosures' endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {
        override def setupStubs(): Unit = DownstreamStub.onSuccess(
          method = DownstreamStub.GET,
          uri = downstreamUrl,
          status = OK,
          body = downstreamResponseBodyJson
        )

        val response: WSResponse = await(request.get())

        response.status shouldBe OK
        response.json shouldBe mtdResponseBodyJson
        response.header("Content-Type") shouldBe Some("application/json")
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {
      def validationErrorTest(requestNino: String, requestTaxYear: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {
          override val nino: String    = requestNino
          override val taxYear: String = requestTaxYear

          val response: WSResponse = await(request.get())

          response.status shouldBe expectedStatus
          response.json shouldBe expectedBody.asJson
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val validationInput: Seq[(String, String, Int, MtdError)] = List(
        ("AA1123A", "2026-27", BAD_REQUEST, NinoFormatError),
        ("AA123456A", "2026", BAD_REQUEST, TaxYearFormatError),
        ("AA123456A", "2018-19", BAD_REQUEST, RuleTaxYearNotSupportedError),
        ("AA123456A", "2026-28", BAD_REQUEST, RuleTaxYearRangeInvalidError)
      )

      validationInput.foreach(validationErrorTest.tupled)

      def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns a code $downstreamCode error and status $downstreamStatus" in new Test {
          override def setupStubs(): Unit = DownstreamStub.onError(
            method = DownstreamStub.GET,
            uri = downstreamUrl,
            errorStatus = downstreamStatus,
            errorBody = errorBody(downstreamCode)
          )

          val response: WSResponse = await(request.get())

          response.status shouldBe expectedStatus
          response.json shouldBe expectedBody.asJson
          response.header("Content-Type") shouldBe Some("application/json")
          response.header("X-CorrelationId").nonEmpty shouldBe true
        }
      }

      val serviceInput: Seq[(Int, String, Int, MtdError)] = List(
        (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
        (BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError),
        (NOT_FOUND, "5010", NOT_FOUND, NotFoundError)
      )

      serviceInput.foreach(serviceErrorTest.tupled)
    }
  }

}
