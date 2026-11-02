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

package v2.createAmendDisclosures

import api.models.errors
import api.models.errors.*
import api.services.*
import api.support.IntegrationBaseSpec
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*
import v2.createAmendDisclosures.def1.fixture.Def1_CreateAmendDisclosuresFixture.mtdRequestBodyJson

class Def2_CreateAmendDisclosuresControllerISpec extends IntegrationBaseSpec {

  private trait Test {
    val nino: String    = "AA123456A"
    val taxYear: String = "2026-27"

    val downstreamUri: String = s"/itsd/disclosures/$nino/$taxYear"

    def setupStubs(): Unit = ()

    def request(): WSRequest = {
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

  private val allInvalidValuesRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": "142111231",
      |      "taxYear": "2026-277"
      |    },
      |    {
      |      "srn": "",
      |      "taxYear": "2026-28"
      |    }
      |  ],
      |  "class2Nics": {
      |    "class2VoluntaryContributions": false
      |  }
      |}
      """.stripMargin
  )

  private val allInvalidValuesRequestErrors: List[MtdError] = List(
    TaxYearFormatError.withPath("/taxAvoidance/0/taxYear"),
    SRNFormatError.withPaths(List("/taxAvoidance/0/srn", "/taxAvoidance/1/srn")),
    RuleTaxYearRangeInvalidError.withPath("/taxAvoidance/1/taxYear"),
    RuleVoluntaryClass2ValueInvalidError.withPath("/class2Nics/class2VoluntaryContributions")
  )

  private val wrappedErrors: ErrorWrapper = ErrorWrapper(
    correlationId = "ignored",
    error = BadRequestError,
    errors = Some(allInvalidValuesRequestErrors)
  )

  "Calling the 'Create and Amend Disclosures' endpoint" should {
    "return a 204 status code" when {
      "any valid request is made" in new Test {
        override def setupStubs(): Unit = DownstreamStub.onSuccess(
          method = DownstreamStub.PUT,
          uri = downstreamUri,
          status = NO_CONTENT
        )

        val response: WSResponse = await(request().put(mtdRequestBodyJson))
        response.status shouldBe NO_CONTENT
        response.body shouldBe ""
        response.header("Content-Type") shouldBe None
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {

      def validationErrorTest(requestNino: String,
                              requestTaxYear: String,
                              requestBody: JsValue,
                              expectedStatus: Int,
                              expectedError: MtdError,
                              expectedErrors: Option[ErrorWrapper]): Unit = {
        s"validation fails with ${expectedError.code} error" in new Test {
          override val nino: String    = requestNino
          override val taxYear: String = requestTaxYear

          val response: WSResponse = await(request().put(requestBody))
          response.status shouldBe expectedStatus
          response.json shouldBe expectedErrors.fold(expectedError.asJson)(Json.toJson(_))
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val validationInput: Seq[(String, String, JsValue, Int, MtdError, Option[ErrorWrapper])] = Seq(
        ("AA1123A", "2026-27", mtdRequestBodyJson, BAD_REQUEST, NinoFormatError, None),
        ("AA123456A", "2026", mtdRequestBodyJson, BAD_REQUEST, TaxYearFormatError, None),
        ("AA123456A", "2026-28", mtdRequestBodyJson, BAD_REQUEST, RuleTaxYearRangeInvalidError, None),
        ("AA123456A", "2018-19", mtdRequestBodyJson, BAD_REQUEST, RuleTaxYearNotSupportedError, None),
        ("AA123456A", "2026-27", JsObject.empty, BAD_REQUEST, RuleIncorrectOrEmptyBodyError, None),
        ("AA123458A", "2026-27", allInvalidValuesRequestBodyJson, BAD_REQUEST, BadRequestError, Some(wrappedErrors))
      )

      validationInput.foreach(validationErrorTest.tupled)

      def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns a code $downstreamCode error and status $downstreamStatus" in new Test {
          override def setupStubs(): Unit = DownstreamStub.onError(
            method = DownstreamStub.PUT,
            uri = downstreamUri,
            errorStatus = downstreamStatus,
            errorBody = errorBody(downstreamCode)
          )

          val response: WSResponse = await(request().put(mtdRequestBodyJson))
          response.status shouldBe expectedStatus
          response.json shouldBe expectedBody.asJson
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val serviceInput: Seq[(Int, String, Int, MtdError)] = Seq(
        (BAD_REQUEST, "1000", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError),
        (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
        (BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, InternalError),
        (UNPROCESSABLE_ENTITY, "5003", NOT_FOUND, NotFoundError.forSelfEmployment),
        (UNPROCESSABLE_ENTITY, "5004", BAD_REQUEST, RuleVoluntaryClass2CannotBeChangedError),
        (UNPROCESSABLE_ENTITY, "4200", BAD_REQUEST, RuleOutsideAmendmentWindowError),
        (NOT_IMPLEMENTED, "5000", BAD_REQUEST, RuleTaxYearNotSupportedError)
      )

      serviceInput.foreach(serviceErrorTest.tupled)
    }
  }

}
