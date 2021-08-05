/*
 * Copyright 2021 HM Revenue & Customs
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
import v1.models.errors.{NinoFormatError, PartnerNinoFormatError}
import v1.models.utils.JsonErrorValidators

class NinoValidationSpec extends UnitSpec with JsonErrorValidators {

  val validNino = "AA123456A"
  val invalidNino = "AA123456ABCBBCBCBC"

  "validate" when {
    "default error is used" must {
      "return no errors" when {
        "when a valid NINO is supplied" in {
          NinoValidation.validate(validNino) shouldBe empty
        }
      }

      "return an error" when {
        "when an invalid NINO is supplied" in {
          NinoValidation.validate(invalidNino) shouldBe List(NinoFormatError)
        }
      }
    }

    "specific error is used" must {
      "return no errors" when {
        "when a valid NINO is supplied" in {
          NinoValidation.validate(validNino, PartnerNinoFormatError) shouldBe empty
        }
      }

      "return an error" when {
        "when an invalid NINO is supplied" in {
          NinoValidation.validate(invalidNino, PartnerNinoFormatError) shouldBe List(PartnerNinoFormatError)
        }
      }
    }
  }
}
