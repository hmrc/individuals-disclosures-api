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

package v1.controllers.validators

import api.controllers.validators.Validator
import api.controllers.validators.resolvers._
import api.models.errors.MtdError
import cats.data.Validated
import cats.implicits._
import play.api.libs.json.JsValue
import v1.controllers.validators.CreateMarriageAllowanceValidator.validateBusinessRules
import v1.models.request.create.{CreateMarriageAllowanceRequestBody, CreateMarriageAllowanceRequestData}

import javax.inject.Singleton
import scala.annotation.nowarn

@Singleton
class CreateMarriageAllowanceValidatorFactory {

  @nowarn("cat=lint-byname-implicit")
  private val resolveJson = new ResolveNonEmptyJsonObject[CreateMarriageAllowanceRequestBody]()

  def validator(nino: String, body: JsValue): Validator[CreateMarriageAllowanceRequestData] =
    new Validator[CreateMarriageAllowanceRequestData] {

      def validate: Validated[Seq[MtdError], CreateMarriageAllowanceRequestData] =
        (
          ResolveNino(nino),
          resolveJson(body)
        ).mapN(CreateMarriageAllowanceRequestData) andThen validateBusinessRules

    }

}
