package au.com.dmg.tefdatamesh


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

//    @Test
//    fun test() {
//        val xml = """<?xml version="1.0" encoding="UTF-8"?><Document><Header><HeaderField Key="PosId">29</HeaderField><HeaderField Key="Alias"/><HeaderField Key="SaleId">3ee459e5-e468-40ed-b5ae-788ed927f1aa</HeaderField><HeaderField Key="Serie"/><HeaderField Key="Number">0</HeaderField><HeaderField Key="DocumentTypeId">0</HeaderField><HeaderField Key="Z">3</HeaderField><HeaderField Key="Date">10-30-2023</HeaderField><HeaderField Key="Time">10:10:22</HeaderField><HeaderField Key="IsTaxIncluded">true</HeaderField><HeaderField Key="IsClosed">false</HeaderField><HeaderField Key="IsSubTotalized">false</HeaderField><HeaderField Key="TaxesAmount">5.0000</HeaderField><HeaderField Key="NetAmount">55.0000</HeaderField><HeaderField Key="ServiceTypeId">0</HeaderField><HeaderField Key="ServiceNumber">0</HeaderField><HeaderField Key="LoyaltyCardNumber"/><HeaderField Key="PrintCount">0</HeaderField><HeaderField Key="BlockToPrint">null</HeaderField><HeaderField Key="TicketToPrint"/><HeaderField Key="Rounding">0.0000</HeaderField><HeaderField Key="TaxExemption"/><HeaderField Key="IsoDocumentId"/><HeaderField Key="IsDemo">false</HeaderField><HeaderField Key="DocumentAPIPromoData"/><HeaderField Key="ControlCode"/><HeaderField Key="PriceListId">1</HeaderField><HeaderField Key="CurrencyISOCode">AUD</HeaderField><HeaderField Key="CurrencyExchangeRate">1.0000</HeaderField><HeaderField Key="CurrencyId">1</HeaderField><HeaderField Key="IsRoomCharge">false</HeaderField><HeaderField Key="RoomName"/><CustomDocHeaderFields/><Discount><DiscountField Key="DiscountReasonId">0</DiscountField><DiscountField Key="Percentage">0.0000</DiscountField><DiscountField Key="Amount">0.0000</DiscountField><DiscountField Key="AmountWithTaxes">0.0000</DiscountField></Discount><Company><CompanyField Key="CompanyId">12213</CompanyField><CompanyField Key="FiscalId">4455687811</CompanyField><CompanyField Key="Name">Bar n Grill (Eft testing)</CompanyField><CompanyField Key="TradeName">Bar n Grill</CompanyField><CompanyField Key="FiscalName">Bar n Grill</CompanyField><CompanyField Key="CorrectTradeName">Bar n Grill (Eft testing)</CompanyField><CompanyField Key="Address">Sydney</CompanyField><CompanyField Key="PostalCode">2015</CompanyField><CompanyField Key="City">Alexandria</CompanyField><CompanyField Key="Phone">96691999</CompanyField><CompanyField Key="Email">samuel@chspos.com.au</CompanyField><CustomCompanyFields/><CustomAccountingCompanyFields/></Company><Shop><ShopField Key="ShopId">1</ShopField><ShopField Key="FiscalId">4455687811</ShopField><ShopField Key="Name">Bar n Grill (Eft testing)</ShopField><ShopField Key="TradeName">Bar n Grill</ShopField><ShopField Key="FiscalName">Bar n Grill</ShopField><ShopField Key="CorrectTradeName">Bar n Grill (Eft testing)</ShopField><ShopField Key="Address">Sydney</ShopField><ShopField Key="PostalCode">2015</ShopField><ShopField Key="City">Alexandria</ShopField><ShopField Key="State">Council of the City of Sydney</ShopField><ShopField Key="Phone">96691999</ShopField><ShopField Key="Email">samuel@chspos.com.au</ShopField><ShopField Key="DefaultPriceListId">1</ShopField><ShopField Key="DefaultPriceListName">Default Price List</ShopField><ShopField Key="MainCurrencyId">1</ShopField><ShopField Key="MainCurrencyName">Australian Dollar</ShopField><ShopField Key="LanguageIsoCode">en</ShopField><ShopField Key="CountryIsoCode">AUS</ShopField><ShopField Key="Web"/><CustomShopFields/></Shop><Seller><SellerField Key="SellerId">3</SellerField><SellerField Key="ContactType">1</SellerField><SellerField Key="Gender">0</SellerField><SellerField Key="FiscalIdDocType">0</SellerField><SellerField Key="FiscalId"/><SellerField Key="Name">Waiter</SellerField><SellerField Key="GivenName1"/><SellerField Key="Address"/><SellerField Key="PostalCode"/><SellerField Key="City"/><SellerField Key="State"/><SellerField Key="Phone"/><SellerField Key="Email">Vendedor 1</SellerField></Seller><Customer><CustomerField Key="customerId">0</CustomerField></Customer><Provider><ProviderField Key="providerId">null</ProviderField></Provider><DocCurrency><DocCurrencyField Key="CurrencyId">1</DocCurrencyField><DocCurrencyField Key="Name">Australian Dollar</DocCurrencyField><DocCurrencyField Key="DecimalCount">2</DocCurrencyField><DocCurrencyField Key="Initials">${'$'}</DocCurrencyField><DocCurrencyField Key="InitialsBefore">1</DocCurrencyField><DocCurrencyField Key="IsoCode">AUD</DocCurrencyField><DocCurrencyField Key="ExchangeRate">1.0</DocCurrencyField></DocCurrency></Header><Lines><Line><LineField Key="LineId">a0808099-9469-4db9-abbf-8b964a11ce27</LineField><LineField Key="LineNumber">1</LineField><LineField Key="ProductId">14</LineField><LineField Key="ProductSizeId">14</LineField><LineField Key="ExternalProductId">0</LineField><LineField Key="Name">Iced Coffee</LineField><LineField Key="Size"/><LineField Key="Units">4.0000</LineField><LineField Key="IsMenu">false</LineField><LineField Key="IsGift">false</LineField><LineField Key="PriceListId">1</LineField><LineField Key="Price">7.5000</LineField><LineField Key="SellerId">3</LineField><LineField Key="WarehouseId">1</LineField><LineField Key="DiscountReasonId">0</LineField><LineField Key="ReturnReasonId">0</LineField><LineField Key="ReturnReasonName"/><LineField Key="DiscountPercentage">0.0000</LineField><LineField Key="DiscountAmount">0.0000</LineField><LineField Key="DiscountAmountWithTaxes">0.0000</LineField><LineField Key="BaseAmount">27.2727</LineField><LineField Key="TaxesAmount">2.7273</LineField><LineField Key="TaxCategory">0</LineField><LineField Key="NetAmount">30.0000</LineField><LineField Key="Measure">1.0000</LineField><LineField Key="MeasureInitials"/><LineField Key="IsNew">false</LineField><LineField Key="ProductReference">200003</LineField><LineField Key="ProductBarcode"/><LineField Key="ServiceTypeId">0</LineField><LineField Key="DestinationWarehouseId">0</LineField><LineField Key="ReturnSaleSerie"/><LineField Key="ReturnSaleNumber"/><LineField Key="ReturnLineServiceTypeId">0</LineField><LineField Key="ProductType">Product</LineField><LineField Key="FamilyId">3</LineField><CustomProductFields/><CustomProductSizeFields/><CustomDocLineFields/><LineTaxes><LineTax><LineTaxField Key="TaxId">1</LineTaxField><LineTaxField Key="Position">1</LineTaxField><LineTaxField Key="Percentage">10.0000</LineTaxField><CustomDocLineTaxFields/><CustomTaxFields/></LineTax></LineTaxes><CustomDocLineSummaryFields/></Line><Line><LineField Key="LineId">3a5229bd-e12e-4e38-8733-af7d6e81ca60</LineField><LineField Key="LineNumber">2</LineField><LineField Key="ProductId">386</LineField><LineField Key="ProductSizeId">405</LineField><LineField Key="ExternalProductId">0</LineField><LineField Key="Name">Ginger Beer</LineField><LineField Key="Size"/><LineField Key="Units">4.0000</LineField><LineField Key="IsMenu">false</LineField><LineField Key="IsGift">false</LineField><LineField Key="PriceListId">1</LineField><LineField Key="Price">5.0000</LineField><LineField Key="SellerId">3</LineField><LineField Key="WarehouseId">1</LineField><LineField Key="DiscountReasonId">0</LineField><LineField Key="ReturnReasonId">0</LineField><LineField Key="ReturnReasonName"/><LineField Key="DiscountPercentage">0.0000</LineField><LineField Key="DiscountAmount">0.0000</LineField><LineField Key="DiscountAmountWithTaxes">0.0000</LineField><LineField Key="BaseAmount">18.1818</LineField><LineField Key="TaxesAmount">1.8182</LineField><LineField Key="TaxCategory">0</LineField><LineField Key="NetAmount">20.0000</LineField><LineField Key="Measure">1.0000</LineField><LineField Key="MeasureInitials"/><LineField Key="IsNew">false</LineField><LineField Key="ProductReference">504</LineField><LineField Key="ProductBarcode"/><LineField Key="ServiceTypeId">0</LineField><LineField Key="DestinationWarehouseId">0</LineField><LineField Key="ReturnSaleSerie"/><LineField Key="ReturnSaleNumber"/><LineField Key="ReturnLineServiceTypeId">0</LineField><LineField Key="ProductType">Product</LineField><LineField Key="FamilyId">3</LineField><CustomProductFields/><CustomProductSizeFields/><CustomDocLineFields/><LineTaxes><LineTax><LineTaxField Key="TaxId">1</LineTaxField><LineTaxField Key="Position">1</LineTaxField><LineTaxField Key="Percentage">10.0000</LineTaxField><CustomDocLineTaxFields/><CustomTaxFields/></LineTax></LineTaxes><CustomDocLineSummaryFields/></Line><Line><LineField Key="LineId">21c8f3d8-d9ca-4bcd-bf9b-670f43691741</LineField><LineField Key="LineNumber">3</LineField><LineField Key="ProductId">387</LineField><LineField Key="ProductSizeId">406</LineField><LineField Key="ExternalProductId">0</LineField><LineField Key="Name">Dry Ginger Ale</LineField><LineField Key="Size"/><LineField Key="Units">1.0000</LineField><LineField Key="IsMenu">false</LineField><LineField Key="IsGift">false</LineField><LineField Key="PriceListId">1</LineField><LineField Key="Price">5.0000</LineField><LineField Key="SellerId">3</LineField><LineField Key="WarehouseId">1</LineField><LineField Key="DiscountReasonId">0</LineField><LineField Key="ReturnReasonId">0</LineField><LineField Key="ReturnReasonName"/><LineField Key="DiscountPercentage">0.0000</LineField><LineField Key="DiscountAmount">0.0000</LineField><LineField Key="DiscountAmountWithTaxes">0.0000</LineField><LineField Key="BaseAmount">4.5455</LineField><LineField Key="TaxesAmount">0.4545</LineField><LineField Key="TaxCategory">0</LineField><LineField Key="NetAmount">5.0000</LineField><LineField Key="Measure">1.0000</LineField><LineField Key="MeasureInitials"/><LineField Key="IsNew">false</LineField><LineField Key="ProductReference">505</LineField><LineField Key="ProductBarcode"/><LineField Key="ServiceTypeId">0</LineField><LineField Key="DestinationWarehouseId">0</LineField><LineField Key="ReturnSaleSerie"/><LineField Key="ReturnSaleNumber"/><LineField Key="ReturnLineServiceTypeId">0</LineField><LineField Key="ProductType">Product</LineField><LineField Key="FamilyId">3</LineField><CustomProductFields/><CustomProductSizeFields/><CustomDocLineFields/><LineTaxes><LineTax><LineTaxField Key="TaxId">1</LineTaxField><LineTaxField Key="Position">1</LineTaxField><LineTaxField Key="Percentage">10.0000</LineTaxField><CustomDocLineTaxFields/><CustomTaxFields/></LineTax></LineTaxes><CustomDocLineSummaryFields/></Line></Lines><Taxes><Tax><TaxField Key="TaxId">1</TaxField><TaxField Key="Description">GST 10%</TaxField><TaxField Key="LineNumber">1</TaxField><TaxField Key="TaxBase">50.0000</TaxField><TaxField Key="Percentage">10.0000</TaxField><TaxField Key="TaxAmount">5.0000</TaxField><TaxField Key="FiscalId"/><TaxField Key="ExemptReason"/><TaxField Key="IsoCode"/><CustomDocTaxFields/><CustomTaxFields/></Tax></Taxes><PaymentMeans><PaymentMean><PaymentMeanField Key="PaymentMeanId">11</PaymentMeanField><PaymentMeanField Key="Type">0</PaymentMeanField><PaymentMeanField Key="LineNumber">1</PaymentMeanField><PaymentMeanField Key="Description">Datamesh Payment</PaymentMeanField><PaymentMeanField Key="PaymenMeanName">Datamesh Payment</PaymentMeanField><PaymentMeanField Key="Amount">55.0000</PaymentMeanField><PaymentMeanField Key="CurrencyISOCode">AUD</PaymentMeanField><PaymentMeanField Key="CurrencyExchangeRate">1.0000</PaymentMeanField><PaymentMeanField Key="TransactionId"/><PaymentMeanField Key="AuthorizationId"/><PaymentMeanField Key="ChargeDiscountType">0</PaymentMeanField><PaymentMeanField Key="ChargeDiscountValue">0.0000</PaymentMeanField><CustomPaymentMeanFields/><CustomDocPaymentMeanFields/><Currency><CurrencyField Key="Name">Australian Dollar</CurrencyField><CurrencyField Key="Initials">${'$'}</CurrencyField><CurrencyField Key="InitialsBefore">true</CurrencyField><CurrencyField Key="DecimalCount">2</CurrencyField><CurrencyField Key="IsoCode">AUD</CurrencyField><CustomCurrencyFields/></Currency><CurrencyField Key="CurrencyId">1</CurrencyField></PaymentMean></PaymentMeans><AdditionalFields><AdditionalField Key="5000013">Sydney</AdditionalField><AdditionalField Key="5000014">Alexandria</AdditionalField><AdditionalField Key="5000016">samuel@chspos.com.au</AdditionalField><AdditionalField Key="5000012">4455687811</AdditionalField><AdditionalField Key="5000010">Bar n Grill (Eft testing)</AdditionalField><AdditionalField Key="5000017">96691999</AdditionalField><AdditionalField Key="5000015">2015</AdditionalField><AdditionalField Key="5000011">Bar n Grill</AdditionalField><AdditionalField Key="5000009">Waiter</AdditionalField></AdditionalFields></Document>"""
//        val doc = parseXML(xml)
//        val header = doc.getElementsByTagName("Header").item(0)
//
//        // Extract SaleId from the Header element
//        val saleId = getElementText(header, "HeaderField", "SaleId")
//        println("SaleId: $saleId")
//
//
//        val sellerId = getElementText(header, "Seller", "SellerField")
//
//        println("sellerId: $sellerId")
//        val lineElements = doc.getElementsByTagName("Line")
//        val items = extractSaleItems(lineElements)
//
//        println("----------")
//        println(TerminalDevices.values().any { it.name == "INGENICO"  }.toString())
//
//        val referenceMessagePaymentType = PaymentType.Refund
//        println(referenceMessagePaymentType.name)
//
//    }
////
//    fun parseXML(xml: String): Document {
//        try {
//            val dbFactory = DocumentBuilderFactory.newInstance()
//            val dBuilder = dbFactory.newDocumentBuilder()
//            return dBuilder.parse(InputSource(StringReader(xml)))
//        } catch (e: Exception) {
//            e.printStackTrace()
//            throw e
//        }
//    }
//
//    fun getElementText(parent: Node, tagName: String, attributeKey: String): String? {
//        val elements = getElementsByTagName(parent, tagName)
//        return elements
//            .mapNotNull {
//                if (it.getAttribute("Key").equals(attributeKey, ignoreCase = true)) {
//                    return it.textContent
//                }
//                return@mapNotNull null
//            }
//            .firstOrNull()
//    }
//
//
//    fun extractSaleItems(lineElements: NodeList): List<HioposXMLMapping.HioposSaleItem> {
//        val items = mutableListOf<HioposXMLMapping.HioposSaleItem>()
//
//        for (i in 0 until lineElements.length) {
//            val lineElement = lineElements.item(i) as Element
//            val productId = getElementText(lineElement, "LineField", "ProductId")
//            val name = getElementText(lineElement, "LineField", "Name")
//            val units = getElementText(lineElement, "LineField", "Units")
//            val price = getElementText(lineElement, "LineField", "Price")
//            val netAmount = getElementText(lineElement, "LineField", "NetAmount")
//
//            if (productId != null && name != null && units != null && price != null && netAmount != null) {
//                val newItem =
//                    HioposXMLMapping.HioposSaleItem.Builder()
//                        .productId(productId)
//                        .name(name)
//                        .units(units)
//                        .price(price)
//                        .netAmount(netAmount)
//                        .build()
//
//                items.add(newItem)
//            }
//        }
//        return items
//    }
//
//    fun getElementsByTagName(parent: Node, tagName: String): List<Element> {
//        val elements = mutableListOf<Element>()
//        val childNodes = parent.childNodes
//        for (i in 0 until childNodes.length) {
//            val node = childNodes.item(i)
//            if (node is Element && node.nodeName == tagName) {
//                elements.add(node)
//            }
//        }
//        return elements
//    }

//    @Test
//    fun addition_isCorrect() {
//        assertEquals(4, 2 + 2)
//    }

}