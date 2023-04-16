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

package api.controllers.requestParsers.validators.validations

import api.models.errors.SRNFormatError
import support.UnitSpec

class SRNValidationSpec extends UnitSpec {

  "SRNValidation" when {
    "validate" must {
      "return an empty list for a valid srn" in {
        SRNValidation.validate(
          srn = "14211123"
        ) shouldBe NoValidationErrors
      }

      "return a SRNFormatError for an invalid srn" in {
        SRNValidation.validate(
          srn = "ABC142111235"
        ) shouldBe List(SRNFormatError)
      }
    }
  }
}