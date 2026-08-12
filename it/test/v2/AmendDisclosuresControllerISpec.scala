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

package v2

import api.models.errors
import api.models.errors.*
import api.services.*
import api.support.IntegrationBaseSpec
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*

class AmendDisclosuresControllerISpec extends IntegrationBaseSpec {

  private trait Test {
    val nino: String          = "AA123456A"
    val taxYear: String       = "2021-22"
    val correlationId: String = "X-123"

    val requestBodyJson: JsValue = Json.parse(
      """
        |{
        |   "taxAvoidance": [
        |      {
        |         "srn": "14211123",
        |         "taxYear": "2020-21"
        |      },
        |      {
        |         "srn": "34522678",
        |         "taxYear": "2021-22"
        |      }
        |   ],
        |   "class2Nics": {
        |      "class2VoluntaryContributions": true
        |   }
        |}
      """.stripMargin
    )

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

  }

  "Calling the 'Create and Amend Disclosures' endpoint" should {
    "return a 204 status code" when {
      "any valid request is made" in new Test {
        override def setupStubs(): Unit = DownstreamStub.onSuccess(
          method = DownstreamStub.PUT,
          uri = downstreamUri,
          status = NO_CONTENT
        )

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe NO_CONTENT
        response.body shouldBe ""
        response.header("Content-Type") shouldBe None
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return a TaxYearFormatError with 400 (BAD_REQUEST) status code" when {
      "any invalid tax year format body request is made" in new Test {
        val invalidTaxYearRequestBodyJson: JsValue = Json.parse(
          """
            |{
            |   "taxAvoidance": [
            |      {
            |         "srn": "14211123",
            |         "taxYear": "2020-222"
            |      }
            |   ],
            |   "class2Nics": {
            |      "class2VoluntaryContributions": true
            |   }
            |}
          """.stripMargin
        )

        val response: WSResponse = await(request().put(invalidTaxYearRequestBodyJson))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.toJson(
          ErrorWrapper(
            correlationId = correlationId,
            error = TaxYearFormatError.withPath("/taxAvoidance/0/taxYear")
          )
        )
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return a RuleTaxYearRangeInvalidError with 400 (BAD_REQUEST) status code" when {
      "any invalid tax year range body request is made" in new Test {
        val invalidTaxYearRequestBodyJson: JsValue = Json.parse(
          """
            |{
            |   "taxAvoidance": [
            |      {
            |         "srn": "14211123",
            |         "taxYear": "2020-22"
            |      }
            |   ],
            |   "class2Nics": {
            |      "class2VoluntaryContributions": true
            |   }
            |}
          """.stripMargin
        )

        val response: WSResponse = await(request().put(invalidTaxYearRequestBodyJson))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.toJson(
          ErrorWrapper(
            correlationId = correlationId,
            error = RuleTaxYearRangeInvalidError.withPath("/taxAvoidance/0/taxYear")
          )
        )
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return a 400 with multiple errors" when {
      "all field value validations fail on the request body" in new Test {
        val allInvalidValueRequestBodyJson: JsValue = Json.parse(
          """
            |{
            |   "taxAvoidance": [
            |      {
            |         "srn": "ABC142111235D",
            |         "taxYear": "2020"
            |      },
            |      {
            |         "srn": "CDE345226789F",
            |         "taxYear": "2020-22"
            |      }
            |   ],
            |   "class2Nics": {
            |      "class2VoluntaryContributions": false
            |   }
            |}
          """.stripMargin
        )

        val allInvalidValueRequestError: List[MtdError] = List(
          TaxYearFormatError.withPath("/taxAvoidance/0/taxYear"),
          SRNFormatError.withPaths(List("/taxAvoidance/0/srn", "/taxAvoidance/1/srn")),
          RuleTaxYearRangeInvalidError.withPath("/taxAvoidance/1/taxYear"),
          RuleVoluntaryClass2ValueInvalidError.withPath("/class2Nics/class2VoluntaryContributions")
        )

        val wrappedErrors: ErrorWrapper = ErrorWrapper(
          correlationId = correlationId,
          error = BadRequestError,
          errors = Some(allInvalidValueRequestError)
        )

        val response: WSResponse = await(request().put(allInvalidValueRequestBodyJson))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.toJson(wrappedErrors)
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return error according to spec" when {
      val validRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |   "taxAvoidance": [
          |      {
          |         "srn": "14211123",
          |         "taxYear": "2020-21"
          |      },
          |      {
          |         "srn": "34522678",
          |         "taxYear": "2021-22"
          |      }
          |   ],
          |   "class2Nics": {
          |      "class2VoluntaryContributions": true
          |   }
          |}
        """.stripMargin
      )

      val nonsenseRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |   "field": "value"
          |}
        """.stripMargin
      )

      val emptyBodyJson: JsValue = Json.parse(
        """
          |{
          |}
        """.stripMargin
      )

      val invalidSRNRequestMissingFieldBodyJson: JsValue = Json.parse(
        """
          |{
          |   "taxAvoidance": [
          |      {
          |         "taxYear": "2020-21"
          |      }
          |   ],
          |   "class2Nics": {
          |      "class2VoluntaryContributions": true
          |   }
          |}
        """.stripMargin
      )

      val invalidSRNFormatRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |   "taxAvoidance": [
          |      {
          |         "srn": true,
          |         "taxYear": "2020-21"
          |      }
          |   ],
          |   "class2Nics": {
          |      "class2VoluntaryContributions": true
          |   }
          |}
        """.stripMargin
      )

      val incorrectBodyError: MtdError = RuleIncorrectOrEmptyBodyError.withPath("/taxAvoidance/0/srn")

      val invalidSRNRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |   "taxAvoidance": [
          |      {
          |         "srn": "ABC142111235",
          |         "taxYear": "2020-21"
          |      }
          |   ],
          |   "class2Nics": {
          |      "class2VoluntaryContributions": true
          |   }
          |}
        """.stripMargin
      )

      val srnFormatError: MtdError = SRNFormatError.withPath("/taxAvoidance/0/srn")

      val invalidClass2ValueRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |   "taxAvoidance": [
          |      {
          |         "srn": "14211123",
          |         "taxYear": "2020-21"
          |      }
          |   ],
          |   "class2Nics": {
          |      "class2VoluntaryContributions": false
          |   }
          |}
        """.stripMargin
      )

      val ruleVoluntaryClass2ValueInvalidError: MtdError = RuleVoluntaryClass2ValueInvalidError.withPath(
        "/class2Nics/class2VoluntaryContributions"
      )

      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: MtdError): Unit = {
          s"validation $requestNino fails with ${expectedBody.code} error" in new Test {
            override val nino: String             = requestNino
            override val taxYear: String          = requestTaxYear
            override val requestBodyJson: JsValue = requestBody

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe expectedBody.asJson
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input: Seq[(String, String, JsValue, Int, MtdError)] = Seq(
          ("AA1123A", "2021-22", validRequestBodyJson, BAD_REQUEST, NinoFormatError),
          ("AA123456A", "20177", validRequestBodyJson, BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2015-17", validRequestBodyJson, BAD_REQUEST, RuleTaxYearRangeInvalidError),
          ("AA123456A", "2015-16", validRequestBodyJson, BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2021-22", nonsenseRequestBodyJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123458A", "2021-22", emptyBodyJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123457A", "2021-22", invalidSRNRequestMissingFieldBodyJson, BAD_REQUEST, incorrectBodyError),
          ("AA123459A", "2021-22", invalidSRNFormatRequestBodyJson, BAD_REQUEST, incorrectBodyError),
          ("AA123456A", "2021-22", invalidSRNRequestBodyJson, BAD_REQUEST, srnFormatError),
          ("AA123456A", "2021-22", invalidClass2ValueRequestBodyJson, BAD_REQUEST, ruleVoluntaryClass2ValueInvalidError)
        )

        input.foreach(validationErrorTest.tupled)
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns a code $downstreamCode error and status $downstreamStatus" in new Test {
            override def setupStubs(): Unit = DownstreamStub.onError(
              method = DownstreamStub.PUT,
              uri = downstreamUri,
              errorStatus = downstreamStatus,
              errorBody = errorBody(downstreamCode)
            )

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe expectedBody.asJson
            response.header("X-CorrelationId").nonEmpty shouldBe true
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        def errorBody(code: String): String =
          s"""
            |[
            |  {
            |    "errorCode": "$code",
            |    "errorDescription": "downstream message"
            |  }
            |]
          """.stripMargin

        val input: Seq[(Int, String, Int, MtdError)] = Seq(
          (BAD_REQUEST, "1000", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, InternalError),
          (UNPROCESSABLE_ENTITY, "5003", NOT_FOUND, NotFoundError.forSelfEmployment),
          (UNPROCESSABLE_ENTITY, "5004", BAD_REQUEST, RuleVoluntaryClass2CannotBeChangedError),
          (UNPROCESSABLE_ENTITY, "4200", BAD_REQUEST, RuleOutsideAmendmentWindowError),
          (NOT_IMPLEMENTED, "5000", BAD_REQUEST, RuleTaxYearNotSupportedError)
        )

        input.foreach(serviceErrorTest.tupled)
      }
    }
  }

}
