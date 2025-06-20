package uk.gov.hmrc.crdlcache.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class CustomsOfficeDetailSpec extends AnyFlatSpec with Matchers with TestData {
  "CustomsOfficeDetail.fromDpsCustomsOfficeDetail" should "convert a DpsCustomsOfficeDetail to the crdl-cache CustomsOfficeDetail model" in {
    val inputCustomOfficeDetail = DK003102.customsofficelsd.head
    val expectedCustomOfficeDetail = CustomsOfficeDetail(
      "Hirtshals Toldekspedition",
      "DA",
      "Hirtshals",
      false,
      None,
      None,
      false,
      "Dalsagervej 7"
    )

    CustomsOfficeDetail.fromDpsCustomsOfficeDetail(inputCustomOfficeDetail) mustBe expectedCustomOfficeDetail

  }
}
