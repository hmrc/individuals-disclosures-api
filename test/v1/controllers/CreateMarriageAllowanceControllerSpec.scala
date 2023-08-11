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

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.mocks.MockIdGenerator
import api.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.Nino
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import mocks.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import v1.controllers.requestParsers.MockCreateMarriageAllowanceRequestParser
import v1.models.request.create.{CreateMarriageAllowanceBody, CreateMarriageAllowanceRawData, CreateMarriageAllowanceRequest}
import v1.services.MockCreateMarriageAllowanceService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateMarriageAllowanceControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAppConfig
    with MockCreateMarriageAllowanceService
    with MockCreateMarriageAllowanceRequestParser
    with MockAuditService
    with MockIdGenerator {

  trait Test extends ControllerTest with AuditEventChecking {

    val controller = new CreateMarriageAllowanceController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockCreateMarriageAllowanceRequestParser,
      service = mockCreateMarriageAllowanceService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.createMarriageAllowance(nino)(fakePostRequest(requestBodyJson))

    def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateMarriageAllowanceClaim",
        transactionName = "create-marriage-allowance-claim",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          params = Map("nino" -> nino),
          request = Some(requestBodyJson),
          `X-CorrelationId` = correlationId,
          response = auditResponse
        )
      )

  }

  val requestBodyJson: JsValue = Json.parse(
    s"""
      |{
      |  "spouseOrCivilPartnerNino": "BB123456B",
      |  "spouseOrCivilPartnerFirstName": "John",
      |  "spouseOrCivilPartnerSurname": "Smith",
      |  "spouseOrCivilPartnerDateOfBirth": "1986-04-06"
      |}
    """.stripMargin
  )

  val rawData: CreateMarriageAllowanceRawData = CreateMarriageAllowanceRawData(
    nino = nino,
    body = AnyContentAsJson(requestBodyJson)
  )

  val createMarriageAllowanceRequestBody: CreateMarriageAllowanceBody = CreateMarriageAllowanceBody(
    "BB123456B",
    Some("John"),
    "Smith",
    Some("1986-04-06")
  )

  val requestData: CreateMarriageAllowanceRequest = CreateMarriageAllowanceRequest(
    nino = Nino(nino),
    body = createMarriageAllowanceRequestBody
  )

  "CreateMarriageAllowanceController" should {
    "return a successful response with status 201 (OK)" when {
      "the request received is valid" in new Test {

        MockCreateMarriageAllowanceRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockCreateMarriageAllowanceService
          .createMarriageAllowance(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(
          expectedStatus = CREATED,
          maybeAuditRequestBody = Some(requestBodyJson)
        )
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {

        MockCreateMarriageAllowanceRequestParser
          .parse(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError)))

        runErrorTestWithAudit(NinoFormatError, Some(requestBodyJson))

      }

      "the service returns an error" in new Test {

        MockCreateMarriageAllowanceRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockCreateMarriageAllowanceService
          .createMarriageAllowance(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTestWithAudit(RuleTaxYearNotSupportedError, maybeAuditRequestBody = Some(requestBodyJson))
      }
    }
  }

}
