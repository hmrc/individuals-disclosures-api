package v1.models.request.sample

import uk.gov.hmrc.domain.Nino
import v1.models.domain.DesTaxYear

case class SampleRequestData(nino: Nino, desTaxYear: DesTaxYear, body: SampleRequestBody)
