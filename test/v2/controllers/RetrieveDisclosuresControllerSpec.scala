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

package v2.controllers

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.mocks.MockIdGenerator
import api.models.domain.{Nino, TaxYear, Timestamp}
import api.models.errors.{ErrorWrapper, NinoFormatError, TaxYearFormatError}
import api.models.outcomes.ResponseWrapper
import api.services.{MockEnrolmentsAuthService, MockMtdIdLookupService}
import config.MockAppConfig
import play.api.Configuration
import play.api.mvc.Result
import v2.controllers.validators.MockRetrieveDisclosuresValidatorFactory
import v2.fixtures.RetrieveDisclosuresControllerFixture.mtdResponse
import v2.models.request.retrieve.RetrieveDisclosuresRequestData
import v2.models.response.retrieveDisclosures.{Class2Nics, RetrieveDisclosuresResponse, TaxAvoidanceItem}
import v2.services.MockRetrieveDisclosuresService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveDisclosuresControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockRetrieveDisclosuresService
    with MockRetrieveDisclosuresValidatorFactory
    with MockIdGenerator
    with MockAppConfig {

  val taxYear: String = "2021-22"

  val requestData: RetrieveDisclosuresRequestData = RetrieveDisclosuresRequestData(
    nino = Nino(nino),
    taxYear = TaxYear.fromMtd(taxYear)
  )

  val taxAvoidanceModel: Seq[TaxAvoidanceItem] = Seq(
    TaxAvoidanceItem(
      srn = "14211123",
      taxYear = "2020-21"
    ),
    TaxAvoidanceItem(
      srn = "34522678",
      taxYear = "2021-22"
    )
  )

  val class2NicsModel: Class2Nics = Class2Nics(class2VoluntaryContributions = Some(true))

  val retrieveDisclosuresResponseModel: RetrieveDisclosuresResponse = RetrieveDisclosuresResponse(
    taxAvoidance = Some(taxAvoidanceModel),
    class2Nics = Some(class2NicsModel),
    submittedOn = Timestamp("2020-07-06T09:37:17Z")
  )

  "RetrieveDisclosuresController" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveDisclosuresService
          .retrieve(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveDisclosuresResponseModel))))

        runOkTest(OK, Some(mtdResponse))

      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveDisclosuresService
          .retrieve(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, TaxYearFormatError))))

        runErrorTest(TaxYearFormatError)
      }
    }

  }

  trait Test extends ControllerTest {

    MockedAppConfig.featureSwitches.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    val controller: RetrieveDisclosuresController = new RetrieveDisclosuresController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockRetrieveDisclosuresValidatorFactory,
      service = mockRetrieveDisclosuresService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    MockedAppConfig.minimumPermittedTaxYear
      .returns(2022)
      .anyNumberOfTimes()

    override protected def callController(): Future[Result] = controller.retrieveDisclosures(nino, taxYear)(fakeGetRequest)

  }

}
