/*
 * Copyright 2022 HM Revenue & Customs
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

package v1.controllers.requestParsers.validators

import config.AppConfig
import v1.controllers.requestParsers.validators.validations._
import v1.models.errors.MtdError
import v1.models.request.DeleteRetrieveRawData

import javax.inject.Inject
import scala.collection.immutable.ListSet

class DeleteRetrieveValidator @Inject()(implicit appConfig: AppConfig) extends Validator[DeleteRetrieveRawData] {
  private val validationSet = ListSet(parameterFormatValidation, parameterValueValidation)

  override def validate(data: DeleteRetrieveRawData): ListSet[MtdError] = run(validationSet, data)

  private def parameterFormatValidation: ValidationType = (data: DeleteRetrieveRawData) => ListSet(
    NinoValidation.validate(data.nino),
    TaxYearValidation.validate(data.taxYear)
  )

  private def parameterValueValidation: ValidationType = (data: DeleteRetrieveRawData) => ListSet(
    TaxYearNotSupportedValidation.validate(data.taxYear)
  )
}
