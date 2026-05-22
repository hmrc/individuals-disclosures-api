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

package v2.services

import api.connectors.DownstreamOutcome
import api.models.domain.Nino
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v2.connectors.MockCreateMarriageAllowanceConnector
import v2.models.request.create.{CreateMarriageAllowanceRequestBody, CreateMarriageAllowanceRequestData}

import scala.concurrent.Future

class CreateMarriageAllowanceServiceSpec extends ServiceSpec {

  private val nino             = "AA112233A"
  private val requestBodyModel = CreateMarriageAllowanceRequestBody("TC663795B", Some("John"), "Smith", Some("1987-10-18"))

  val createMarriageAllowanceRequest: CreateMarriageAllowanceRequestData = CreateMarriageAllowanceRequestData(
    nino = Nino(nino),
    body = requestBodyModel
  )

  trait Test extends MockCreateMarriageAllowanceConnector {

    val service: CreateMarriageAllowanceService = new CreateMarriageAllowanceService(
      connector = mockCreateMarriageAllowanceConnector
    )

  }

  "CreateMarriageAllowanceService.create" should {
    "return success response" when {
      "a valid request is supplied" in new Test {
        val outcome: DownstreamOutcome[Unit] = Right(ResponseWrapper(correlationId, ()))

        MockCreateMarriageAllowanceConnector
          .create(createMarriageAllowanceRequest)
          .returns(Future.successful(outcome))

        await(service.create(createMarriageAllowanceRequest)) shouldBe outcome
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a code $downstreamErrorCode error is returned from the service" in new Test {

          MockCreateMarriageAllowanceConnector
            .create(createMarriageAllowanceRequest)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          await(service.create(createMarriageAllowanceRequest)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val input = List(
        ("INVALID_IDVALUE", NinoFormatError),
        ("DECEASED_PARTICIPANT", RuleDeceasedRecipientError),
        ("RELATIONSHIP_ALREADY_EXISTS", RuleActiveMarriageAllowanceClaimError),
        ("INVALID_IDTYPE", InternalError),
        ("END_DATE_CODE_NOT_FOUND", InternalError),
        ("INVALID_CORRELATIONID", InternalError),
        ("INVALID_PAYLOAD", InternalError),
        ("NINO_OR_TRN_NOT_FOUND", RuleInvalidRequestError),
        ("INVALID_ACTUAL_END_DATE", InternalError),
        ("INVALID_PARTICIPANT_END_DATE", InternalError),
        ("INVALID_PARTICIPANT_START_DATE", InternalError),
        ("INVALID_RELATIONSHIP_CODE", InternalError),
        ("PARTICIPANT1_CANNOT_BE_UPDATED", InternalError),
        ("PARTICIPANT2_CANNOT_BE_UPDATED", InternalError),
        ("CONFIDENCE_CHECK_FAILED", RuleInvalidRequestError.forPersonalDetailsMismatch),
        ("CONFIDENCE_CHECK_SURNAME_MISSED", InternalError),
        ("BAD_GATEWAY", InternalError),
        ("SERVER_ERROR", InternalError),
        ("SERVICE_UNAVAILABLE", InternalError)
      )

      input.foreach(serviceError.tupled)
    }
  }

}
