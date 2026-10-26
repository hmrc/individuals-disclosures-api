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

package v2.retrieveDisclosures.def1.model.response

import api.models.domain.Timestamp
import play.api.libs.json.{Json, OFormat}
import v2.retrieveDisclosures.model.response.RetrieveDisclosuresResponse

case class Def1_RetrieveDisclosuresResponse(taxAvoidance: Option[Seq[TaxAvoidanceItem]], class2Nics: Option[Class2Nics], submittedOn: Timestamp)
    extends RetrieveDisclosuresResponse

object Def1_RetrieveDisclosuresResponse {
  implicit val format: OFormat[Def1_RetrieveDisclosuresResponse] = Json.format[Def1_RetrieveDisclosuresResponse]
}
