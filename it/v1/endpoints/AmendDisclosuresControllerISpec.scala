/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.domain.DesTaxYear
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class AmendDisclosuresControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String = "AA123456A"
    val taxYear: String = "2021-22"
    val correlationId: String = "X-123"

    val requestBodyJson: JsValue = Json.parse(
      """
        |{
        |  "taxAvoidance": [
        |    {
        |      "srn": "14211123",
        |      "taxYear": "2020-21"
        |    },
        |    {
        |      "srn": "34522678",
        |      "taxYear": "2021-22"
        |    }
        |  ],
        |  "class2Nics": {
        |     "class2VoluntaryContributions": true
        | }
        |}
    """.stripMargin
    )

    val hateoasResponse: JsValue = Json.parse(
      s"""
         |{
         |   "links":[
         |      {
         |         "href":"/individuals/disclosures/$nino/$taxYear",
         |         "rel":"amend-disclosures",
         |         "method":"PUT"
         |      },
         |      {
         |         "href":"/individuals/disclosures/$nino/$taxYear",
         |         "rel":"self",
         |         "method":"GET"
         |      },
         |      {
         |         "href":"/individuals/disclosures/$nino/$taxYear",
         |         "rel":"delete-disclosures",
         |         "method":"DELETE"
         |      }
         |   ]
         |}
    """.stripMargin
    )

    def uri: String = s"/$nino/$taxYear"

    def desUri: String = s"/disc-placeholder/disclosures/$nino/${DesTaxYear.fromMtd(taxYear)}"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }
  }

  "Calling the 'amend disclosures' endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.PUT, desUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
        response.body[JsValue] shouldBe hateoasResponse
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return a 400 with multiple errors" when {
      "all field value validations fail on the request body" in new Test {

        val allInvalidValueRequestBodyJson: JsValue = Json.parse(
          """
            |{
            |  "taxAvoidance": [
            |    {
            |      "srn": "ABC142111235D",
            |      "taxYear": "2020"
            |    },
            |    {
            |      "srn": "CDE345226789F",
            |      "taxYear": "2020-22"
            |    }
            |  ],
            |    "class2Nics": {
            |     "class2VoluntaryContributions": true
            | }
            |}
            |""".stripMargin
        )

        val allInvalidValueRequestError: List[MtdError] = List(
          RuleTaxYearRangeInvalidError.copy(
            paths = Some(List(
              "/taxAvoidance/1/taxYear"
            ))
          ),
          TaxYearFormatError.copy(
            paths = Some(List(
              "/taxAvoidance/0/taxYear"
            ))
          ),
          SRNFormatError.copy(
            paths = Some(List(
              "/taxAvoidance/0/srn",
              "/taxAvoidance/1/srn"
            ))
          )
        )

        val wrappedErrors: ErrorWrapper = ErrorWrapper(
          correlationId = Some(correlationId),
          error = BadRequestError,
          errors = Some(allInvalidValueRequestError)
        )

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().put(allInvalidValueRequestBodyJson))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.toJson(wrappedErrors)
      }

      "complex error scenario" in new Test {

        val disclosuresAmendErrorsRequest: JsValue = Json.parse(
          """
            |{
            |  "taxAvoidance": [
            |    {
            |      "srn": "ABC142111235D",
            |      "taxYear": "2020"
            |    },
            |    {
            |      "srn": "CDE345226789F",
            |      "taxYear": "2020-22"
            |    }
            |  ],
            |    "class2Nics": {
            |     "class2VoluntaryContributions": true
            | }
            |}
            |""".stripMargin
        )

        val disclosuresAmendErrorsResponse: JsValue = Json.parse(
          """
            {
            |    "code": "INVALID_REQUEST",
            |    "errors": [
            |     {
            |            "code": "RULE_TAX_YEAR_RANGE_INVALID",
            |            "message": "Tax year range invalid. A tax year range of one year is required",
            |            "paths": [
            |                "/taxAvoidance/1/taxYear"
            |            ]
            |        },
            |        {
            |            "code": "FORMAT_TAX_YEAR",
            |            "message": "The provided tax year is invalid",
            |            "paths": [
            |                "/taxAvoidance/0/taxYear"
            |            ]
            |        },
            |        {
            |            "code": "FORMAT_SRN_INVALID",
            |            "message": "The provided scheme reference number is invalid",
            |            "paths": [
            |                "/taxAvoidance/0/srn",
            |                "/taxAvoidance/1/srn"
            |            ]
            |        }
            |    ],
            |    "message": "Invalid request"
            |}
            |""".stripMargin
        )

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().put(disclosuresAmendErrorsRequest))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe disclosuresAmendErrorsResponse
      }
    }

    "return error according to spec" when {

      val validRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "taxAvoidance": [
          |    {
          |      "srn": "14211123",
          |      "taxYear": "2020-21"
          |    },
          |    {
          |      "srn": "34522678",
          |      "taxYear": "2021-22"
          |    }
          |  ],
          |    "class2Nics": {
          |     "class2VoluntaryContributions": true
          | }
          |}
         """.stripMargin
      )

      val nonsenseRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "field": "value"
          |}
        """.stripMargin
      )

      val invalidSRNRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "taxAvoidance": [
          |    {
          |      "srn": "ABC142111235",
          |      "taxYear": "2020-21"
          |    }
          |  ],
          |    "class2Nics": {
          |     "class2VoluntaryContributions": true
          | }
          |}
        """.stripMargin
      )

      val invalidSRNRequestMissingFieldBodyJson: JsValue = Json.parse(
        """
          |{
          |  "taxAvoidance": [
          |    {
          |      "taxYear": "2020-21"
          |    }
          |  ],
          |    "class2Nics": {
          |     "class2VoluntaryContributions": true
          | }
          |}
    """.stripMargin
      )

      val invalidSRNFormatRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "taxAvoidance": [
          |    {
          |      "taxYear": "2020-21",
          |      "srn": true
          |    }
          |  ],
          |    "class2Nics": {
          |     "class2VoluntaryContributions": true
          | }
          |}
    """.stripMargin
      )

      val emptyBodyJson: JsValue = Json.parse(
        """
          |{
          |}
    """.stripMargin
      )

      val srnFormatError: MtdError = SRNFormatError.copy(
        paths = Some(Seq(
          "/taxAvoidance/0/srn"
        ))
      )

      val incorrectBodyError: MtdError = RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq(
        "/taxAvoidance/0/srn"
      ))
      )

      "validation error" when {
        def validationErrorTest(requestNino: String, requestTaxYear: String, requestBody: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation ${requestNino} fails with ${expectedBody.code} error" in new Test {

            override val nino: String = requestNino
            override val taxYear: String = requestTaxYear
            override val requestBodyJson: JsValue = requestBody

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          ("AA1123A", "2021-22", validRequestBodyJson, BAD_REQUEST, NinoFormatError),
          ("AA123456A", "20177", validRequestBodyJson,  BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2015-17", validRequestBodyJson, BAD_REQUEST, RuleTaxYearRangeInvalidError),
          ("AA123456A", "2015-16", validRequestBodyJson, BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2021-22", nonsenseRequestBodyJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123457A", "2021-22", invalidSRNRequestMissingFieldBodyJson, BAD_REQUEST, incorrectBodyError),
          ("AA123458A", "2021-22", emptyBodyJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123459A", "2021-22", invalidSRNFormatRequestBodyJson, BAD_REQUEST, incorrectBodyError),
          ("AA123456A", "2021-22", invalidSRNRequestBodyJson, BAD_REQUEST, srnFormatError))

        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "des service error" when {
        def serviceErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"des returns an $desCode error and status $desStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DesStub.onError(DesStub.PUT, desUri, desStatus, errorBody(desCode))
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        def errorBody(code: String): String =
          s"""
             |{
             |   "code": "$code",
             |   "reason": "des message"
             |}
            """.stripMargin

        val input = Seq(
          (BAD_REQUEST, "INVALID_NINO", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "NOT_FOUND", NOT_FOUND, NotFoundError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, DownstreamError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, DownstreamError))

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}