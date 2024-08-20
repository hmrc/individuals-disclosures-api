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

package auth

import api.models.domain.TaxYear
import api.services.DownstreamStub
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSRequest, WSResponse}

class DisclosuresAPIAuthSupportingAgentsOnlyISpec extends AuthMainAgentsOnlyISpec {

  val callingApiVersion = "1.0"

  val supportingAgentsNotAllowedEndpoint = "delete-disclosures"

  private val taxYear = TaxYear.fromMtd("2021-22")

  val mtdUrl = s"/$nino/${taxYear.asMtd}"

  def sendMtdRequest(request: WSRequest): WSResponse = await(request.delete())

  val downstreamUri: String = s"/income-tax/disclosures/$nino/${taxYear.asMtd}"

  val maybeDownstreamResponseJson: Option[JsValue] = None

  override val downstreamHttpMethod: DownstreamStub.HTTPMethod = DownstreamStub.DELETE

  override val expectedMtdSuccessStatus: Int = NO_CONTENT

}
