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

package v2.createAmendDisclosures.def2.model.request

import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import support.UnitSpec
import v2.createAmendDisclosures.def2.fixture.Def2_CreateAmendDisclosuresFixture.*

class Def2_CreateAmendDisclosuresRequestBodySpec extends UnitSpec {

  "Def2_CreateAmendDisclosuresRequestBody" when {
    "read from valid JSON" should {
      "produce the expected Def2_CreateAmendDisclosuresRequestBody object" in {
        mtdRequestBodyJson.as[Def2_CreateAmendDisclosuresRequestBody] shouldBe requestBodyModel
      }
    }

    "read from empty JSON" should {
      "produce an empty Def2_CreateAmendDisclosuresRequestBody object" in {
        JsObject.empty.as[Def2_CreateAmendDisclosuresRequestBody] shouldBe requestBodyModel.copy(
          taxAvoidance = None,
          class2Nics = None
        )
      }
    }

    "read from valid JSON with empty taxAvoidance array and class2Nics object" should {
      "produce an Def2_CreateAmendDisclosuresRequestBody with an empty List" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "taxAvoidance": [],
            |  "class2Nics": {}
            |}
          """.stripMargin
        )

        json.as[Def2_CreateAmendDisclosuresRequestBody] shouldBe requestBodyModel.copy(
          taxAvoidance = Some(List()),
          class2Nics = None
        )
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        val invalidJson: JsValue = Json.parse(
          """
            |{
            |  "taxAvoidance": [
            |    {
            |      "srn": true,
            |      "taxYear": "2020-21"
            |    }
            |  ],
            |  "class2Nics": {
            |    "class2VoluntaryContributions": true
            |  }
            |}
          """.stripMargin
        )

        invalidJson.validate[Def2_CreateAmendDisclosuresRequestBody] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(requestBodyModel) shouldBe downstreamRequestBodyJson
      }
    }
  }

}
