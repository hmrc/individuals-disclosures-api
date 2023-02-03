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

import api.models.errors.MtdError
import play.api.http.Status.BAD_REQUEST
import support.UnitSpec

class DateFormatValidationSpec extends UnitSpec {

  "DateFormatValidation" when {
    object DummyError extends MtdError(code = "ERROR_CODE", message = "Error message", httpStatus = BAD_REQUEST, paths = Some(List("/path")))

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
