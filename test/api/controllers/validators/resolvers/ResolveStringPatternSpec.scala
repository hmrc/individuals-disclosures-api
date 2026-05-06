/*
 * Copyright 2026 HM Revenue & Customs
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

package api.controllers.validators.resolvers

import cats.data.Validated.{Invalid, Valid}
import api.models.errors.RuleIncorrectOrEmptyBodyError
import support.UnitSpec

import scala.util.matching.Regex

class ResolveStringPatternSpec extends UnitSpec {

  private val nameRegex: Regex        = "^[A-Za-z0-9 ,.()/&'-]{1,35}$".r
  private val resolveNameRegexPattern = ResolveStringPattern(nameRegex, RuleIncorrectOrEmptyBodyError)

  "ResolveStringPattern" should {
    "return no errors for a mandatory string" when {
      "given a matching string" in {
        val result = resolveNameRegexPattern("some name")
        result shouldBe Valid("some name")
      }

      "given a matching string with leading/trailing whitespace" in {
        val result = resolveNameRegexPattern("    some name    ")
        result shouldBe Valid("    some name    ")
      }
    }

    "return no errors for an optional string" when {
      "given a matching optional string" in {
        val result = resolveNameRegexPattern(Some("some name"))
        result shouldBe Valid(Some("some name"))
      }

      "given no string" in {
        val result = resolveNameRegexPattern(None)
        result shouldBe Valid(None)
      }
    }

    "return the expected error for a mandatory string" when {
      "given an invalid string" in {
        val result = resolveNameRegexPattern("!#@?")
        result shouldBe Invalid(List(RuleIncorrectOrEmptyBodyError))
      }

      "given an empty string" in {
        val result = resolveNameRegexPattern("")
        result shouldBe Invalid(List(RuleIncorrectOrEmptyBodyError))
      }
  
      "given only whitespace" in {
        val result = resolveNameRegexPattern("    ")
        result shouldBe Invalid(List(RuleIncorrectOrEmptyBodyError))
      }
    }

    "return the expected error for an optional string" when {
      "given an invalid string" in {
        val result = resolveNameRegexPattern(Some("a" * 322))
        result shouldBe Invalid(List(RuleIncorrectOrEmptyBodyError))
      }

      "given an empty string" in {
        val result = resolveNameRegexPattern(Some(""))
        result shouldBe Invalid(List(RuleIncorrectOrEmptyBodyError))
      }
  
      "given only whitespace" in {
        val result = resolveNameRegexPattern(Some("    "))
        result shouldBe Invalid(List(RuleIncorrectOrEmptyBodyError))
      }
    }
  }
}
