/*
 * Copyright 2025 HM Revenue & Customs
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

package api.models.errors

import play.api.http.Status._

// Format Errors
object NinoFormatError extends MtdError("FORMAT_NINO", "The provided NINO is invalid", BAD_REQUEST)

object TaxYearFormatError extends MtdError("FORMAT_TAX_YEAR", "The provided tax year is invalid", BAD_REQUEST)

object BusinessIdFormatError extends MtdError("FORMAT_BUSINESS_ID", "The provided Business ID is invalid", BAD_REQUEST)

object InvalidTaxYearParameterError
    extends MtdError(code = "INVALID_TAX_YEAR_PARAMETER", message = "A tax year before 2023-24 was supplied", BAD_REQUEST)

object ValueFormatError extends MtdError("FORMAT_VALUE", "The value must be between 0 and 99999999999.99", BAD_REQUEST) {

  def forPathAndRange(path: String, min: String, max: String): MtdError =
    ValueFormatError.copy(paths = Some(Seq(path)), message = s"The value must be between $min and $max")

}

object CalculationIdFormatError extends MtdError("FORMAT_CALCULATION_ID", "The provided calculation ID is invalid", BAD_REQUEST)

object StartDateFormatError extends MtdError("FORMAT_START_DATE", "The provided Start date is invalid", BAD_REQUEST)
object EndDateFormatError   extends MtdError("FORMAT_END_DATE", "The provided End date is invalid", BAD_REQUEST)

object SRNFormatError extends MtdError("FORMAT_SRN_INVALID", "The provided scheme reference number is invalid", BAD_REQUEST)

object PartnerFirstNameFormatError
    extends MtdError("FORMAT_SPOUSE_OR_CIVIL_PARTNERS_FIRST_NAME", "The provided spouse or civil partner's first name is invalid", BAD_REQUEST)

object PartnerSurnameFormatError
    extends MtdError("FORMAT_SPOUSE_OR_CIVIL_PARTNERS_SURNAME", "The provided spouse or civil partner's surname is invalid", BAD_REQUEST)

object PartnerNinoFormatError
    extends MtdError(
      "FORMAT_SPOUSE_OR_CIVIL_PARTNERS_NINO",
      "The provided spouse or civil partner's National Insurance Number is invalid",
      BAD_REQUEST)

object PartnerDoBFormatError
    extends MtdError(
      "FORMAT_SPOUSE_OR_CIVIL_PARTNERS_DATE_OF_BIRTH",
      "The provided spouse or civil partner's date of birth date is invalid",
      BAD_REQUEST)

// Rule Errors
object RuleTaxYearNotSupportedError
    extends MtdError(
      "RULE_TAX_YEAR_NOT_SUPPORTED",
      "The specified tax year is not supported. That is, the tax year specified is before the minimum tax year value",
      BAD_REQUEST
    )

object RuleVoluntaryClass2CannotBeChangedError
    extends MtdError(
      "RULE_VOLUNTARY_CLASS2_CANNOT_BE_CHANGED",
      "Voluntary Class 2 NICs cannot be changed after 31st Jan following the year of submission",
      BAD_REQUEST)

object RuleIncorrectOrEmptyBodyError
    extends MtdError("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "An empty or non-matching body was submitted", BAD_REQUEST)

object RuleTaxYearRangeInvalidError
    extends MtdError("RULE_TAX_YEAR_RANGE_INVALID", "Tax year range invalid. A tax year range of one year is required", BAD_REQUEST)

object RuleEndBeforeStartDateError
    extends MtdError("RULE_END_DATE_BEFORE_START_DATE", "The supplied accounting period end date is before the start date", BAD_REQUEST)

object RuleVoluntaryClass2ValueInvalidError
    extends MtdError("RULE_VOLUNTARY_CLASS2_VALUE_INVALID", "Voluntary Class 2 Contributions can only be set to true", BAD_REQUEST)

object RuleDeceasedRecipientError extends MtdError("RULE_DECEASED_RECIPIENT", "The provided spouse or civil partner is deceased", BAD_REQUEST)

object RuleActiveMarriageAllowanceClaimError
    extends MtdError(
      "RULE_ACTIVE_MARRIAGE_ALLOWANCE_CLAIM",
      "Marriage Allowance has already been transferred to a spouse or civil partner",
      BAD_REQUEST)

object RuleInvalidRequestError extends MtdError("RULE_INVALID_REQUEST", "The NINO supplied is invalid", BAD_REQUEST)

object RuleOutsideAmendmentWindowError extends MtdError("RULE_OUTSIDE_AMENDMENT_WINDOW", "You are outside the amendment window", BAD_REQUEST)

//Standard Errors
object NotFoundError extends MtdError("MATCHING_RESOURCE_NOT_FOUND", "Matching resource not found", NOT_FOUND)

object InternalError extends MtdError("INTERNAL_SERVER_ERROR", "An internal server error occurred", INTERNAL_SERVER_ERROR)

object BadRequestError extends MtdError("INVALID_REQUEST", "Invalid request", BAD_REQUEST)

object BVRError extends MtdError("BUSINESS_ERROR", "Business validation error", BAD_REQUEST)

object ServiceUnavailableError extends MtdError("SERVICE_UNAVAILABLE", "Internal server error", INTERNAL_SERVER_ERROR)

object InvalidHttpMethodError extends MtdError("INVALID_HTTP_METHOD", "Invalid HTTP method", METHOD_NOT_ALLOWED)

//Authorisation Errors

object InvalidBearerTokenError extends MtdError("UNAUTHORIZED", "Bearer token is missing or not authorized", UNAUTHORIZED)

// Authentication OK but not allowed access to the requested resource
object ClientOrAgentNotAuthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client or agent is not authorised", FORBIDDEN) {
  def withStatus401: MtdError = copy(httpStatus = UNAUTHORIZED)
}

// Accept header Errors
object InvalidAcceptHeaderError extends MtdError("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid", NOT_ACCEPTABLE)

object UnsupportedVersionError extends MtdError("NOT_FOUND", "The requested resource could not be found", NOT_FOUND)

object InvalidBodyTypeError extends MtdError("INVALID_BODY_TYPE", "Expecting text/json or application/json body", UNSUPPORTED_MEDIA_TYPE)

//Stub Errors
object RuleIncorrectGovTestScenarioError extends MtdError("RULE_INCORRECT_GOV_TEST_SCENARIO", "The supplied Gov-Test-Scenario is not valid", BAD_REQUEST)
