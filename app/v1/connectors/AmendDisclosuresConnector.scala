/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.connectors

import config.AppConfig
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v1.models.request.disclosures.AmendDisclosuresRequest

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendDisclosuresConnector @Inject()(val http: HttpClient,
                                          val appConfig: AppConfig) extends BaseDesConnector {

  def amendDisclosures(request: AmendDisclosuresRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[DesOutcome[Unit]] = {

    import v1.connectors.httpparsers.StandardDesHttpParser._

    put(
      uri = DesUri[Unit](s"disc-placeholder/disclosures/${request.nino}/${request.taxYear.value}"), body = request.body
    )
  }
}