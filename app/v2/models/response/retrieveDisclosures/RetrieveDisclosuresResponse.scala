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

package v2.models.response.retrieveDisclosures

import api.models.domain.Timestamp
import play.api.libs.functional.syntax.*
import play.api.libs.json.{JsPath, Json, OWrites, Reads}
import utils.JsonUtils

case class RetrieveDisclosuresResponse(taxAvoidance: Option[Seq[TaxAvoidanceItem]], class2Nics: Option[Class2Nics], submittedOn: Timestamp)

object RetrieveDisclosuresResponse extends  JsonUtils {

  implicit val reads: Reads[RetrieveDisclosuresResponse] = (
    (JsPath \ "taxAvoidance").readNullable[Seq[TaxAvoidanceItem]].mapEmptySeqToNone and
      (JsPath \ "class2Nics").readNullable[Class2Nics].mapEmptyModelToNone(Class2Nics.empty) and
      (JsPath \ "submittedOn").read[Timestamp]
  )(RetrieveDisclosuresResponse.apply)

  implicit val writes: OWrites[RetrieveDisclosuresResponse] = Json.writes[RetrieveDisclosuresResponse]


}

