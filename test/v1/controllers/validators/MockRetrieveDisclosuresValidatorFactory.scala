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

package v1.controllers.validators

import api.controllers.validators.Validator
import api.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import v1.models.request.retrieve.RetrieveDisclosuresRequestData

trait MockRetrieveDisclosuresValidatorFactory extends TestSuite with MockFactory {

  val mockRetrieveDisclosuresValidatorFactory: RetrieveDisclosuresValidatorFactory = mock[RetrieveDisclosuresValidatorFactory]

  object MockedRetrieveDisclosuresValidatorFactory {

    def validator(): CallHandler[Validator[RetrieveDisclosuresRequestData]] =
      (mockRetrieveDisclosuresValidatorFactory.validator(_: String, _: String)).expects(*, *)

  }

  def willUseValidator(use: Validator[RetrieveDisclosuresRequestData]): CallHandler[Validator[RetrieveDisclosuresRequestData]] = {
    MockedRetrieveDisclosuresValidatorFactory
      .validator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: RetrieveDisclosuresRequestData): Validator[RetrieveDisclosuresRequestData] =
    new Validator[RetrieveDisclosuresRequestData] {
      def validate: Validated[Seq[MtdError], RetrieveDisclosuresRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[RetrieveDisclosuresRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[RetrieveDisclosuresRequestData] = new Validator[RetrieveDisclosuresRequestData] {
    def validate: Validated[Seq[MtdError], RetrieveDisclosuresRequestData] = Invalid(result)
  }

}
