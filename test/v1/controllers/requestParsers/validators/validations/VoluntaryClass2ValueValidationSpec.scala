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

package v1.controllers.requestParsers.validators.validations

import support.UnitSpec
import v1.models.errors.RuleVoluntaryClass2ValueInvalidError

class VoluntaryClass2ValueValidationSpec extends UnitSpec {

  "VoluntaryClass2ValueValidation" when {
    "validateOptional" must {
      "return an empty list for a value of 'None'" in {
        VoluntaryClass2ValueValidation.validateOptional(
          class2VoluntaryContributions = None
        ) shouldBe NoValidationErrors
      }

      "validate correctly for some valid boolean value i.e. true" in {
        VoluntaryClass2ValueValidation.validateOptional(
          class2VoluntaryContributions = Some(true)
        ) shouldBe NoValidationErrors
      }

      "validate correctly for some invalid boolean value i.e. false" in {
        VoluntaryClass2ValueValidation.validateOptional(
          class2VoluntaryContributions = Some(false)
        ) shouldBe List(RuleVoluntaryClass2ValueInvalidError)
      }
    }
  }
}