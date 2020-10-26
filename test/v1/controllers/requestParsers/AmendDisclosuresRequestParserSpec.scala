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

package v1.controllers.requestParsers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v1.mocks.validators.MockAmendDisclosuresValidator
import v1.models.errors._
import v1.models.request.disclosures._

class AmendDisclosuresRequestParserSpec extends UnitSpec {

  val nino: String = "AA123456B"
  val taxYear: String = "2020-21"

  private val validRequestBodyJson: JsValue = Json.parse(
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
      |  }
      |}
    """.stripMargin
  )

  private val validRawRequestBody = AnyContentAsJson(validRequestBodyJson)

  private val taxAvoidanceModel = Seq(
    AmendTaxAvoidance(
      srn = "14211123",
      taxYear = "2020-21"
    ),
    AmendTaxAvoidance(
      srn = "34522678",
      taxYear = "2021-22"
    )
  )

  val class2Nics: AmendClass2Nics = AmendClass2Nics(true)

  private val validRequestBodyModel = AmendDisclosuresRequestBody(
    Some(taxAvoidanceModel),
    Some(class2Nics)
  )

  private val amendDisclosuresRawData = AmendDisclosuresRawData(
    nino = nino,
    taxYear = taxYear,
    body = validRawRequestBody
  )

  trait Test extends MockAmendDisclosuresValidator {
    lazy val parser: AmendDisclosuresRequestParser = new AmendDisclosuresRequestParser(
      validator = mockAmendDisclosuresValidator
    )
  }

  "parse" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockAmendDisclosuresValidator.validate(amendDisclosuresRawData).returns(Nil)

        parser.parseRequest(amendDisclosuresRawData) shouldBe
          Right(AmendDisclosuresRequest(Nino(nino), taxYear, validRequestBodyModel))
      }
    }

    "return an ErrorWrapper" when {
      "a single validation error occurs" in new Test {
        MockAmendDisclosuresValidator.validate(amendDisclosuresRawData.copy(nino = "notANino"))
          .returns(List(NinoFormatError))

        parser.parseRequest(amendDisclosuresRawData.copy(nino = "notANino")) shouldBe
          Left(ErrorWrapper("", NinoFormatError, None))
      }

      "multiple path parameter validation errors occur" in new Test {
        MockAmendDisclosuresValidator.validate(amendDisclosuresRawData.copy(nino = "notANino", taxYear = "notATaxYear"))
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(amendDisclosuresRawData.copy(nino = "notANino", taxYear = "notATaxYear")) shouldBe
          Left(ErrorWrapper("", BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }

      "path parameter TaxYearNotSupported validation occurs" in new Test {
        MockAmendDisclosuresValidator.validate(amendDisclosuresRawData.copy(taxYear = "2019-20"))
          .returns(List(RuleTaxYearNotSupportedError))

        parser.parseRequest(amendDisclosuresRawData.copy(taxYear = "2019-20")) shouldBe
          Left(ErrorWrapper("", RuleTaxYearNotSupportedError))
      }

      "multiple field value validation errors occur" in new Test {

        private val allInvalidValueRequestBodyJson: JsValue = Json.parse(
          """
            |{
            |  "taxAvoidance": [
            |    {
            |      "srn": "ABC142111235D",
            |      "taxYear": "2020"
            |    },
            |    {
            |      "srn": "CDE345226789F",
            |      "taxYear": "2020-22"
            |    }
            |  ]
            |}
          """.stripMargin
        )

        private val allInvalidValueRawRequestBody = AnyContentAsJson(allInvalidValueRequestBodyJson)

        private val allInvalidValueErrors = List(
          RuleTaxYearRangeInvalidError.copy(
            paths = Some(List(
              "/taxAvoidance/1/taxYear"
            ))
          ),
          TaxYearFormatError.copy(
            paths = Some(List(
              "/taxAvoidance/0/taxYear"
            ))
          ),
          SRNFormatError.copy(
            paths = Some(List(
              "/taxAvoidance/0/srn",
              "/taxAvoidance/1/srn"
            ))
          )
        )

        MockAmendDisclosuresValidator.validate(amendDisclosuresRawData.copy(body = allInvalidValueRawRequestBody))
          .returns(allInvalidValueErrors)

        parser.parseRequest(amendDisclosuresRawData.copy(body = allInvalidValueRawRequestBody)) shouldBe
          Left(ErrorWrapper("", BadRequestError, Some(allInvalidValueErrors)))
      }
    }
  }
}