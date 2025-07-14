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

package v1

import api.models.errors
import api.models.errors._
import api.services._
import api.support.IntegrationBaseSpec
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers._

class AmendDisclosuresControllerHipISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String          = "AA123456A"
    val taxYear: String       = "2021-22"
    private val downstreamTaxYear: String = "2021-22"
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

    val hateoasResponse: JsValue = Json.parse(
      s"""
         |{
         |   "links": [
         |      {
         |         "href": "/individuals/disclosures/$nino/$taxYear",
         |         "rel": "create-and-amend-disclosures",
         |         "method": "PUT"
         |      },
         |      {
         |         "href": "/individuals/disclosures/$nino/$taxYear",
         |         "rel": "self",
         |         "method": "GET"
         |      },
         |      {
         |         "href": "/individuals/disclosures/$nino/$taxYear",
         |         "rel": "delete-disclosures",
         |         "method": "DELETE"
         |      }
         |   ]
         |}
       """.stripMargin
    )

    private def uri: String = s"/$nino/$taxYear"

    def downstreamUri: String = s"/itsd/disclosures/$nino/$downstreamTaxYear"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.1.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

  }

  "Calling the 'amend disclosures' endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
        response.body[JsValue] shouldBe hateoasResponse
        response.header("Content-Type") shouldBe Some("application/json")
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

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().put(invalidTaxYearRequestBodyJson))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.toJson(
          ErrorWrapper(
            correlationId = correlationId,
            error = TaxYearFormatError.copy(
              paths = Some(List("/taxAvoidance/0/taxYear"))
            ),
            errors = None
          ))
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

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().put(invalidTaxYearRequestBodyJson))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.toJson(
          ErrorWrapper(
            correlationId = correlationId,
            error = RuleTaxYearRangeInvalidError.copy(
              paths = Some(List("/taxAvoidance/0/taxYear"))
            ),
            errors = None
          ))
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
          TaxYearFormatError.copy(
            paths = Some(
              List(
                "/taxAvoidance/0/taxYear"
              ))
          ),
          SRNFormatError.copy(
            paths = Some(
              List(
                "/taxAvoidance/0/srn",
                "/taxAvoidance/1/srn"
              ))
          ),
          RuleTaxYearRangeInvalidError.copy(
            paths = Some(
              List(
                "/taxAvoidance/1/taxYear"
              ))
          ),
          RuleVoluntaryClass2ValueInvalidError.copy(
            paths = Some(
              List(
                "/class2Nics/class2VoluntaryContributions"
              ))
          )
        )

        val wrappedErrors: ErrorWrapper = ErrorWrapper(
          correlationId = correlationId,
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

        val disclosuresAmendErrorsResponse: JsValue = Json.parse(
          """
            |{
            |   "code": "INVALID_REQUEST",
            |   "errors": [
            |      {
            |         "code": "FORMAT_TAX_YEAR",
            |         "message": "The provided tax year is invalid",
            |         "paths": [
            |            "/taxAvoidance/0/taxYear"
            |         ]
            |      },
            |      {
            |         "code": "FORMAT_SRN_INVALID",
            |         "message": "The provided scheme reference number is invalid",
            |         "paths": [
            |            "/taxAvoidance/0/srn",
            |            "/taxAvoidance/1/srn"
            |         ]
            |      },
            |      {
            |         "code": "RULE_TAX_YEAR_RANGE_INVALID",
            |         "message": "Tax year range invalid. A tax year range of one year is required",
            |         "paths": [
            |            "/taxAvoidance/1/taxYear"
            |         ]
            |      },
            |      {
            |         "code": "RULE_VOLUNTARY_CLASS2_VALUE_INVALID",
            |         "message": "Voluntary Class 2 Contributions can only be set to true",
            |         "paths": [
            |            "/class2Nics/class2VoluntaryContributions"
            |         ]
            |      }
            |   ],
            |   "message": "Invalid request"
            |}
          """.stripMargin
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

      val incorrectBodyError: MtdError = RuleIncorrectOrEmptyBodyError.copy(
        paths = Some(List("/taxAvoidance/0/srn"))
      )

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

      val srnFormatError: MtdError = SRNFormatError.copy(
        paths = Some(List("/taxAvoidance/0/srn"))
      )

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

      val ruleVoluntaryClass2ValueInvalidError: MtdError = RuleVoluntaryClass2ValueInvalidError.copy(
        paths = Some(List("/class2Nics/class2VoluntaryContributions"))
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
        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $downstreamStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.PUT, downstreamUri, downstreamStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
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

        val input = Seq(
          (BAD_REQUEST, "1000", INTERNAL_SERVER_ERROR, errors.InternalError),
          (BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, errors.InternalError),
          (UNPROCESSABLE_ENTITY, "5003", NOT_FOUND, NotFoundError),
          (UNPROCESSABLE_ENTITY, "5004", BAD_REQUEST, RuleVoluntaryClass2CannotBeChangedError),
          (NOT_IMPLEMENTED, "5000", BAD_REQUEST, RuleTaxYearNotSupportedError)
        )
        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }

}
