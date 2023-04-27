/*
 * Copyright 2023 HM Revenue & Customs
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

package v1.controllers

import api.controllers.{ ControllerBaseSpec, ControllerTestRunner }
import api.mocks.MockIdGenerator
import api.mocks.hateoas.MockHateoasFactory
import api.mocks.services.{ MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService }
import api.models.audit.{ AuditEvent, AuditResponse, GenericAuditDetail }
import api.models.domain.Nino
import api.models.errors
import api.models.errors._
import api.models.hateoas.HateoasWrapper
import api.models.outcomes.ResponseWrapper
import mocks.MockAppConfig
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, Result }
import v1.mocks.requestParsers.MockAmendDisclosuresRequestParser
import v1.mocks.services.MockAmendDisclosuresService
import v1.models.request.amend._
import v1.models.response.amendDisclosures.AmendDisclosuresHateoasData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendDisclosuresControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAppConfig
    with MockAmendDisclosuresService
    with MockHateoasFactory
    with MockAmendDisclosuresRequestParser
    with MockAuditService
    with MockIdGenerator {

  val taxYear: String = "2021-22"

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

  val rawData: AmendDisclosuresRawData = AmendDisclosuresRawData(
    nino = nino,
    taxYear = taxYear,
    body = AnyContentAsJson(requestBodyJson)
  )

  val taxAvoidanceModel: Seq[AmendTaxAvoidanceItem] = Seq(
    AmendTaxAvoidanceItem(
      srn = "14211123",
      taxYear = "2020-21"
    ),
    AmendTaxAvoidanceItem(
      srn = "34522678",
      taxYear = "2021-22"
    )
  )

  val class2NicsModel: AmendClass2Nics = AmendClass2Nics(class2VoluntaryContributions = Some(true))

  val amendDisclosuresRequestBody: AmendDisclosuresRequestBody = AmendDisclosuresRequestBody(
    taxAvoidance = Some(taxAvoidanceModel),
    class2Nics = Some(class2NicsModel)
  )

  val requestData: AmendDisclosuresRequest = AmendDisclosuresRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    body = amendDisclosuresRequestBody
  )

  val hateoasResponse: JsValue = Json.parse(
    s"""
      |{
      |   "links": [
      |      {
      |         "href": "/baseUrl/$nino/$taxYear",
      |         "rel": "create-and-amend-disclosures",
      |         "method": "PUT"
      |      },
      |      {
      |         "href": "/baseUrl/$nino/$taxYear",
      |         "rel": "self",
      |         "method": "GET"
      |      },
      |      {
      |         "href": "/baseUrl/$nino/$taxYear",
      |         "rel": "delete-disclosures",
      |         "method": "DELETE"
      |      }
      |   ]
      |}
    """.stripMargin
  )

  trait Test extends ControllerTest with AuditEventChecking {

    val controller = new AmendDisclosuresController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      appConfig = mockAppConfig,
      parser = mockAmendDisclosuresRequestParser,
      service = mockAmendDisclosuresService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator,
      hateoasFactory = mockHateoasFactory,
    )

    override protected def callController(): Future[Result] = controller.amendDisclosures(nino, taxYear)(fakePostRequest(requestBodyJson))

    override protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateAmendDisclosures",
        transactionName = "create-amend-disclosures",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          request = Some(requestBodyJson),
          `X-CorrelationId` = correlationId,
          response = auditResponse
        )
      )

  }

  "AmendDisclosuresController" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockAmendDisclosuresRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockAmendDisclosuresService
          .amendDisclosures(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        MockHateoasFactory
          .wrap((), AmendDisclosuresHateoasData(nino, taxYear))
          .returns(HateoasWrapper((), hateoaslinks))

        runOkTestWithAudit(
          expectedStatus = OK,
          maybeExpectedResponseBody = Some(hateoaslinksJson),
          maybeAuditRequestBody = Some(requestBodyJson),
          maybeAuditResponseBody = Some(hateoaslinksJson)
        )
      }
    }

    "return the error as per spec" when {
      "parser validation fails" in new Test {

        MockAmendDisclosuresRequestParser
          .parse(rawData)
          .returns(Left(errors.ErrorWrapper(correlationId, NinoFormatError)))

        runErrorTestWithAudit(NinoFormatError, Some(requestBodyJson))
      }

      "service errors occur" in new Test {

        MockAmendDisclosuresRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockAmendDisclosuresService
          .amendDisclosures(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTestWithAudit(RuleTaxYearNotSupportedError, maybeAuditRequestBody = Some(requestBodyJson))
      }
    }
  }
}
