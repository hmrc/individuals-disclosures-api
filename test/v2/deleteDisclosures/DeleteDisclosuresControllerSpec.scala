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

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc.Result
import v2.deleteDisclosures.def1.model.request.Def1_DeleteDisclosuresRequestData
import v2.deleteDisclosures.model.request.DeleteDisclosuresRequestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteDisclosuresControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockDeleteDisclosuresService
    with MockDeleteDisclosuresValidatorFactory {

  private val taxYear: String = "2026-27"

  private val requestData: DeleteDisclosuresRequestData = Def1_DeleteDisclosuresRequestData(
    nino = Nino(nino),
    taxYear = TaxYear.fromMtd(taxYear)
  )

  "DeleteDisclosuresController" should {
    "return 204 NO_CONTENT" when {
      "the request received is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteDisclosuresService
          .delete(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(expectedStatus = NO_CONTENT)
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTestWithAudit(expectedError = NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteDisclosuresService
          .delete(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, TaxYearFormatError))))

        runErrorTestWithAudit(expectedError = TaxYearFormatError)
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking {

    val controller: DeleteDisclosuresController = new DeleteDisclosuresController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockDeleteDisclosuresService,
      validatorFactory = mockDeleteDisclosuresValidatorFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedAppConfig.featureSwitches.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    override protected def callController(): Future[Result] = controller.deleteDisclosures(nino, taxYear)(fakeRequest)

    override protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "DeleteDisclosures",
        transactionName = "delete-disclosures",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          request = None,
          `X-CorrelationId` = correlationId,
          response = auditResponse
        )
      )

  }

}
