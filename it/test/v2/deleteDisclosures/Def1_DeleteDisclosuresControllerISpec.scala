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

package v2.deleteDisclosures

import api.models.errors.*
import api.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import api.support.IntegrationBaseSpec
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*

class Def1_DeleteDisclosuresControllerISpec extends IntegrationBaseSpec {

  private trait Test {
    val nino: String    = "AA123456A"
    val taxYear: String = "2026-27"

    val downstreamUrl: String = s"/itsd/disclosures/$nino/$taxYear"

    def setupStubs(): Unit = ()

    def request: WSRequest = {
      AuditStub.audit()
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

  "Calling the 'Delete Disclosures' endpoint" should {
    "return a 204 status code" when {
      "any valid request is made" in new Test {
        override def setupStubs(): Unit = DownstreamStub.onSuccess(
          method = DownstreamStub.DELETE,
          uri = downstreamUrl,
          status = NO_CONTENT
        )

        val response: WSResponse = await(request.delete())

        response.status shouldBe NO_CONTENT
        response.body shouldBe ""
        response.header("Content-Type") shouldBe None
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {
      def validationErrorTest(requestNino: String, requestTaxYear: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {
          override val nino: String    = requestNino
          override val taxYear: String = requestTaxYear

          val response: WSResponse = await(request.delete())

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
            method = DownstreamStub.DELETE,
            uri = downstreamUrl,
            errorStatus = downstreamStatus,
            errorBody = errorBody(downstreamCode)
          )

          val response: WSResponse = await(request.delete())

          response.status shouldBe expectedStatus
          response.json shouldBe expectedBody.asJson
          response.header("Content-Type") shouldBe Some("application/json")
          response.header("X-CorrelationId").nonEmpty shouldBe true
        }
      }

      val serviceInput: Seq[(Int, String, Int, MtdError)] = List(
        (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
        (BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError),
        (NOT_FOUND, "5010", NOT_FOUND, NotFoundError),
        (UNPROCESSABLE_ENTITY, "4200", BAD_REQUEST, RuleOutsideAmendmentWindowError),
        (UNPROCESSABLE_ENTITY, "5000", BAD_REQUEST, RuleTaxYearNotSupportedError)
      )

      serviceInput.foreach(serviceErrorTest.tupled)
    }
  }

}
