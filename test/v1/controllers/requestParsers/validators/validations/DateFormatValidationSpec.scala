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
import v1.models.errors.{MtdError, SRNFormatError}

class DateFormatValidationSpec extends UnitSpec {

  "DateFormatValidation" when {
    object DummyError extends MtdError("ERROR_CODE", "Error message", Some(Seq("/path")))

    "validate" must {
      "return an empty list for a valid date" in {
        DateFormatValidation.validate(date = "2019-04-20", DummyError) shouldBe NoValidationErrors
      }

      "return an error for an invalid date" in {
        DateFormatValidation.validate(date = "20-04-2017", DummyError) shouldBe List(DummyError)
      }
    }
  }
}
