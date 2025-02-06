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

package v2.controllers

import api.controllers._
import api.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import config.AppConfig
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import utils.IdGenerator
import v2.controllers.validators.CreateMarriageAllowanceValidatorFactory
import v2.services.CreateMarriageAllowanceService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CreateMarriageAllowanceController @Inject() (val authService: EnrolmentsAuthService,
                                                   val lookupService: MtdIdLookupService,
                                                   validatorFactory: CreateMarriageAllowanceValidatorFactory,
                                                   service: CreateMarriageAllowanceService,
                                                   auditService: AuditService,
                                                   cc: ControllerComponents,
                                                   val idGenerator: IdGenerator)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends AuthorisedController(cc) {

  val endpointName = "create-marriage-allowance"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateMarriageAllowanceController", endpointName = "createMarriageAllowance")

  def createMarriageAllowance(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, request.body)

      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.create)
          .withNoContentResult(CREATED)
          .withAuditing(AuditHandler(
            auditService,
            auditType = "CreateMarriageAllowanceClaim",
            transactionName = "create-marriage-allowance-claim",
            params = Map("nino" -> nino),
            Some(request.body),
            includeResponse = true
          ))

      requestHandler.handleRequest()
    }

}
