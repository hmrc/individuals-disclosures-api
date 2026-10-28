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

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc.Result
import v2.createAmendDisclosures.def2.fixture.Def2_CreateAmendDisclosuresFixture.*
import v2.createAmendDisclosures.def2.model.request.Def2_CreateAmendDisclosuresRequestData
import v2.createAmendDisclosures.model.request.CreateAmendDisclosuresRequestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateAmendDisclosuresControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockCreateAmendDisclosuresService
    with MockCreateAmendDisclosuresValidatorFactory {

  private val taxYear: String = "2026-27"

  private val requestData: CreateAmendDisclosuresRequestData = Def2_CreateAmendDisclosuresRequestData(
    nino = Nino("AA123456A"),
    taxYear = TaxYear.fromMtd("2026-27"),
    body = requestBodyModel
  )

  "CreateAmendDisclosuresController" should {
    "return 204 NO_CONTENT" when {
      "the request received is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockCreateAmendDisclosuresService
          .createAmendDisclosures(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(expectedStatus = NO_CONTENT, maybeAuditRequestBody = Some(mtdRequestBodyJson))
      }
    }

    "return the error as per spec" when {
      "parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTestWithAudit(expectedError = NinoFormatError, maybeAuditRequestBody = Some(mtdRequestBodyJson))
      }

      "service errors occur" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockCreateAmendDisclosuresService
          .createAmendDisclosures(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTestWithAudit(expectedError = RuleTaxYearNotSupportedError, maybeAuditRequestBody = Some(mtdRequestBodyJson))
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking {

    val controller: CreateAmendDisclosuresController = new CreateAmendDisclosuresController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockCreateAmendDisclosuresValidatorFactory,
      service = mockCreateAmendDisclosuresService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedAppConfig.featureSwitches.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    override protected def callController(): Future[Result] = controller.createAmendDisclosures(nino, taxYear)(fakePostRequest(mtdRequestBodyJson))

    override protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateAmendDisclosures",
        transactionName = "create-amend-disclosures",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          request = Some(mtdRequestBodyJson),
          `X-CorrelationId` = correlationId,
          response = auditResponse
        )
      )

  }

}
