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

package v2.models.response.retrieveDisclosures

import api.models.domain.Timestamp
import play.api.libs.json.{JsError, Json}
import support.UnitSpec
import v2.retrieve.def1.model.response.{Def1_Class2Nics, Def1_RetrieveDisclosuresResponse, Def1_TaxAvoidanceItem}

class Def1RetrieveDisclosuresResponseSpec extends UnitSpec {

  private val mtdJson = Json.parse(
    """
      |{
      |   "taxAvoidance": [
      |      {
      |         "srn": "14211123",
      |         "taxYear": "2020-21"
      |      }
      |   ],
      |   "class2Nics": {
      |      "class2VoluntaryContributions": true
      |   },
      |   "submittedOn": "2020-07-06T09:37:17.000Z"
      |}
    """.stripMargin
  )

  private val downstreamJson = Json.parse(
    """
      |{
      |   "taxAvoidance": [
      |      {
      |         "SRN": "14211123",
      |         "taxYear": "2020-21"
      |      }
      |   ],
      |   "class2Nics": {
      |      "class2VoluntaryContributions": true
      |   },
      |   "submittedOn": "2020-07-06T09:37:17.000Z"
      |}
    """.stripMargin
  )

  private val taxAvoidanceItem = List(
    Def1_TaxAvoidanceItem(
      srn = "14211123",
      taxYear = "2020-21"
    )
  )

  private val class2NicsModel = Def1_Class2Nics(class2VoluntaryContributions = Some(true))

  private val parsedResponse = Def1_RetrieveDisclosuresResponse(
    taxAvoidance = Some(taxAvoidanceItem),
    def1Class2Nics = Some(class2NicsModel),
    submittedOn = Timestamp("2020-07-06T09:37:17Z")
  )

  "RetrieveDisclosuresResponse" when {
    "read from valid JSON" should {
      "produce the expected RetrieveDisclosuresResponse object" in {
        downstreamJson.as[Def1_RetrieveDisclosuresResponse] shouldBe parsedResponse
      }
    }

    "read from valid JSON with empty taxAvoidance array and class2Nics object" should {
      "produce the expected RetrieveDisclosuresResponse object" in {
        val json = Json.parse(
          """
            |{
            |   "taxAvoidance": [ ],
            |   "class2Nics": { },
            |   "submittedOn": "2020-07-06T09:37:17Z"
            |}
          """.stripMargin
        )

        json.as[Def1_RetrieveDisclosuresResponse] shouldBe parsedResponse.copy(
          taxAvoidance = None,
          def1Class2Nics = None,
          submittedOn = Timestamp("2020-07-06T09:37:17Z"))
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        val invalidJson = Json.parse(
          """
            |{
            |   "taxAvoidance": [
            |      {
            |         "SRN": true,
            |         "taxYear": "2020-21"
            |      }
            |   ],
            |   "class2Nics": {
            |      "class2VoluntaryContributions": true
            |   },
            |   "submittedOn": "2020-07-06T09:37:17Z"
            |}
          """.stripMargin
        )
        invalidJson.validate[Def1_RetrieveDisclosuresResponse] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(parsedResponse) shouldBe mtdJson
      }
    }
  }

}
