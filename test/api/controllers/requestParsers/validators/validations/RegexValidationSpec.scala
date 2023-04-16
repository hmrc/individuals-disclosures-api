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

import scala.util.matching.Regex

class RegexValidationSpec extends UnitSpec {

  val validation: RegexValidation = new RegexValidation {
    override val regex: Regex = "A[0-9]B".r
  }

  "RegexValidation" when {
    object DummyError extends MtdError(code = "ERROR_CODE", message = "Error message", httpStatus = BAD_REQUEST, paths = Some(List("/path")))

    "matches regex" must {
      "return an empty list" in {
        validation.validate("A1B", DummyError) shouldBe Nil
      }
    }

    "does not match regex" must {
      "return the error in a list" in {
        validation.validate("AxB", DummyError) shouldBe List(DummyError)
        validation.validate("ABC", DummyError) shouldBe List(DummyError)
      }
    }
  }
}