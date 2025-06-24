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

package v2.controllers.validators

import api.controllers.validators.Validator
import api.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import play.api.libs.json.JsValue
import v2.models.request.create.CreateMarriageAllowanceRequestData

trait MockCreateMarriageAllowanceValidatorFactory extends TestSuite with MockFactory {

  val mockCreateMarriageAllowanceValidatorFactory: CreateMarriageAllowanceValidatorFactory = mock[CreateMarriageAllowanceValidatorFactory]

  object MockedCreateMarriageAllowanceValidatorFactory {

    def validator(): CallHandler[Validator[CreateMarriageAllowanceRequestData]] =
      (mockCreateMarriageAllowanceValidatorFactory.validator(_: String, _: JsValue)).expects(*, *)

  }

  def willUseValidator(use: Validator[CreateMarriageAllowanceRequestData]): CallHandler[Validator[CreateMarriageAllowanceRequestData]] = {
    MockedCreateMarriageAllowanceValidatorFactory
      .validator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: CreateMarriageAllowanceRequestData): Validator[CreateMarriageAllowanceRequestData] =
    new Validator[CreateMarriageAllowanceRequestData] {
      def validate: Validated[Seq[MtdError], CreateMarriageAllowanceRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[CreateMarriageAllowanceRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[CreateMarriageAllowanceRequestData] =
    new Validator[CreateMarriageAllowanceRequestData] {
      def validate: Validated[Seq[MtdError], CreateMarriageAllowanceRequestData] = Invalid(result)
    }

}
