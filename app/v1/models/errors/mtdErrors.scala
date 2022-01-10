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

package v1.models.errors

import play.api.libs.json.{Json, OWrites}

case class MtdError(code: String, message: String, paths: Option[Seq[String]] = None)

object MtdError {
  implicit val writes: OWrites[MtdError] = Json.writes[MtdError]

  implicit def genericWrites[T <: MtdError]: OWrites[T] =
    writes.contramap[T](c => c: MtdError)
}

object MtdErrorWithCustomMessage {
  def unapply(arg: MtdError): Option[String] = Some(arg.code)
}

object NinoFormatError extends MtdError(
  code = "FORMAT_NINO",
  message = "The provided NINO is invalid"
)

object TaxYearFormatError extends MtdError(
  code = "FORMAT_TAX_YEAR",
  message = "The provided tax year is invalid"
)

object SRNFormatError extends MtdError(
  code = "FORMAT_SRN_INVALID",
  message = "The provided scheme reference number is invalid"
)

object PartnerFirstNameFormatError extends MtdError(
  code = "FORMAT_SPOUSE_OR_CIVIL_PARTNERS_FIRST_NAME",
  message = "The provided spouse or civil partner's first name is invalid"
)

object PartnerSurnameFormatError extends MtdError(
  code = "FORMAT_SPOUSE_OR_CIVIL_PARTNERS_SURNAME",
  message = "The provided spouse or civil partner's surname is invalid"
)

object PartnerNinoFormatError extends MtdError(
  code = "FORMAT_SPOUSE_OR_CIVIL_PARTNERS_NINO",
  message = "The provided spouse or civil partner's National Insurance Number is invalid"
)

object PartnerDoBFormatError extends MtdError(
  code = "FORMAT_SPOUSE_OR_CIVIL_PARTNERS_DATE_OF_BIRTH",
  message = "The provided spouse or civil partner's date of birth date is invalid"
)


// Rule Errors
object RuleTaxYearNotSupportedError extends MtdError(
  code = "RULE_TAX_YEAR_NOT_SUPPORTED",
  message = "The specified tax year is not supported. That is, the tax year specified is before the minimum tax year value"
)

object RuleVoluntaryClass2CannotBeChangedError extends MtdError(
  code = "RULE_VOLUNTARY_CLASS2_CANNOT_BE_CHANGED",
  message = "Voluntary Class 2 NICs cannot be changed after 31st Jan following the year of submission"
)

object RuleIncorrectOrEmptyBodyError extends MtdError(
  code = "RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED",
  message = "An empty or non-matching body was submitted"
)

object RuleTaxYearRangeInvalidError extends MtdError(
  code = "RULE_TAX_YEAR_RANGE_INVALID",
  message = "Tax year range invalid. A tax year range of one year is required"
)

object RuleVoluntaryClass2ValueInvalidError extends MtdError(
  code = "RULE_VOLUNTARY_CLASS2_VALUE_INVALID",
  message = "Voluntary Class 2 Contributions can only be set to true"
)

object RuleDeceasedRecipientError extends MtdError(
  code = "RULE_DECEASED_RECIPIENT",
  message = "The provided spouse or civil partner is deceased"
)

object RuleActiveMarriageAllowanceClaimError extends MtdError(
  code = "RULE_ACTIVE_MARRIAGE_ALLOWANCE_CLAIM",
  message = "Marriage Allowance has already been transferred to a spouse or civil partner"
)

object RuleInvalidRequestError extends MtdError(
  code = "RULE_INVALID_REQUEST",
  message = "The NINO supplied is invalid"
)

//Standard Errors
object NotFoundError extends MtdError(
  code = "MATCHING_RESOURCE_NOT_FOUND",
  message = "Matching resource not found"
)

object DownstreamError extends MtdError(
  code = "INTERNAL_SERVER_ERROR",
  message = "An internal server error occurred"
)

object BadRequestError extends MtdError(
  code = "INVALID_REQUEST",
  message = "Invalid request"
)

object BVRError extends MtdError(
  code = "BUSINESS_ERROR",
  message = "Business validation error"
)

object ServiceUnavailableError extends MtdError(
  code = "SERVICE_UNAVAILABLE",
  message = "Internal server error"
)

//Authorisation Errors
object UnauthorisedError extends MtdError(
  code = "CLIENT_OR_AGENT_NOT_AUTHORISED",
  message = "The client and/or agent is not authorised"
)

object InvalidBearerTokenError extends MtdError(
  code = "UNAUTHORIZED",
  message = "Bearer token is missing or not authorized"
)

// Accept header Errors
object InvalidAcceptHeaderError extends MtdError(
  code = "ACCEPT_HEADER_INVALID",
  message = "The accept header is missing or invalid"
)

object UnsupportedVersionError extends MtdError(
  code = "NOT_FOUND",
  message = "The requested resource could not be found"
)

object InvalidBodyTypeError extends MtdError(
  code = "INVALID_BODY_TYPE",
  message = "Expecting text/json or application/json body"
)
