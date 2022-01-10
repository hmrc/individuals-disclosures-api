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

package v1.fixtures

import play.api.libs.json.{JsObject, JsValue, Json}

object RetrieveDisclosuresControllerFixture {

  val fullRetrieveDisclosuresResponse: JsValue = Json.parse(
    """
      |{
      |  "taxAvoidance": [
      |    {
      |      "srn": "14211123",
      |      "taxYear": "2020-21"
      |    },
      |    {
      |      "srn": "34522678",
      |      "taxYear": "2021-22"
      |    }
      |  ],
      |  "class2Nics": {
      |     "class2VoluntaryContributions": true
      |  },
      |  "submittedOn": "2020-07-06T09:37:17Z"
      |}
    """.stripMargin
  )

  def mtdResponseWithHateoas(nino: String, taxYear: String): JsObject = fullRetrieveDisclosuresResponse.as[JsObject] ++ Json.parse(
    s"""
       |{
       |   "links":[
       |      {
       |         "href":"/individuals/disclosures/$nino/$taxYear",
       |         "method":"PUT",
       |         "rel":"create-and-amend-disclosures"
       |      },
       |      {
       |         "href":"/individuals/disclosures/$nino/$taxYear",
       |         "method":"GET",
       |         "rel":"self"
       |      },
       |      {
       |         "href":"/individuals/disclosures/$nino/$taxYear",
       |         "method":"DELETE",
       |         "rel":"delete-disclosures"
       |      }
       |   ]
       |}
    """.stripMargin
  ).as[JsObject]
}