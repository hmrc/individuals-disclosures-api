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

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.models.domain.{Nino, TaxYear}
import api.models.errors.{ErrorWrapper, NinoFormatError, TaxYearFormatError}
import api.models.outcomes.ResponseWrapper
import play.api.Configuration
import play.api.mvc.Result
import v2.retrieveDisclosures.def2.fixture.Def2_RetrieveDisclosuresFixture.*
import v2.retrieveDisclosures.def2.model.request.Def2_RetrieveDisclosuresRequestData
import v2.retrieveDisclosures.model.request.RetrieveDisclosuresRequestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveDisclosuresControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockRetrieveDisclosuresService
    with MockRetrieveDisclosuresValidatorFactory {

  private val taxYear: String = "2026-27"

  private val requestData: RetrieveDisclosuresRequestData = Def2_RetrieveDisclosuresRequestData(
    nino = Nino(nino),
    taxYear = TaxYear.fromMtd(taxYear)
  )

  "RetrieveDisclosuresController" should {
    "return 200 OK" when {
      "the request received is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveDisclosuresService
          .retrieve(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseBodyModel))))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseBodyJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTest(expectedError = NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveDisclosuresService
          .retrieve(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, TaxYearFormatError))))

        runErrorTest(expectedError = TaxYearFormatError)
      }
    }
  }

  private trait Test extends ControllerTest {

    val controller: RetrieveDisclosuresController = new RetrieveDisclosuresController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockRetrieveDisclosuresValidatorFactory,
      service = mockRetrieveDisclosuresService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedAppConfig.featureSwitches.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    override protected def callController(): Future[Result] = controller.retrieveDisclosures(nino, taxYear)(fakeGetRequest)
  }

}
