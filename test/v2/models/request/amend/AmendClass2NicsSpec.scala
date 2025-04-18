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

package v2.models.request.amend

import play.api.libs.json.{JsError, JsObject, Json}
import support.UnitSpec

class AmendClass2NicsSpec extends UnitSpec {

  private val json = Json.parse(
    """
      |{
      |   "class2VoluntaryContributions": true
      |}
    """.stripMargin
  )

  private val model = AmendClass2Nics(class2VoluntaryContributions = Some(true))

  "AmendClass2Nics" when {
    "read from valid JSON" should {
      "produce the expected AmendClass2Nics object" in {
        json.as[AmendClass2Nics] shouldBe model
      }
    }

    "read from empty JSON" should {
      "produce an empty AmendClass2Nics object" in {
        val emptyJson = JsObject.empty

        emptyJson.as[AmendClass2Nics] shouldBe AmendClass2Nics.empty
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        val invalidJson = Json.parse(
          """
            |{
            |   "class2VoluntaryContributions": "no"
            |}
          """.stripMargin
        )

        invalidJson.validate[AmendClass2Nics] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(model) shouldBe json
      }
    }
  }

}
