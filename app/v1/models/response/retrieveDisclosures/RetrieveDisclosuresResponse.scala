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

package v1.models.response.retrieveDisclosures

import config.AppConfig
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OWrites, Reads}
import utils.JsonUtils
import v1.hateoas.{HateoasLinks, HateoasLinksFactory}
import v1.models.hateoas.{HateoasData, Link}

case class RetrieveDisclosuresResponse(taxAvoidance: Option[Seq[TaxAvoidanceItem]],
                                       class2Nics: Option[Class2Nics],
                                       submittedOn: String)

object RetrieveDisclosuresResponse extends HateoasLinks with JsonUtils {

  implicit val reads: Reads[RetrieveDisclosuresResponse] = (
    (JsPath \ "taxAvoidance").readNullable[Seq[TaxAvoidanceItem]].mapEmptySeqToNone and
      (JsPath \ "class2Nics").readNullable[Class2Nics].mapEmptyModelToNone(Class2Nics.empty) and
      (JsPath \ "submittedOn").read[String]
    ) (RetrieveDisclosuresResponse.apply _)

  implicit val writes: OWrites[RetrieveDisclosuresResponse] = Json.writes[RetrieveDisclosuresResponse]

  implicit object RetrieveDisclosuresLinksFactory extends HateoasLinksFactory[RetrieveDisclosuresResponse, RetrieveDisclosuresHateoasData] {
    override def links(appConfig: AppConfig, data: RetrieveDisclosuresHateoasData): Seq[Link] = {
      import data._
      Seq(
        amendDisclosures(appConfig, nino, taxYear),
        retrieveDisclosures(appConfig, nino, taxYear),
        deleteDisclosures(appConfig, nino, taxYear)
      )
    }
  }
}

case class RetrieveDisclosuresHateoasData(nino: String, taxYear: String) extends HateoasData