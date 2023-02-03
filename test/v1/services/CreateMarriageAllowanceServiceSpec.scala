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

package v1.services

import api.controllers.EndpointLogContext
import api.models.errors
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v1.mocks.connectors.MockCreateMarriageAllowanceConnector
import v1.models.domain.Nino
import v1.models.request.create.{ CreateMarriageAllowanceBody, CreateMarriageAllowanceRequest }

import scala.concurrent.Future

class CreateMarriageAllowanceServiceSpec extends ServiceSpec {

  private val nino             = "AA112233A"
  private val requestBodyModel = CreateMarriageAllowanceBody("TC663795B", Some("John"), "Smith", Some("1987-10-18"))

  val createMarriageAllowanceRequest: CreateMarriageAllowanceRequest = CreateMarriageAllowanceRequest(
    nino = Nino(nino),
    body = requestBodyModel
  )

  trait Test extends MockCreateMarriageAllowanceConnector {
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val service: CreateMarriageAllowanceService = new CreateMarriageAllowanceService(
      connector = mockCreateMarriageAllowanceConnector
    )
  }

  "CreateMarriageAllowanceService.create" should {
    "return success response" when {
      "a valid request is supplied" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockCreateMarriageAllowanceConnector
          .create(createMarriageAllowanceRequest)
          .returns(Future.successful(outcome))

        await(service.create(createMarriageAllowanceRequest)) shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
          s"a $downstreamErrorCode error is returned from the service" in new Test {

            MockCreateMarriageAllowanceConnector
              .create(createMarriageAllowanceRequest)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            await(service.create(createMarriageAllowanceRequest)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val input = List(
          ("INVALID_IDVALUE", NinoFormatError),
          ("DECEASED_PARTICIPANT", RuleDeceasedRecipientError),
          ("RELATIONSHIP_ALREADY_EXISTS", RuleActiveMarriageAllowanceClaimError),
          ("INVALID_IDTYPE", errors.InternalError),
          ("END_DATE_CODE_NOT_FOUND", errors.InternalError),
          ("INVALID_CORRELATIONID", errors.InternalError),
          ("INVALID_PAYLOAD", errors.InternalError),
          ("NINO_OR_TRN_NOT_FOUND", RuleInvalidRequestError),
          ("INVALID_ACTUAL_END_DATE", errors.InternalError),
          ("INVALID_PARTICIPANT_END_DATE", errors.InternalError),
          ("INVALID_PARTICIPANT_START_DATE", errors.InternalError),
          ("INVALID_RELATIONSHIP_CODE", errors.InternalError),
          ("PARTICIPANT1_CANNOT_BE_UPDATED", errors.InternalError),
          ("PARTICIPANT2_CANNOT_BE_UPDATED", errors.InternalError),
          ("CONFIDENCE_CHECK_FAILED", errors.InternalError),
          ("CONFIDENCE_CHECK_SURNAME_MISSED", errors.InternalError),
          ("BAD_GATEWAY", errors.InternalError),
          ("SERVER_ERROR", errors.InternalError),
          ("SERVICE_UNAVAILABLE", errors.InternalError)
        )

        val extra_errors = List {
          ("INVALID_NINO", NinoFormatError)
        }

        (input ++ extra_errors).foreach(args => (serviceError _).tupled(args))
      }
    }
  }
}
